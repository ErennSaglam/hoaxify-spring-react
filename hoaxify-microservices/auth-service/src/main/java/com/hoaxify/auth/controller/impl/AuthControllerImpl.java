package com.hoaxify.auth.controller.impl;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.hoaxify.auth.controller.IAuthController;
import com.hoaxify.auth.dto.DtoAuthResponse;
import com.hoaxify.auth.dto.DtoCredentialsIU;
import com.hoaxify.auth.dto.DtoTokenVerification;
import com.hoaxify.auth.dto.DtoTokenVerifyIU;
import com.hoaxify.auth.services.IAuthService;
import com.hoaxify.auth.web.TokenResolver;
import com.hoaxify.common.web.HoaxifyHeaders;
import com.hoaxify.common.web.dto.DtoMessage;
import com.hoaxify.common.web.i18n.MessageResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthControllerImpl implements IAuthController {

	private final IAuthService authService;
	private final MessageResolver messageResolver;

	@PostMapping("/api/v1/auth")
	@Override
	public ResponseEntity<DtoAuthResponse> login(@Valid @RequestBody DtoCredentialsIU credentials) {
		DtoAuthResponse response = authService.login(credentials);
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, tokenCookie(response.token().token(), -1).toString())
				.body(response);
	}

	@PostMapping("/api/v1/logout")
	@Override
	public ResponseEntity<DtoMessage> logout(HttpServletRequest request) {
		TokenResolver.resolve(request).ifPresent(authService::logout);
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, tokenCookie("", 0).toString())
				.body(new DtoMessage(messageResolver.get("hoaxify.logout.success")));
	}

	/**
	 * SADECE gateway çağırır (gateway'de dışarıya açık bir route'u yok).
	 * Her korumalı istekte token'ın hangi kullanıcıya ait olduğunu söyler.
	 */
	@PostMapping("/internal/auth/verify")
	@Override
	public ResponseEntity<DtoTokenVerification> verify(@Valid @RequestBody DtoTokenVerifyIU request) {
		return ResponseEntity.ok(new DtoTokenVerification(authService.verify(request.token())));
	}

	/** maxAgeSeconds < 0: tarayıcı kapanana kadar; 0: hemen sil */
	private ResponseCookie tokenCookie(String value, long maxAgeSeconds) {
		return ResponseCookie.from(HoaxifyHeaders.TOKEN_COOKIE, value)
				.path("/")
				.httpOnly(true)
				.sameSite("Lax")
				.maxAge(maxAgeSeconds)
				.build();
	}
}
