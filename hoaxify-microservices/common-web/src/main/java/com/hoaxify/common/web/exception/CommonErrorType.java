package com.hoaxify.common.web.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Tüm servislerde ortak olan hata türleri */
@Getter
@AllArgsConstructor
public enum CommonErrorType implements ErrorType {

	UNAUTHORIZED("hoaxify.error.unauthorized", HttpStatus.UNAUTHORIZED),
	FORBIDDEN("hoaxify.error.forbidden", HttpStatus.FORBIDDEN),
	/** Çağrılan başka bir servis cevap vermediğinde (Feign / gRPC) */
	DEPENDENCY_UNAVAILABLE("hoaxify.error.dependency.unavailable", HttpStatus.SERVICE_UNAVAILABLE);

	private final String messageKey;

	private final HttpStatus status;
}
