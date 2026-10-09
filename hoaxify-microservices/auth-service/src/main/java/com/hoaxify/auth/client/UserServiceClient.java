package com.hoaxify.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.hoaxify.auth.dto.DtoUser;

/**
 * OpenFeign: arayüzü yazarız, HTTP istemcisini Spring üretir (kursundaki UserProfileManager'ın aynısı).
 * Fark: URL koda gömülü değil, config-server'dan geliyor (hoaxify.clients.user-service-url).
 * /internal/** uçları gateway'de dışarıya açılmaz; sadece servisler arası kullanılır.
 */
@FeignClient(name = "user-service", url = "${hoaxify.clients.user-service-url}", path = "/internal/users")
public interface UserServiceClient {

	@GetMapping("/{id}")
	DtoUser getUser(@PathVariable("id") Long id);
}
