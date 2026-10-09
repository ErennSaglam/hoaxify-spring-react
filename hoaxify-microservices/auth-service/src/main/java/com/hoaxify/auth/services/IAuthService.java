package com.hoaxify.auth.services;

import com.hoaxify.auth.dto.DtoAuthResponse;
import com.hoaxify.auth.dto.DtoCredentialsIU;

/** Oturum: giriş, çıkış, token doğrulama */
public interface IAuthService {

	DtoAuthResponse login(DtoCredentialsIU credentials);

	void logout(String token);

	/** Gateway her korumalı istekte bunu çağırır; geçersizse 401 */
	Long verify(String token);
}
