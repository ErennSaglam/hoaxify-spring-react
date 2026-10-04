package com.hoaxify.ws.services;

import java.util.Optional;

import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoToken;
import com.hoaxify.ws.entities.User;

/**
 * Strategy pattern: aynı arayüzün üç implementasyonu var (Basic, JWT, Opaque).
 * Hangisinin bean olacağı hoaxify.token-type ayarıyla (@ConditionalOnProperty) seçilir.
 * Kullanan sınıflar (AuthServiceImpl, TokenFilter) sadece bu arayüzü bilir.
 *
 * verifyToken bir entity döner, çünkü sadece security katmanı içinde kullanılır;
 * controller'a kadar çıkmaz.
 */
public interface ITokenService {

	DtoToken createToken(User user, DtoCredentialsIU credentials);

	/** @param token Authorization başlığındaki prefix'siz değer ya da cookie değeri */
	Optional<User> verifyToken(String token);

	void logout(String token);
}
