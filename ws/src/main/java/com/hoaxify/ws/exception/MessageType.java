package com.hoaxify.ws.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Uygulamanın fırlatabileceği tüm iş hataları tek yerde. Her hata; mesaj anahtarını
 * (messages.properties) ve döneceği HTTP status'u bilir. Yeni bir hata türü eklemek
 * için buraya bir satır eklemek yeterli.
 */
@Getter
@AllArgsConstructor
public enum MessageType {

	USER_NOT_FOUND("hoaxify.user.not.found", HttpStatus.NOT_FOUND),
	USER_NOT_ACTIVE("hoaxify.user.not.active", HttpStatus.UNAUTHORIZED),
	EMAIL_NOT_UNIQUE("hoaxify.constraint.email.notunique", HttpStatus.BAD_REQUEST),
	INVALID_CREDENTIALS("hoaxify.auth.invalid.credentials", HttpStatus.UNAUTHORIZED),
	INVALID_ACTIVATION_TOKEN("hoaxify.activate.user.invalid.token", HttpStatus.BAD_REQUEST),
	INVALID_PASSWORD_RESET_TOKEN("hoaxify.password.reset.invalid.token", HttpStatus.BAD_REQUEST),
	ACTIVATION_EMAIL_FAILURE("hoaxify.create.user.email.failure", HttpStatus.BAD_GATEWAY),

	HOAX_NOT_FOUND("hoaxify.hoax.not.found", HttpStatus.NOT_FOUND),
	HOAX_DELETE_FORBIDDEN("hoaxify.hoax.delete.forbidden", HttpStatus.FORBIDDEN),

	FILE_SAVE_FAILURE("hoaxify.file.save.failure", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String messageKey;

	private final HttpStatus status;
}
