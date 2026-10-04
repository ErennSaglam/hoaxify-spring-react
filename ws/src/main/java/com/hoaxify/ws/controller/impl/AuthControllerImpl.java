package com.hoaxify.ws.controller.impl;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hoaxify.ws.controller.IAuthController;
import com.hoaxify.ws.dto.DtoAuthResponse;
import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoMessage;
import com.hoaxify.ws.security.TokenResolver;
import com.hoaxify.ws.services.IAuthService;
import com.hoaxify.ws.utils.MessageResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthControllerImpl implements IAuthController {

	private final IAuthService authService;
	private final MessageResolver messageResolver;

	/**
	 * Token hem body'de hem httpOnly cookie'de döner. Frontend cookie'yi kullanır:
	 * httpOnly olduğu için JavaScript okuyamaz, XSS ile çalınamaz. SameSite=Lax başka
	 * sitelerden gelen POST/PUT/DELETE isteklerinde cookie'nin gönderilmesini engeller (CSRF).
	 */
	@PostMapping("/auth")
	@Override
	public ResponseEntity<DtoAuthResponse> login(@Valid @RequestBody DtoCredentialsIU credentials) {
		DtoAuthResponse authResponse = authService.authenticate(credentials);
		ResponseCookie cookie = ResponseCookie.from(TokenResolver.TOKEN_COOKIE, authResponse.getToken().getToken())
				.path("/")
				.httpOnly(true)
				.sameSite("Lax")
				.build();
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(authResponse);
	}

	@PostMapping("/logout")
	@Override
	public ResponseEntity<DtoMessage> logout(HttpServletRequest request) {
		TokenResolver.resolve(request).ifPresent(authService::logout);
		ResponseCookie expiredCookie = ResponseCookie.from(TokenResolver.TOKEN_COOKIE, "")
				.path("/")
				.maxAge(0)
				.httpOnly(true)
				.sameSite("Lax")
				.build();
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
				.body(new DtoMessage(messageResolver.get("hoaxify.logout.success")));
	}
}
