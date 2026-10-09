package com.hoaxify.auth.exception;

import org.springframework.http.HttpStatus;

import com.hoaxify.common.web.exception.ErrorType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorType implements ErrorType {

	EMAIL_NOT_UNIQUE("hoaxify.constraint.email.notunique", HttpStatus.BAD_REQUEST),
	INVALID_CREDENTIALS("hoaxify.auth.invalid.credentials", HttpStatus.UNAUTHORIZED),
	USER_NOT_ACTIVE("hoaxify.user.not.active", HttpStatus.UNAUTHORIZED),
	INVALID_ACTIVATION_TOKEN("hoaxify.activate.user.invalid.token", HttpStatus.BAD_REQUEST),
	INVALID_PASSWORD_RESET_TOKEN("hoaxify.password.reset.invalid.token", HttpStatus.BAD_REQUEST),
	INVALID_SESSION_TOKEN("hoaxify.error.unauthorized", HttpStatus.UNAUTHORIZED);

	private final String messageKey;

	private final HttpStatus status;
}
