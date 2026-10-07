package com.hoaxify.ws.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hoaxify.ws.repository.UserRepository;

import jakarta.validation.ConstraintValidatorContext;

/**
 * Tespit edilen senaryolar:
 *  isValid : e-posta kayıtlı -> false | e-posta boşta -> true | null / boş (erken return, DB'ye gidilmez)
 */
@ExtendWith(MockitoExtension.class)
class UniqueEmailValidatorTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private ConstraintValidatorContext context;

	@InjectMocks
	private UniqueEmailValidator validator;

	@Test
	void isValid_emailTaken_shouldReturnFalse() {
		// Arrange
		when(userRepository.existsByEmail("taken@mail.com")).thenReturn(true);

		// Act & Assert
		assertFalse(validator.isValid("taken@mail.com", context));
		verify(userRepository, times(1)).existsByEmail("taken@mail.com");
	}

	@Test
	void isValid_emailFree_shouldReturnTrue() {
		// Arrange
		when(userRepository.existsByEmail("free@mail.com")).thenReturn(false);

		// Act & Assert
		assertTrue(validator.isValid("free@mail.com", context));
	}

	@Test
	void isValid_null_shouldNotCallRepository() {
		assertTrue(validator.isValid(null, context));
		verifyNoInteractions(userRepository);
	}

	@Test
	void isValid_blank_shouldNotCallRepository() {
		assertTrue(validator.isValid("   ", context));
		verifyNoInteractions(userRepository);
	}
}
