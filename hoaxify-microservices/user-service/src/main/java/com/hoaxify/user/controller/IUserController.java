package com.hoaxify.user.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.hoaxify.common.web.dto.DtoPage;
import com.hoaxify.user.dto.DtoUser;
import com.hoaxify.user.dto.DtoUserUpdateIU;

public interface IUserController {

	ResponseEntity<DtoPage<DtoUser>> getUsers(Pageable pageable, Long currentUserId);

	ResponseEntity<DtoUser> getUser(Long id);

	ResponseEntity<DtoUser> updateUser(Long id, DtoUserUpdateIU request, Long currentUserId);

	ResponseEntity<Void> deleteUser(Long id, Long currentUserId);

	/** Sadece servisler arası (auth-service Feign) */
	ResponseEntity<DtoUser> getUserInternal(Long id);
}
