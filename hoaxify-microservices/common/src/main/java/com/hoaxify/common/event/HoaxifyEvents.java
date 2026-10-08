package com.hoaxify.common.event;

/**
 * RabbitMQ'daki olay (event) altyapısının isimleri tek yerde.
 *
 * Topic exchange: yayıncı (publisher) mesajı "user.registered" gibi bir routing key ile exchange'e atar,
 * hangi kuyruklara gideceğini bilmez. Her dinleyici (consumer) servis kendi kuyruğunu açıp ilgilendiği
 * routing key'lere bağlar (binding). Yeni bir servis eklemek için yayıncıyı değiştirmek gerekmez.
 */
public final class HoaxifyEvents {

	public static final String EXCHANGE = "hoaxify.events";

	/** İşlenemeyen (retry'lar tükenen) mesajların gittiği exchange ve kuyruk */
	public static final String DEAD_LETTER_EXCHANGE = "hoaxify.events.dlx";
	public static final String DEAD_LETTER_QUEUE = "hoaxify.dead-letter";

	public static final String USER_REGISTERED = "user.registered";
	public static final String USER_DELETED = "user.deleted";
	public static final String PASSWORD_RESET_REQUESTED = "user.password-reset-requested";

	private HoaxifyEvents() {
	}
}
