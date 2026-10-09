package com.hoaxify.auth.controller;

import org.springframework.http.ResponseEntity;

import com.hoaxify.auth.dto.DtoAuthResponse;
import com.hoaxify.auth.dto.DtoCredentialsIU;
import com.hoaxify.auth.dto.DtoTokenVerification;
import com.hoaxify.auth.dto.DtoTokenVerifyIU;
import com.hoaxify.common.web.dto.DtoMessage;

import jakarta.servlet.http.HttpServletRequest;

public interface IAuthController {

	ResponseEntity<DtoAuthResponse> login(DtoCredentialsIU credentials);

	ResponseEntity<DtoMessage> logout(HttpServletRequest request);

	ResponseEntity<DtoTokenVerification> verify(DtoTokenVerifyIU request);
}
