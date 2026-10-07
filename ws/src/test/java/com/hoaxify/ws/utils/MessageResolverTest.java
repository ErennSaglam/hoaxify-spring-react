package com.hoaxify.ws.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
class MessageResolverTest {

	@Mock
	private MessageSource messageSource;

	@InjectMocks
	private MessageResolver messageResolver;

	@AfterEach
	void tearDown() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void get_shouldUseRequestLocaleArgsAndKeyAsDefault() {
		// Arrange
		Locale turkish = Locale.forLanguageTag("tr");
		LocaleContextHolder.setLocale(turkish);
		Object[] args = { "5" };
		when(messageSource.getMessage("hoaxify.user.not.found", args, "hoaxify.user.not.found", turkish))
				.thenReturn("5 numarali kullanici bulunamadi");

		// Act
		String result = messageResolver.get("hoaxify.user.not.found", args);

		// Assert
		assertEquals("5 numarali kullanici bulunamadi", result);
		verify(messageSource, times(1)).getMessage("hoaxify.user.not.found", args, "hoaxify.user.not.found", turkish);
	}

	@Test
	void get_missingKey_shouldReturnKeyItself() {
		// Arrange
		LocaleContextHolder.setLocale(Locale.ENGLISH);
		Object[] noArgs = {};
		when(messageSource.getMessage("unknown.key", noArgs, "unknown.key", Locale.ENGLISH)).thenReturn("unknown.key");

		// Act
		String result = messageResolver.get("unknown.key");

		// Assert
		assertEquals("unknown.key", result);
	}
}
