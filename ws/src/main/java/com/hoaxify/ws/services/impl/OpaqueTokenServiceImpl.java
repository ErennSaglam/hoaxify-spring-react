package com.hoaxify.ws.services.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoToken;
import com.hoaxify.ws.entities.Token;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.repository.TokenRepository;
import com.hoaxify.ws.services.ITokenService;

import lombok.RequiredArgsConstructor;

/**
 * Rastgele (anlamsız) bir değer üretip veritabanında kullanıcıyla eşleştirir.
 * + Logout / hesap silme anında token gerçekten geçersiz olur.
 * - Her istekte bir veritabanı sorgusu gerekir.
 */
@Service
@ConditionalOnProperty(name = "hoaxify.token-type", havingValue = "opaque", matchIfMissing = true)
@RequiredArgsConstructor
public class OpaqueTokenServiceImpl implements ITokenService {

	private final TokenRepository tokenRepository;

	@Override
	@Transactional
	public DtoToken createToken(User user, DtoCredentialsIU credentials) {
		Token token = new Token(UUID.randomUUID().toString(), user);
		tokenRepository.save(token);
		return new DtoToken("Bearer", token.getToken());
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> verifyToken(String token) {
		return tokenRepository.findById(token).map(Token::getUser);
	}

	@Override
	@Transactional
	public void logout(String token) {
		tokenRepository.deleteById(token);
	}
}
