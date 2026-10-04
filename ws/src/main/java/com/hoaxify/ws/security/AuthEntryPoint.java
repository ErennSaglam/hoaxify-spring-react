package com.hoaxify.ws.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Giriş yapmadan korumalı bir endpoint'e gelinirse Spring Security burayı çağırır.
 * Hatayı GlobalExceptionHandler'a yönlendiriyoruz ki cevap diğer hatalarla aynı ApiError formatında olsun.
 */
@RequiredArgsConstructor
public class AuthEntryPoint implements AuthenticationEntryPoint {

	private final HandlerExceptionResolver exceptionResolver;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) {
		exceptionResolver.resolveException(request, response, null, authException);
	}
}
