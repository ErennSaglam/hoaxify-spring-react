package com.hoaxify.gateway.security;

import java.util.Optional;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import com.hoaxify.common.web.HoaxifyHeaders;

/** Monolith'teki TokenResolver'ın reaktif karşılığı: önce hoax-token cookie'si, yoksa Authorization başlığı */
public final class TokenExtractor {

	private TokenExtractor() {
	}

	public static Optional<String> extract(ServerHttpRequest request) {
		HttpCookie cookie = request.getCookies().getFirst(HoaxifyHeaders.TOKEN_COOKIE);
		if (cookie != null && !cookie.getValue().isBlank()) {
			return Optional.of(cookie.getValue());
		}
		String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (header == null) {
			return Optional.empty();
		}
		String[] parts = header.trim().split(" ", 2);
		return parts.length == 2 && !parts[1].isBlank() ? Optional.of(parts[1].trim()) : Optional.empty();
	}
}
