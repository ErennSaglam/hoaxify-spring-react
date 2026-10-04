package com.hoaxify.ws.services.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoToken;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.repository.UserRepository;
import com.hoaxify.ws.services.ITokenService;

import lombok.RequiredArgsConstructor;

/**
 * HTTP Basic: token = base64("email:password"). Sadece eğitim amaçlı karşılaştırma için;
 * şifre her istekte (kolayca decode edilebilir halde) gider ve her istekte BCrypt çalışır.
 * Üretimde kullanılmaz.
 */
@Service
@ConditionalOnProperty(name = "hoaxify.token-type", havingValue = "basic")
@RequiredArgsConstructor
public class BasicAuthTokenServiceImpl implements ITokenService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public DtoToken createToken(User user, DtoCredentialsIU credentials) {
		String emailColonPassword = credentials.getEmail() + ":" + credentials.getPassword();
		String token = Base64.getEncoder().encodeToString(emailColonPassword.getBytes(StandardCharsets.UTF_8));
		return new DtoToken("Basic", token);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> verifyToken(String token) {
		String decoded;
		try {
			decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
		String[] parts = decoded.split(":", 2);
		if (parts.length != 2) {
			return Optional.empty();
		}
		return userRepository.findByEmail(parts[0])
				.filter(user -> passwordEncoder.matches(parts[1], user.getPassword()));
	}

	@Override
	public void logout(String token) {
		// Sunucuda saklanan bir oturum yok
	}
}
