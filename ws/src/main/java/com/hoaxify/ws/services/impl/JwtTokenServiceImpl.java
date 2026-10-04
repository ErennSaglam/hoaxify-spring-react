package com.hoaxify.ws.services.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.hoaxify.ws.configuration.HoaxifyProperties;
import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoToken;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.services.ITokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Kullanıcı bilgisini imzalı bir JWT içine gömer; doğrulama için veritabanına gitmez.
 * + Stateless, ölçeklenmesi kolay.
 * - Süresi dolana kadar geçerlidir; logout sunucu tarafında token'ı iptal edemez
 *   (bunun için blacklist gerekir). Token'daki "active" bilgisi de eskiyebilir.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "hoaxify.token-type", havingValue = "jwt")
public class JwtTokenServiceImpl implements ITokenService {

	private static final String ACTIVE_CLAIM = "active";

	private final SecretKey key;
	private final HoaxifyProperties hoaxifyProperties;

	// Constructor'da ek iş (secret -> SecretKey) yaptığımız için @RequiredArgsConstructor yerine elle yazdık
	public JwtTokenServiceImpl(HoaxifyProperties hoaxifyProperties) {
		this.hoaxifyProperties = hoaxifyProperties;
		String secret = hoaxifyProperties.getJwt().getSecret();
		if (secret == null || secret.length() < 32) {
			throw new IllegalStateException("hoaxify.jwt.secret (JWT_SECRET) must be at least 32 characters");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public DtoToken createToken(User user, DtoCredentialsIU credentials) {
		Date now = new Date();
		String token = Jwts.builder()
				.subject(String.valueOf(user.getId()))
				.claim(ACTIVE_CLAIM, user.isActive())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + hoaxifyProperties.getJwt().getValidity().toMillis()))
				.signWith(key)
				.compact();
		return new DtoToken("Bearer", token);
	}

	@Override
	public Optional<User> verifyToken(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			User user = new User();
			user.setId(Long.valueOf(claims.getSubject()));
			user.setActive(Boolean.TRUE.equals(claims.get(ACTIVE_CLAIM, Boolean.class)));
			return Optional.of(user);
		} catch (JwtException | IllegalArgumentException ex) {
			log.debug("Invalid JWT: {}", ex.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public void logout(String token) {
		// Stateless: sunucuda silinecek bir şey yok, client token'ı/cookie'yi siler
	}
}
