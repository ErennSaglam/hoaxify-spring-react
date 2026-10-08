package com.hoaxify.common.web.exception;

import org.springframework.http.HttpStatus;

/**
 * Her servis kendi hata türlerini bu arayüzü uygulayan bir enum'da tanımlar (ör. AuthErrorType).
 * Ortak GlobalExceptionHandler, hangi servisin enum'u olduğunu bilmeden hepsini aynı şekilde işler.
 */
public interface ErrorType {

	/** messages*.properties içindeki anahtar */
	String getMessageKey();

	HttpStatus getStatus();
}
