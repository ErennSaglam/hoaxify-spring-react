package com.hoaxify.ws.validation;

import com.hoaxify.ws.repository.UserRepository;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

/**
 * Validator'ları Spring oluşturduğu için (SpringConstraintValidatorFactory)
 * constructor injection burada da çalışır.
 */
@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

	private final UserRepository userRepository;

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		// null/boş kontrolü @NotBlank'in işi; burada sadece benzersizliğe bakıyoruz
		if (value == null || value.isBlank()) {
			return true;
		}
		return !userRepository.existsByEmail(value);
	}
}
