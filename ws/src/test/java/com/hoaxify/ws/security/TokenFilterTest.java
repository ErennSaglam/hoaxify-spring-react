package com.hoaxify.ws.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.services.ITokenService;

import jakarta.servlet.http.Cookie;

/**
 * Tespit edilen senaryolar:
 *  doFilterInternal : token yok (servis çağrılmaz) | token geçersiz | geçerli + aktif (SecurityContext dolar)
 *                     | geçerli + pasif (zincir durur, DisabledException resolver'a gider) | cookie'den token
 */
@ExtendWith(MockitoExtension.class)
class TokenFilterTest {

	@Mock
	private ITokenService tokenService;

	@Mock
	private HandlerExceptionResolver exceptionResolver;

	@InjectMocks
	private TokenFilter tokenFilter;

	private EasyRandom easyRandom;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private MockFilterChain chain;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		chain = new MockFilterChain();
	}

	@AfterEach
	void tearDown() {
		// SecurityContext thread'e bağlı; testler birbirini etkilemesin
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilter_noToken_shouldContinueAnonymously() throws Exception {
		// Act
		tokenFilter.doFilter(request, response, chain);

		// Assert
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertNotNull(chain.getRequest(), "istek zincirde devam etmeli");
		verifyNoInteractions(tokenService, exceptionResolver);
	}

	@Test
	void doFilter_invalidToken_shouldContinueAnonymously() throws Exception {
		// Arrange
		request.addHeader("Authorization", "Bearer bad");
		when(tokenService.verifyToken("bad")).thenReturn(Optional.empty());

		// Act
		tokenFilter.doFilter(request, response, chain);

		// Assert
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertNotNull(chain.getRequest());
		verify(tokenService, times(1)).verifyToken("bad");
	}

	@Test
	void doFilter_validTokenActiveUser_shouldAuthenticate() throws Exception {
		// Arrange
		User user = easyRandom.nextObject(User.class);
		user.setId(7L);
		user.setActive(true);
		request.addHeader("Authorization", "Bearer good");
		when(tokenService.verifyToken("good")).thenReturn(Optional.of(user));

		// Act
		tokenFilter.doFilter(request, response, chain);

		// Assert
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(authentication);
		assertTrue(authentication.getPrincipal() instanceof CurrentUser);
		assertEquals(7L, ((CurrentUser) authentication.getPrincipal()).getId());
		assertNotNull(chain.getRequest());
		verify(exceptionResolver, never()).resolveException(any(), any(), any(), any());
	}

	@Test
	void doFilter_validTokenInactiveUser_shouldStopChain() throws Exception {
		// Arrange
		User user = easyRandom.nextObject(User.class);
		user.setActive(false);
		request.addHeader("Authorization", "Bearer good");
		when(tokenService.verifyToken("good")).thenReturn(Optional.of(user));

		// Act
		tokenFilter.doFilter(request, response, chain);

		// Assert
		verify(exceptionResolver, times(1))
				.resolveException(eq(request), eq(response), isNull(), isA(DisabledException.class));
		assertNull(chain.getRequest(), "istek controller'a ulaşmamalı");
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void doFilter_tokenInCookie_shouldBeVerified() throws Exception {
		// Arrange
		request.setCookies(new Cookie("hoax-token", "cookie-token"));
		when(tokenService.verifyToken("cookie-token")).thenReturn(Optional.empty());

		// Act
		tokenFilter.doFilter(request, response, chain);

		// Assert
		verify(tokenService, times(1)).verifyToken("cookie-token");
	}
}
