package com.hoaxify.ws.exception;

import lombok.Getter;

/**
 * Tüm iş (business) hatalarının tek exception sınıfı. Servisler null dönmek yerine
 * bunu fırlatır, GlobalExceptionHandler yakalayıp ApiError'a çevirir.
 *
 * Örnek: throw new BaseException(MessageType.USER_NOT_FOUND, id);
 */
@Getter
public class BaseException extends RuntimeException {

	private final MessageType messageType;

	/** Mesajdaki {0}, {1} yer tutucularına gelecek değerler */
	private final transient Object[] args;

	/** Hata belirli bir form alanına aitse (ör. "email"), frontend onu alanın altında gösterir */
	private final String field;

	public BaseException(MessageType messageType, Object... args) {
		this(messageType, null, args);
	}

	private BaseException(MessageType messageType, String field, Object[] args) {
		super(messageType.name());
		this.messageType = messageType;
		this.field = field;
		this.args = args;
	}

	public static BaseException forField(MessageType messageType, String field) {
		return new BaseException(messageType, field, new Object[0]);
	}
}
