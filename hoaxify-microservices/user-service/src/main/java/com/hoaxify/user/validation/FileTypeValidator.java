package com.hoaxify.user.validation;

import java.util.Arrays;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import com.hoaxify.user.services.IFileService;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileTypeValidator implements ConstraintValidator<FileType, String> {

	private final IFileService fileService;

	private String[] types;

	@Override
	public void initialize(FileType constraintAnnotation) {
		this.types = constraintAnnotation.types();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isEmpty()) {
			return true;
		}
		String detectedType;
		try {
			detectedType = fileService.detectType(value);
		} catch (IllegalArgumentException ex) {
			detectedType = "";
		}
		for (String validType : types) {
			if (detectedType.contains(validType)) {
				return true;
			}
		}
		context.disableDefaultConstraintViolation();
		HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
		hibernateContext.addMessageParameter("types", String.join(", ", Arrays.asList(types)));
		hibernateContext.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
				.addConstraintViolation();
		return false;
	}
}
