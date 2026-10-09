package com.hoaxify.auth.web;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpHeaders;

import com.hoaxify.common.web.HoaxifyHeaders;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/** Monolith'teki TokenResolver: önce hoax-token cookie'si, yoksa "Authorization: Bearer xxx" */
public final class TokenResolver {

	private TokenResolver() {
	}

	public static Optional<String> resolve(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			Optional<String> fromCookie = Arrays.stream(cookies)
					.filter(cookie -> HoaxifyHeaders.TOKEN_COOKIE.equals(cookie.getName()))
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
