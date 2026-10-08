package com.hoaxify.common.web;

/**
 * Servisler arası HTTP sözleşmesi.
 *
 * Kimlik doğrulama SADECE api-gateway'de yapılır. Gateway token'ı doğrular ve arkadaki servislere
 * kullanıcının id'sini X-User-Id başlığıyla iletir. Servisler token görmez, bu başlığa güvenir.
 * Bu yüzden gateway, dışarıdan gelen X-User-Id başlığını her istekte SİLER (sahtecilik önlemi)
 * ve servis portları dış dünyaya açılmaz; sadece gateway'e açıktır.
 */
public final class HoaxifyHeaders {

	public static final String USER_ID = "X-User-Id";

	/** Tarayıcıdaki httpOnly oturum cookie'si (monolith ile aynı isim, frontend değişmesin diye) */
	public static final String TOKEN_COOKIE = "hoax-token";

	private HoaxifyHeaders() {
	}
}
