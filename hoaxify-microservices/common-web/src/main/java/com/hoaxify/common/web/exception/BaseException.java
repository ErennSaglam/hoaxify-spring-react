package com.hoaxify.common.web.exception;

import lombok.Getter;

/**
 * Tüm servislerin iş hatası. Monolith'teki BaseException'ın aynısı; tek fark MessageType yerine
 * ErrorType arayüzünü alması (her servis kendi enum'unu getirir).
 */
@Getter
public class BaseException extends RuntimeException {

	private final ErrorType errorType;

	private final transient Object[] args;

	/** Hata bir form alanına aitse (ör. "email") frontend onu alanın altında gösterir */
	private final String field;

	public BaseException(ErrorType errorType, Object... args) {
		this(errorType, null, args);
	}

	private BaseException(ErrorType errorType, String field, Object[] args) {
		super(errorType.getMessageKey());
		this.errorType = errorType;
		this.field = field;
		this.args = args;
	}

	public static BaseException forField(ErrorType errorType, String field) {
		return new BaseException(errorType, field, new Object[0]);
	}
}
