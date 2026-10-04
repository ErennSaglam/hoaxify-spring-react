package com.hoaxify.ws.controller.impl;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hoaxify.ws.controller.IUserController;
import com.hoaxify.ws.dto.DtoMessage;
import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.dto.DtoPasswordResetIU;
import com.hoaxify.ws.dto.DtoPasswordUpdateIU;
import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.dto.DtoUserIU;
import com.hoaxify.ws.dto.DtoUserUpdateIU;
import com.hoaxify.ws.security.CurrentUser;
import com.hoaxify.ws.services.IUserService;
import com.hoaxify.ws.utils.MessageResolver;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller sadece HTTP ile ilgilenir: isteği DTO olarak alır, servise verir,
 * sonucu doğru status koduyla döner. İş kuralı ve entity burada yok.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserControllerImpl implements IUserController {

	private final IUserService userService;
	private final MessageResolver messageResolver;

	@PostMapping
	@Override
	public ResponseEntity<DtoMessage> createUser(@Valid @RequestBody DtoUserIU dtoUserIU) {
		DtoUser created = userService.createUser(dtoUserIU);
		// 201 Created + Location: /api/v1/users/{id}
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(created.getId()).toUri();
		return ResponseEntity.created(location)
				.body(new DtoMessage(messageResolver.get("hoaxify.create.user.success.message")));
	}

	@PatchMapping("/{token}/active")
	@Override
	public ResponseEntity<DtoMessage> activateUser(@PathVariable String token) {
		userService.activateUser(token);
		return ResponseEntity.ok(new DtoMessage(messageResolver.get("hoaxify.activate.user.success.message")));
	}

	/** ?page=0&size=3&sort=username,asc */
	@GetMapping
	@Override
	public ResponseEntity<DtoPage<DtoUser>> getUsers(@PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
			@AuthenticationPrincipal CurrentUser currentUser) {
		Long currentUserId = currentUser != null ? currentUser.getId() : null;
		return ResponseEntity.ok(userService.getUsers(pageable, currentUserId));
	}

	@GetMapping("/{id}")
	@Override
	public ResponseEntity<DtoUser> getUserById(@PathVariable Long id) {
		return ResponseEntity.ok(userService.getUserById(id));
	}

	/** Sadece kendi profilini güncelleyebilir: path'teki id == giriş yapan kullanıcının id'si */
	@PutMapping("/{id}")
	@PreAuthorize("#id == principal.id")
	@Override
	public ResponseEntity<DtoUser> updateUser(@PathVariable Long id, @Valid @RequestBody DtoUserUpdateIU dtoUserUpdateIU) {
		return ResponseEntity.ok(userService.updateUser(id, dtoUserUpdateIU));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("#id == principal.id")
	@Override
	public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/password-reset")
	@Override
	public ResponseEntity<DtoMessage> requestPasswordReset(@Valid @RequestBody DtoPasswordResetIU dtoPasswordResetIU) {
		userService.requestPasswordReset(dtoPasswordResetIU);
		return ResponseEntity.ok(new DtoMessage(messageResolver.get("hoaxify.password.reset.request.success")));
	}

	@PatchMapping("/{token}/password")
	@Override
	public ResponseEntity<DtoMessage> resetPassword(@PathVariable String token,
			@Valid @RequestBody DtoPasswordUpdateIU dtoPasswordUpdateIU) {
		userService.resetPassword(token, dtoPasswordUpdateIU);
		return ResponseEntity.ok(new DtoMessage(messageResolver.get("hoaxify.password.reset.success")));
	}
}
