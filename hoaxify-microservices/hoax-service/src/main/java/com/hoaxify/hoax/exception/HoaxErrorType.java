package com.hoaxify.hoax.exception;

import org.springframework.http.HttpStatus;

import com.hoaxify.common.web.exception.ErrorType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HoaxErrorType implements ErrorType {

	USER_NOT_FOUND("hoaxify.user.not.found", HttpStatus.NOT_FOUND),
	HOAX_NOT_FOUND("hoaxify.hoax.not.found", HttpStatus.NOT_FOUND),
	HOAX_DELETE_FORBIDDEN("hoaxify.hoax.delete.forbidden", HttpStatus.FORBIDDEN);

	private final String messageKey;

	private final HttpStatus status;
}
