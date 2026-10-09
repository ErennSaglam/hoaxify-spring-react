package com.hoaxify.auth.store;

import java.util.Optional;

/**
 * Oturum token'larının saklandığı yer. Monolith'te PostgreSQL'deki "token" tablosuydu;
 * burada Redis: her istekte okunduğu için hızlı olmalı ve süresi dolanı Redis kendisi siler (TTL).
 * Arayüz sayesinde servis Redis'i bilmez; yarın başka bir depoya geçmek sadece impl'i değiştirir.
 */
public interface ITokenStore {

	String create(Long userId);

	Optional<Long> findUserId(String token);

	void revoke(String token);

	/** Kullanıcının tüm oturumlarını kapatır (hesap silindi, şifre değişti) */
	void revokeAll(Long userId);
}
