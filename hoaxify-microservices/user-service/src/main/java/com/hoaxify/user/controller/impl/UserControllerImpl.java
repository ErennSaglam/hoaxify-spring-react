package com.hoaxify.user.controller.impl;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.hoaxify.common.web.dto.DtoPage;
import com.hoaxify.common.web.security.CurrentUserId;
import com.hoaxify.user.controller.IUserController;
import com.hoaxify.user.dto.DtoUser;
import com.hoaxify.user.dto.DtoUserUpdateIU;
import com.hoaxify.user.services.IUserProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserControllerImpl implements IUserController {

	private final IUserProfileService userProfileService;

	/** Anonim de çağrılabilir; giriş yapılmışsa kullanıcı kendini listede görmez */
	@GetMapping("/api/v1/users")
	@Override
	public ResponseEntity<DtoPage<DtoUser>> getUsers(
			@PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
			@CurrentUserId(required = false) Long currentUserId) {
		return ResponseEntity.ok(userProfileService.getUsers(pageable, currentUserId));
	}

	@GetMapping("/api/v1/users/{id}")
	@Override
	public ResponseEntity<DtoUser> getUser(@PathVariable Long id) {
		return ResponseEntity.ok(userProfileService.getUser(id));
	}

	@PutMapping("/api/v1/users/{id}")
	@Override
	public ResponseEntity<DtoUser> updateUser(@PathVariable Long id, @Valid @RequestBody DtoUserUpdateIU request,
			@CurrentUserId Long currentUserId) {
		return ResponseEntity.ok(userProfileService.updateUser(id, currentUserId, request));
	}

	@DeleteMapping("/api/v1/users/{id}")
	@Override
	public ResponseEntity<Void> deleteUser(@PathVariable Long id, @CurrentUserId Long currentUserId) {
		userProfileService.deleteUser(id, currentUserId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/internal/users/{id}")
	@Override
	public ResponseEntity<DtoUser> getUserInternal(@PathVariable Long id) {
		return ResponseEntity.ok(userProfileService.getUser(id));
	}
}
