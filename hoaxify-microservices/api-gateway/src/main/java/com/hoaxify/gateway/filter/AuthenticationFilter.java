package com.hoaxify.gateway.filter;

import java.util.Optional;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.hoaxify.common.web.HoaxifyHeaders;
import com.hoaxify.gateway.error.ErrorResponseWriter;
import com.hoaxify.gateway.security.ITokenVerifier;
import com.hoaxify.gateway.security.ProtectedEndpoints;
import com.hoaxify.gateway.security.TokenExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Her isteğe uygulanan filtre (monolith'teki TokenFilter'ın gateway karşılığı):
 *
 *  1) Dışarıdan gelen X-User-Id başlığını SİLER. Silmezsek kötü niyetli biri "X-User-Id: 1" yazıp
 *     başkası gibi davranabilirdi. Servisler bu başlığa sadece gateway koyduğu için güvenir.
 *  2) Token varsa auth-service'e doğrulatır; geçerliyse X-User-Id'yi gateway kendisi ekler.
 *  3) Korumalı bir endpoint'e kimliksiz gelinirse 401 döner, istek servise hiç gitmez.
 *  4) auth-service'e ulaşılamazsa 503 döner.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

	private final ITokenVerifier tokenVerifier;
	private final ErrorResponseWriter errorResponseWriter;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest sanitized = exchange.getRequest().mutate()
				.headers(headers -> headers.remove(HoaxifyHeaders.USER_ID))
				.build();
		ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitized).build();

		Optional<String> token = TokenExtractor.extract(sanitized);
		if (token.isEmpty()) {
			return continueAs(sanitizedExchange, chain, Optional.empty());
		}
		return tokenVerifier.verify(token.get())
				.flatMap(userId -> continueAs(sanitizedExchange, chain, userId))
				.onErrorResume(ex -> {
					log.warn("Token verification failed: {}", ex.toString());
					return errorResponseWriter.write(sanitizedExchange, HttpStatus.SERVICE_UNAVAILABLE,
							"hoaxify.error.dependency.unavailable");
				});
	}

	private Mono<Void> continueAs(ServerWebExchange exchange, GatewayFilterChain chain, Optional<Long> userId) {
		ServerHttpRequest request = exchange.getRequest();
		if (userId.isEmpty()) {
			if (ProtectedEndpoints.requiresAuthentication(request.getMethod(), request.getPath().value())) {
				return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED, "hoaxify.error.unauthorized");
			}
			return chain.filter(exchange);
		}
		ServerHttpRequest authenticated = request.mutate()
				.header(HoaxifyHeaders.USER_ID, String.valueOf(userId.get()))
				.build();
		return chain.filter(exchange.mutate().request(authenticated).build());
	}

	/** Route'lama filtrelerinden önce çalışmalı */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
