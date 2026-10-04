package com.hoaxify.ws.services;

import com.hoaxify.ws.dto.DtoAuthResponse;
import com.hoaxify.ws.dto.DtoCredentialsIU;

public interface IAuthService {

	DtoAuthResponse authenticate(DtoCredentialsIU credentials);

	void logout(String token);
}
