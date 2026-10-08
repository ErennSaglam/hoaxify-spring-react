package com.hoaxify.common.web.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import lombok.RequiredArgsConstructor;

/** İsteğin diline (Accept-Language) göre mesaj okur. Bean olarak auto-configuration'da tanımlanır. */
@RequiredArgsConstructor
public class MessageResolver {

	private final MessageSource messageSource;

	public String get(String key, Object... args) {
		return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
	}
}
