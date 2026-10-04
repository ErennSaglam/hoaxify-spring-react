package com.hoaxify.ws.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * messages*.properties dosyalarından, isteğin diline (Accept-Language) göre mesaj okur.
 */
@Component
@RequiredArgsConstructor
public class MessageResolver {

	private final MessageSource messageSource;

	public String get(String key, Object... args) {
		return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
	}
}
