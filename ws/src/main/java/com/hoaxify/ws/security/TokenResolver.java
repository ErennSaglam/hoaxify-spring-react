package com.hoaxify.ws.security;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * İstekten token'ı okur. Önce httpOnly "hoax-token" cookie'sine (frontend bunu kullanır),
 * yoksa "Authorization: Bearer xxx" başlığına (Swagger / Postman) bakar.
 * Dönen değer prefix'sizdir.
 */
public final class TokenResolver {

	public static final String TOKEN_COOKIE = "hoax-token";

	private TokenResolver() {
	}

	public static Optional<String> resolve(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			Optional<String> fromCookie = Arrays.stream(cookies)
					.filter(cookie -> TOKEN_COOKIE.equals(cookie.getName()))
					.map(Cookie::getValue)
					.filter(value -> value != null && !value.isBlank())
					.findFirst();
			if (fromCookie.isPresent()) {
				return fromCookie;
			}
		}

		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null) {
			return Optional.empty();
		}
		String[] parts = header.trim().split(" ", 2);
		return parts.length == 2 && !parts[1].isBlank() ? Optional.of(parts[1].trim()) : Optional.empty();
	}
}
