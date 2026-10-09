package com.hoaxify.auth.controller;

import org.springframework.http.ResponseEntity;

import com.hoaxify.auth.dto.DtoPasswordResetIU;
import com.hoaxify.auth.dto.DtoPasswordUpdateIU;
import com.hoaxify.auth.dto.DtoRegisterIU;
import com.hoaxify.common.web.dto.DtoMessage;

public interface IAccountController {

	ResponseEntity<DtoMessage> register(DtoRegisterIU request);

	ResponseEntity<DtoMessage> activate(String token);

	ResponseEntity<DtoMessage> requestPasswordReset(DtoPasswordResetIU request);

	ResponseEntity<DtoMessage> resetPassword(String token, DtoPasswordUpdateIU request);
}
