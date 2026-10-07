package com.hoaxify.ws.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.hoaxify.ws.dto.DtoUserIU;
import com.hoaxify.ws.utils.MessageResolver;

/**
 * Tespit edilen senaryolar:
 *  handleValidation     : alan hataları map'e dolar | aynı alanda iki hata -> ilki kalır
 *  handleBaseException  : alan hatası (field != null) | alan hatası değil | 5xx (loglanır) | mesaj argümanları
 *  handleAuthentication : DisabledException -> "not active" | diğer -> "unauthorized"
 *  handleAccessDenied   : 403
 *  handleBadRequest     : bozuk JSON -> 400
 *  handleUnknown        : Spring ErrorResponse (404 NoResourceFound) -> kendi status'u | diğer -> 500
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	@Mock
	private MessageResolver messageResolver;

	@InjectMocks
	private GlobalExceptionHandler exceptionHandler;

	private MockHttpServletRequest request;

	@BeforeEach
	void setUp() {
		request = new MockHttpServletRequest("POST", "/api/v1/users");
	}

	@Test
	void handleValidation_shouldCollectFirstErrorPerField() {
		// Arrange
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new DtoUserIU(), "dtoUserIU");
		bindingResult.addError(new FieldError("dtoUserIU", "email", "must be a well-formed email address"));
		bindingResult.addError(new FieldError("dtoUserIU", "email", "E-mail in use"));
		bindingResult.addError(new FieldError("dtoUserIU", "password", "size must be between 8 and 255"));
		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
		when(messageResolver.get("hoaxify.error.validation")).thenReturn("Validation error");

		// Act
		ResponseEntity<ApiError<Map<String, String>>> result = exceptionHandler.handleValidation(exception, request);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
		ApiError<Map<String, String>> body = result.getBody();
		assertEquals(400, body.getStatus());
		assertEquals("Validation error", body.getMessage());
		assertEquals("/api/v1/users", body.getPath());
		assertNotNull(body.getId());
		assertNotNull(body.getTimestamp());
		assertEquals(2, body.getValidationErrors().size());
		assertEquals("must be a well-formed email address", body.getValidationErrors().get("email"));
	}

	@Test
	void handleBaseException_withoutField_shouldUseMessageTypeStatus() {
		// Arrange
		BaseException exception = new BaseException(MessageType.USER_NOT_FOUND, "5");
		when(messageResolver.get("hoaxify.user.not.found", "5")).thenReturn("User with ID 5 does not exist");

		// Act
		ResponseEntity<ApiError<Map<String, String>>> result = exceptionHandler.handleBaseException(exception, request);

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
		assertEquals("User with ID 5 does not exist", result.getBody().getMessage());
		assertNull(result.getBody().getValidationErrors());
		verify(messageResolver, times(1)).get("hoaxify.user.not.found", "5");
	}

	@Test
	void handleBaseException_withField_shouldReturnValidationErrors() {
		// Arrange
		BaseException exception = BaseException.forField(MessageType.EMAIL_NOT_UNIQUE, "email");
		when(messageResolver.get("hoaxify.constraint.email.notunique")).thenReturn("E-mail in use");
		when(messageResolver.get("hoaxify.error.validation")).thenReturn("Validation error");

		// Act
		ResponseEntity<ApiError<Map<String, String>>> result = exceptionHandler.handleBaseException(exception, request);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
		assertEquals("Validation error", result.getBody().getMessage());
		assertEquals("E-mail in use", result.getBody().getValidationErrors().get("email"));
	}

	@Test
	void handleBaseException_serverError_shouldReturn5xx() {
		// Arrange
		BaseException exception = new BaseException(MessageType.FILE_SAVE_FAILURE);
		when(messageResolver.get("hoaxify.file.save.failure")).thenReturn("File could not be saved");

		// Act
		ResponseEntity<ApiError<Map<String, String>>> result = exceptionHandler.handleBaseException(exception, request);

		// Assert
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
		assertEquals(500, result.getBody().getStatus());
	}

	@Test
	void handleAuthentication_disabledUser() {
		// Arrange
		when(messageResolver.get("hoaxify.user.not.active")).thenReturn("Account is not activated yet");

		// Act
		ResponseEntity<ApiError<Map<String, String>>> result = exceptionHandler
				.handleAuthentication(new DisabledException("disabled"), request);

		// Assert
		assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
		assertEquals("Account is not activated yet", result.getBody().getMessage());
	}

	@Test
	void handleAuthentication_missingToken() {
		// Arrange
		when(messageResolver.get("hoaxify.error.unauthorized")).thenReturn("Authentication is required");

		// Act
		ResponseEntity<ApiError<Map<String, String>>> result = exceptionHandler
				.handleAuthentication(new InsufficientAuthenticationException("no token"), request);

		// Assert
		assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
		assertEquals("Authentication is required", result.getBody().getMessage());
	}

	@Test
	void handleAccessDenied_shouldReturn403() {
		// Arrange
		when(messageResolver.get("hoaxify.error.forbidden")).thenReturn("Forbidden");

		// Act
		ResponseEntity<ApiError<Map<String, String>>> result = exceptionHandler
				.handleAccessDenied(new AccessDeniedException("denied"), request);

		// Assert
		assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
		assertEquals(403, result.getBody().getStatus());
	}

	@Test
	void handleBadRequest_malformedJson_shouldReturn400() {
		// Arrange
		HttpMessageNotReadableException exception = new HttpMessageNotReadableException("bad json",
				new MockHttpInputMessage(new byte[0]));
		when(messageResolver.get("hoaxify.error.bad.request")).thenReturn("Malformed request");

		// Act
		ResponseEntity<ApiError<Map<String, String>>> result = exceptionHandler.handleBadRequest(exception, request);

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
		assertEquals("Malformed request", result.getBody().getMessage());
	}

	@Test
	void handleUnknown_springErrorResponse_shouldKeepItsStatus() {
		// Arrange
		NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "api/v1/nothing");

		// Act
		ResponseEntity<ApiError<Map<String, String>>> result = exceptionHandler.handleUnknown(exception, request);

		// Assert: ErrorResponse branch'i -> MessageResolver'a gidilmez, Spring'in status'u kullanılır
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
		assertEquals(404, result.getBody().getStatus());
	}

	@Test
	void handleUnknown_unexpectedException_shouldReturn500WithGenericMessage() {
		// Arrange
		when(messageResolver.get("hoaxify.error.unexpected")).thenReturn("An unexpected error occurred");

		// Act
		ResponseEntity<ApiError<Map<String, String>>> result = exceptionHandler
				.handleUnknown(new IllegalStateException("secret internal detail"), request);

		// Assert: iç hata detayı client'a sızdırılmaz
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
		assertEquals("An unexpected error occurred", result.getBody().getMessage());
	}
}
