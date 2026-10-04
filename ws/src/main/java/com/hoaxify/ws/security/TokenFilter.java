package com.hoaxify.ws.security;

import java.io.IOException;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.services.ITokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Her istekte bir kez çalışır: token geçerliyse kullanıcıyı SecurityContext'e koyar.
 * Token yoksa/geçersizse isteği anonim olarak devam ettirir; korumalı endpoint'lerde
 * Spring Security 401 döner.
 *
 * Bilerek @Component DEĞİL: bean olsaydı Spring Boot onu servlet filtresi olarak da
 * kaydederdi ve security zincirinin dışında ikinci kez çalışırdı. SecurityConfiguration
 * içinde new ile oluşturup sadece security zincirine ekliyoruz.
 */
@RequiredArgsConstructor
public class TokenFilter extends OncePerRequestFilter {

	private final ITokenService tokenService;
	private final HandlerExceptionResolver exceptionResolver;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		User user = TokenResolver.resolve(request).flatMap(tokenService::verifyToken).orElse(null);

		if (user != null) {
			if (!user.isActive()) {
				// Filtrede fırlayan exception @RestControllerAdvice'a ulaşmaz; resolver ile oraya yönlendiriyoruz
				exceptionResolver.resolveException(request, response, null, new DisabledException("User is disabled"));
				return;
			}
			CurrentUser currentUser = new CurrentUser(user);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(currentUser,
					null, currentUser.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		filterChain.doFilter(request, response);
	}
}
