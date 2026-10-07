package com.hoaxify.ws.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hoaxify.ws.services.IFileService;

import jakarta.validation.ConstraintValidatorContext;

/**
 * Tespit edilen senaryolar:
 *  isValid : null / boş (erken return) | izin verilen tip (png) | izin verilen tip (jpeg)
 *            | izin verilmeyen tip (mesaja tipler eklenir) | base64 değil (catch bloğu)
 */
@ExtendWith(MockitoExtension.class)
class FileTypeValidatorTest {

	@Mock
	private IFileService fileService;

	@Mock
	private ConstraintValidatorContext context;

	@Mock
	private FileType annotation;

	@InjectMocks
	private FileTypeValidator validator;

	@BeforeEach
	void setUp() {
		when(annotation.types()).thenReturn(new String[] { "jpeg", "png" });
		validator.initialize(annotation);
	}

	@Test
	void isValid_nullOrEmpty_shouldNotCallFileService() {
		assertTrue(validator.isValid(null, context));
		assertTrue(validator.isValid("", context));
		verifyNoInteractions(fileService);
	}

	@Test
	void isValid_png_shouldReturnTrue() {
		// Arrange
		when(fileService.detectType("png-data")).thenReturn("image/png");

		// Act & Assert
		assertTrue(validator.isValid("png-data", context));
		verify(fileService, times(1)).detectType("png-data");
		verifyNoInteractions(context);
	}

	@Test
	void isValid_jpeg_shouldReturnTrue() {
		// Arrange
		when(fileService.detectType("jpeg-data")).thenReturn("image/jpeg");

		// Act & Assert
		assertTrue(validator.isValid("jpeg-data", context));
	}

	@Test
	void isValid_notAllowedType_shouldReturnFalseWithTypesInMessage() {
		// Arrange
		HibernateConstraintValidatorContext hibernateContext = mock(HibernateConstraintValidatorContext.class,
				RETURNS_DEEP_STUBS);
		when(fileService.detectType("text-data")).thenReturn("text/plain");
		when(context.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(hibernateContext);

		// Act
		boolean result = validator.isValid("text-data", context);

		// Assert
		assertFalse(result);
		verify(context, times(1)).disableDefaultConstraintViolation();
		verify(hibernateContext, times(1)).addMessageParameter("types", "jpeg, png");
	}

	@Test
	void isValid_notBase64_shouldReturnFalse() {
		// Arrange
		HibernateConstraintValidatorContext hibernateContext = mock(HibernateConstraintValidatorContext.class,
				RETURNS_DEEP_STUBS);
		when(fileService.detectType("%%%")).thenThrow(new IllegalArgumentException("Illegal base64 character"));
		when(context.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(hibernateContext);

		// Act & Assert: IllegalArgumentException yakalanır, tip "" kabul edilir -> geçersiz
		assertFalse(validator.isValid("%%%", context));
	}
}
