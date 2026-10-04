package com.hoaxify.ws.controller;

import org.springframework.http.ResponseEntity;

import com.hoaxify.ws.dto.DtoAuthResponse;
import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoMessage;

import jakarta.servlet.http.HttpServletRequest;

public interface IAuthController {

	ResponseEntity<DtoAuthResponse> login(DtoCredentialsIU credentials);

	ResponseEntity<DtoMessage> logout(HttpServletRequest request);
}
