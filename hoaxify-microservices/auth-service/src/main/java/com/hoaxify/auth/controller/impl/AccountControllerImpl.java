package com.hoaxify.auth.controller.impl;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hoaxify.auth.controller.IAccountController;
import com.hoaxify.auth.dto.DtoPasswordResetIU;
import com.hoaxify.auth.dto.DtoPasswordUpdateIU;
import com.hoaxify.auth.dto.DtoRegisterIU;
import com.hoaxify.auth.services.IAccountService;
import com.hoaxify.common.web.dto.DtoMessage;
import com.hoaxify.common.web.i18n.MessageResolver;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * URL'ler monolith'le aynı (/api/v1/users/...) ki frontend değişmesin. Gateway, kayıt / aktivasyon /
 * şifre sıfırlama isteklerini buraya, profil isteklerini (/api/v1/users/{id}) user-service'e yönlendirir.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AccountControllerImpl implements IAccountController {

	private final IAccountService accountService;
	private final MessageResolver messageResolver;

	@PostMapping
	@Override
	public ResponseEntity<DtoMessage> register(@Valid @RequestBody DtoRegisterIU request) {
		Long userId = accountService.register(request);
		return ResponseEntity.created(URI.create("/api/v1/users/" + userId))
				.body(new DtoMessage(messageResolver.get("hoaxify.create.user.success.message")));
	}

	@PatchMapping("/{token}/active")
	@Override
	public ResponseEntity<DtoMessage> activate(@PathVariable String token) {
		accountService.activate(token);
		return ResponseEntity.ok(new DtoMessage(messageResolver.get("hoaxify.activate.user.success.message")));
	}

	@PostMapping("/password-reset")
	@Override
	public ResponseEntity<DtoMessage> requestPasswordReset(@Valid @RequestBody DtoPasswordResetIU request) {
		accountService.requestPasswordReset(request);
		return ResponseEntity.ok(new DtoMessage(messageResolver.get("hoaxify.password.reset.request.success")));
	}

	@PatchMapping("/{token}/password")
	@Override
	public ResponseEntity<DtoMessage> resetPassword(@PathVariable String token,
			@Valid @RequestBody DtoPasswordUpdateIU request) {
		accountService.resetPassword(token, request);
		return ResponseEntity.ok(new DtoMessage(messageResolver.get("hoaxify.password.reset.success")));
	}
}
