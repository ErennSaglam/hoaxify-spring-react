package com.hoaxify.ws.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.hoaxify.ws.utils.MessageResolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller'lardan (ve security filtrelerinden) fırlayan tüm exception'lar burada
 * ApiError formatına çevrilir. Controller ve servislerde try/catch yazmaya gerek kalmaz.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final MessageResolver messageResolver;

	/** @Valid ile işaretli DTO validation'dan geçemezse */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		Map<String, String> validationErrors = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			// Aynı alanda birden fazla hata varsa ilkini gösteriyoruz
			validationErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return build(HttpStatus.BAD_REQUEST, messageResolver.get("hoaxify.error.validation"), request,
				validationErrors);
	}

	/** Servis katmanından fırlatılan iş hataları */
	@ExceptionHandler(BaseException.class)
	public ResponseEntity<ApiError<Map<String, String>>> handleBaseException(BaseException ex,
			HttpServletRequest request) {
		MessageType type = ex.getMessageType();
		String message = messageResolver.get(type.getMessageKey(), ex.getArgs());

		if (ex.getField() != null) {
			return build(type.getStatus(), messageResolver.get("hoaxify.error.validation"), request,
					Map.of(ex.getField(), message));
		}
		ResponseEntity<ApiError<Map<String, String>>> response = build(type.getStatus(), message, request, null);
		if (type.getStatus().is5xxServerError()) {
			log.error("Business error id={} type={}", response.getBody().getId(), type, ex);
		}
		return response;
	}

	/** Token yok / geçersiz / kullanıcı pasif (TokenFilter ve AuthEntryPoint buraya yönlendirir) */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError<Map<String, String>>> handleAuthentication(AuthenticationException ex,
			HttpServletRequest request) {
		String key = ex instanceof DisabledException ? "hoaxify.user.not.active" : "hoaxify.error.unauthorized";
		return build(HttpStatus.UNAUTHORIZED, messageResolver.get(key), request, null);
	}

	/** @PreAuthorize("#id == principal.id") gibi yetki kontrolleri başarısız olursa */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError<Map<String, String>>> handleAccessDenied(AccessDeniedException ex,
			HttpServletRequest request) {
		return build(HttpStatus.FORBIDDEN, messageResolver.get("hoaxify.error.forbidden"), request, null);
	}

	/** Bozuk JSON gövdesi veya /users/abc gibi tipi uymayan path parametresi */
	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ApiError<Map<String, String>>> handleBadRequest(Exception ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, messageResolver.get("hoaxify.error.bad.request"), request, null);
	}

	/** Geri kalan her şey */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError<Map<String, String>>> handleUnknown(Exception ex, HttpServletRequest request) {
		// Spring'in kendi hataları (404 NoResourceFound, 405 MethodNotSupported ...) status kodunu kendisi taşır
		if (ex instanceof ErrorResponse errorResponse) {
			return build(errorResponse.getStatusCode(), ex.getMessage(), request, null);
		}
		ResponseEntity<ApiError<Map<String, String>>> response = build(HttpStatus.INTERNAL_SERVER_ERROR,
				messageResolver.get("hoaxify.error.unexpected"), request, null);
		log.error("Unexpected error id={} path={}", response.getBody().getId(), request.getRequestURI(), ex);
		return response;
	}

	private <T> ResponseEntity<ApiError<T>> build(HttpStatusCode status, String message, HttpServletRequest request,
			T validationErrors) {
		ApiError<T> apiError = new ApiError<>();
		apiError.setId(UUID.randomUUID().toString());
		apiError.setStatus(status.value());
		apiError.setMessage(message);
		apiError.setPath(request.getRequestURI());
		apiError.setTimestamp(Instant.now());
		apiError.setValidationErrors(validationErrors);
		return ResponseEntity.status(status).body(apiError);
	}
}
