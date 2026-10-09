package com.hoaxify.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hoaxify.common.web.HoaxifyHeaders;
import com.hoaxify.gateway.error.ErrorResponseWriter;
import com.hoaxify.gateway.security.ITokenVerifier;

import reactor.core.publisher.Mono;

/**
 * Gateway filtresi: MockServerWebExchange sahte bir HTTP isteği, chain ise "sonraki adım"ı temsil eder.
 * Chain'e ulaşan isteği yakalayıp arkadaki servise hangi başlıklarla gideceğini kontrol ediyoruz.
 *
 * Tespit edilen senaryolar:
 *  token yok + açık endpoint         -> geçer, X-User-Id yok
 *  token yok + korumalı endpoint     -> 401, servise gitmez
 *  sahte X-User-Id                   -> silinir
 *  geçerli token (cookie / header)   -> X-User-Id eklenir
 *  geçersiz token + korumalı         -> 401
 *  auth-service ulaşılamıyor         -> 503
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

	@Mock
	private ITokenVerifier tokenVerifier;

	@Mock
	private GatewayFilterChain chain;

	private AuthenticationFilter filter;

	/** Chain'e iletilen (servise gidecek) exchange */
	private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		filter = new AuthenticationFilter(tokenVerifier, new ErrorResponseWriter(objectMapper, new StaticMessageSource()));
	}

	private void chainRecordsForwardedExchange() {
		when(chain.filter(any())).thenAnswer(invocation -> {
			forwarded.set(invocation.getArgument(0));
			return Mono.empty();
		});
	}

	@Test
	void noToken_publicEndpoint_shouldPassWithoutUserId() {
		// Arrange
		chainRecordsForwardedExchange();
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/hoaxes"));

		// Act
		filter.filter(exchange, chain).block();

		// Assert
		assertNotNull(forwarded.get());
		assertNull(forwarded.get().getRequest().getHeaders().getFirst(HoaxifyHeaders.USER_ID));
		verifyNoInteractions(tokenVerifier);
	}

	@Test
	void noToken_protectedEndpoint_shouldReturn401() {
		// Arrange
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/hoaxes"));

		// Act
		filter.filter(exchange, chain).block();

		// Assert
		assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
		assertTrue(exchange.getResponse().getBodyAsString().block().contains("\"status\":401"));
		verify(chain, never()).filter(any());
	}

	@Test
	void spoofedUserIdHeader_shouldBeRemoved() {
		// Arrange
		chainRecordsForwardedExchange();
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/users").header(HoaxifyHeaders.USER_ID, "1"));

		// Act
		filter.filter(exchange, chain).block();

		// Assert
		assertNull(forwarded.get().getRequest().getHeaders().getFirst(HoaxifyHeaders.USER_ID));
	}

	@Test
	void validCookieToken_shouldAddUserIdHeader() {
		// Arrange
		chainRecordsForwardedExchange();
		when(tokenVerifier.verify("cookie-token")).thenReturn(Mono.just(Optional.of(7L)));
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/hoaxes")
				.cookie(new HttpCookie(HoaxifyHeaders.TOKEN_COOKIE, "cookie-token")));

		// Act
		filter.filter(exchange, chain).block();

		// Assert
		assertEquals("7", forwarded.get().getRequest().getHeaders().getFirst(HoaxifyHeaders.USER_ID));
		verify(tokenVerifier, times(1)).verify("cookie-token");
	}

	@Test
	void validBearerToken_shouldReplaceSpoofedHeaderWithRealUserId() {
		// Arrange
		chainRecordsForwardedExchange();
		when(tokenVerifier.verify("header-token")).thenReturn(Mono.just(Optional.of(7L)));
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users")
				.header("Authorization", "Bearer header-token")
				.header(HoaxifyHeaders.USER_ID, "1"));

		// Act
		filter.filter(exchange, chain).block();

		// Assert
		assertEquals("7", forwarded.get().getRequest().getHeaders().getFirst(HoaxifyHeaders.USER_ID));
	}

	@Test
	void invalidToken_protectedEndpoint_shouldReturn401() {
		// Arrange
		when(tokenVerifier.verify("expired")).thenReturn(Mono.just(Optional.empty()));
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.delete("/api/v1/hoaxes/5").header("Authorization", "Bearer expired"));

		// Act
		filter.filter(exchange, chain).block();

		// Assert
		assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
		verify(chain, never()).filter(any());
	}

	@Test
	void authServiceUnavailable_shouldReturn503() {
		// Arrange
		when(tokenVerifier.verify("token")).thenReturn(Mono.error(new IllegalStateException("connection refused")));
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/users").header("Authorization", "Bearer token"));

		// Act
		filter.filter(exchange, chain).block();

		// Assert
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
		verify(chain, never()).filter(any());
	}
}
