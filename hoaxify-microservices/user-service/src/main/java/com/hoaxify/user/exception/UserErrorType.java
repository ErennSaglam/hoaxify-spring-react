package com.hoaxify.user.exception;

import org.springframework.http.HttpStatus;

import com.hoaxify.common.web.exception.ErrorType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorType implements ErrorType {

	USER_NOT_FOUND("hoaxify.user.not.found", HttpStatus.NOT_FOUND),
	FILE_SAVE_FAILURE("hoaxify.file.save.failure", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String messageKey;

	private final HttpStatus status;
}
