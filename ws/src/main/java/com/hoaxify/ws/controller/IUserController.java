package com.hoaxify.ws.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.hoaxify.ws.dto.DtoMessage;
import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.dto.DtoPasswordResetIU;
import com.hoaxify.ws.dto.DtoPasswordUpdateIU;
import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.dto.DtoUserIU;
import com.hoaxify.ws.dto.DtoUserUpdateIU;
import com.hoaxify.ws.security.CurrentUser;

public interface IUserController {

	ResponseEntity<DtoMessage> createUser(DtoUserIU dtoUserIU);

	ResponseEntity<DtoMessage> activateUser(String token);

	ResponseEntity<DtoPage<DtoUser>> getUsers(Pageable pageable, CurrentUser currentUser);

	ResponseEntity<DtoUser> getUserById(Long id);

	ResponseEntity<DtoUser> updateUser(Long id, DtoUserUpdateIU dtoUserUpdateIU);

	ResponseEntity<Void> deleteUser(Long id);

	ResponseEntity<DtoMessage> requestPasswordReset(DtoPasswordResetIU dtoPasswordResetIU);

	ResponseEntity<DtoMessage> resetPassword(String token, DtoPasswordUpdateIU dtoPasswordUpdateIU);
}
