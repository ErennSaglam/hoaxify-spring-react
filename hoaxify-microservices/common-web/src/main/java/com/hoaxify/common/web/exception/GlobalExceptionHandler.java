package com.hoaxify.common.web.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.hoaxify.common.error.ApiError;
import com.hoaxify.common.web.i18n.MessageResolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tüm servislerde ortak hata yönetimi. Monolith'teki GlobalExceptionHandler'ın aynısı;
 * Spring Security handler'ları yok çünkü kimlik doğrulama gateway'de yapılıyor.
 * Her servis aynı kodu kopyalamak yerine bu kütüphaneyi kullanır: bir hata formatı değişikliği tek yerden yapılır.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final MessageResolver messageResolver;

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		Map<String, String> validationErrors = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return build(HttpStatus.BAD_REQUEST, messageResolver.get("hoaxify.error.validation"), request,
				validationErrors);
	}

	@ExceptionHandler(BaseException.class)
	public ResponseEntity<ApiError<Map<String, String>>> handleBaseException(BaseException ex,
			HttpServletRequest request) {
		ErrorType type = ex.getErrorType();
		String message = messageResolver.get(type.getMessageKey(), ex.getArgs());

		if (ex.getField() != null) {
			return build(type.getStatus(), messageResolver.get("hoaxify.error.validation"), request,
					Map.of(ex.getField(), message));
		}
		ResponseEntity<ApiError<Map<String, String>>> response = build(type.getStatus(), message, request, null);
		if (type.getStatus().is5xxServerError()) {
			log.error("Business error id={} type={}", response.getBody().id(), type, ex);
		}
		return response;
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ApiError<Map<String, String>>> handleBadRequest(Exception ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, messageResolver.get("hoaxify.error.bad.request"), request, null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError<Map<String, String>>> handleUnknown(Exception ex, HttpServletRequest request) {
		if (ex instanceof ErrorResponse errorResponse) {
			return build(errorResponse.getStatusCode(), ex.getMessage(), request, null);
		}
		ResponseEntity<ApiError<Map<String, String>>> response = build(HttpStatus.INTERNAL_SERVER_ERROR,
				messageResolver.get("hoaxify.error.unexpected"), request, null);
		log.error("Unexpected error id={} path={}", response.getBody().id(), request.getRequestURI(), ex);
		return response;
	}

	private <T> ResponseEntity<ApiError<T>> build(HttpStatusCode status, String message, HttpServletRequest request,
			T validationErrors) {
		ApiError<T> apiError = new ApiError<>(UUID.randomUUID().toString(), status.value(), message,
				request.getRequestURI(), Instant.now(), validationErrors);
		return ResponseEntity.status(status).body(apiError);
	}
}
