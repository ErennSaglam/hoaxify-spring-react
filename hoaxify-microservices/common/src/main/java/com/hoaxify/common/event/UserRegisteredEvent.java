package com.hoaxify.common.event;

/**
 * auth-service yayınlar -> user-service profil oluşturur, notification-service aktivasyon maili gönderir.
 *
 * Olaylar geçmiş zamanlı isimlendirilir ("kayıt OLDU"): bir emir değil, olmuş bir gerçeğin duyurusudur.
 * locale: mail, kullanıcının kayıt olduğu dilde gönderilsin diye.
 */
public record UserRegisteredEvent(
		Long userId,
		String username,
		String email,
		String activationToken,
		String locale) {
}
