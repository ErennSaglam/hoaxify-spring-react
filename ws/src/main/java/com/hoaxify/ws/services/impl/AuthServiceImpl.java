package com.hoaxify.ws.services.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.ws.dto.DtoAuthResponse;
import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoToken;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.mapper.UserMapper;
import com.hoaxify.ws.repository.UserRepository;
import com.hoaxify.ws.services.IAuthService;
import com.hoaxify.ws.services.ITokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ITokenService tokenService;
	private final UserMapper userMapper;

	@Override
	@Transactional
	public DtoAuthResponse authenticate(DtoCredentialsIU credentials) {
		// E-posta yok da, şifre yanlış da aynı hata: hangisinin yanlış olduğunu söylemiyoruz
		User user = userRepository.findByEmail(credentials.getEmail())
				.filter(u -> passwordEncoder.matches(credentials.getPassword(), u.getPassword()))
				.orElseThrow(() -> new BaseException(MessageType.INVALID_CREDENTIALS));

		if (!user.isActive()) {
			throw new BaseException(MessageType.USER_NOT_ACTIVE);
		}

		DtoToken token = tokenService.createToken(user, credentials);
		return new DtoAuthResponse(userMapper.toDto(user), token);
	}

	@Override
	@Transactional
	public void logout(String token) {
		tokenService.logout(token);
	}
}
