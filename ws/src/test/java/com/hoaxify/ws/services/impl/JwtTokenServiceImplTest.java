package com.hoaxify.ws.services.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hoaxify.ws.configuration.HoaxifyProperties;
import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoToken;
import com.hoaxify.ws.entities.User;

/**
 * JwtTokenServiceImpl'in mock'lanacak bir bağımlılığı yok (HoaxifyProperties sadece veri taşıyor),
 * bu yüzden MockitoExtension kullanmıyoruz. Gerçek imzalama/doğrulama test ediliyor.
 *
 * Tespit edilen senaryolar:
 *  constructor : secret null | secret 32 karakterden kısa
 *  createToken + verifyToken : success (id + active claim) | aktif olmayan kullanıcı
 *  verifyToken : token bozulmuş | başka anahtarla imzalanmış | süresi dolmuş | JWT değil
 *  logout      : hiçbir şey yapmaz
 */
class JwtTokenServiceImplTest {

	private static final String SECRET = "a-very-secret-key-that-is-at-least-32-chars";

	private EasyRandom easyRandom;

	private DtoCredentialsIU credentials;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
		credentials = easyRandom.nextObject(DtoCredentialsIU.class);
	}

	private JwtTokenServiceImpl createService(String secret, Duration validity) {
		HoaxifyProperties properties = new HoaxifyProperties();
		properties.getJwt().setSecret(secret);
		properties.getJwt().setValidity(validity);
		return new JwtTokenServiceImpl(properties);
	}

	@Test
	void constructor_nullSecret_shouldThrow() {
		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> createService(null, Duration.ofHours(1)));

		assertTrue(exception.getMessage().contains("32"));
	}

	@Test
	void constructor_shortSecret_shouldThrow() {
		assertThrows(IllegalStateException.class, () -> createService("short", Duration.ofHours(1)));
	}

	@Test
	void createAndVerifyToken_success() {
		// Arrange
		JwtTokenServiceImpl tokenService = createService(SECRET, Duration.ofHours(1));
		User user = easyRandom.nextObject(User.class);
		user.setId(42L);
		user.setActive(true);

		// Act
		DtoToken token = tokenService.createToken(user, credentials);
		Optional<User> result = tokenService.verifyToken(token.getToken());

		// Assert
		assertEquals("Bearer", token.getPrefix());
		assertEquals(3, token.getToken().split("\\.").length); // header.payload.signature
		assertTrue(result.isPresent());
		assertEquals(42L, result.get().getId());
		assertTrue(result.get().isActive());
	}

	@Test
	void createAndVerifyToken_inactiveUser_shouldCarryActiveFalse() {
		// Arrange
		JwtTokenServiceImpl tokenService = createService(SECRET, Duration.ofHours(1));
		User user = easyRandom.nextObject(User.class);
		user.setActive(false);

		// Act
		Optional<User> result = tokenService.verifyToken(tokenService.createToken(user, credentials).getToken());

		// Assert
		assertTrue(result.isPresent());
		assertFalse(result.get().isActive());
	}

	@Test
	void verifyToken_tampered_shouldReturnEmpty() {
		// Arrange
		JwtTokenServiceImpl tokenService = createService(SECRET, Duration.ofHours(1));
		String token = tokenService.createToken(easyRandom.nextObject(User.class), credentials).getToken();

		// Act
		Optional<User> result = tokenService.verifyToken(token + "x");

		// Assert
		assertFalse(result.isPresent());
	}

	@Test
	void verifyToken_signedWithAnotherKey_shouldReturnEmpty() {
		// Arrange
		String foreignToken = createService("another-secret-key-that-is-at-least-32-ch", Duration.ofHours(1))
				.createToken(easyRandom.nextObject(User.class), credentials).getToken();
		JwtTokenServiceImpl tokenService = createService(SECRET, Duration.ofHours(1));

		// Act
		Optional<User> result = tokenService.verifyToken(foreignToken);

		// Assert
		assertFalse(result.isPresent());
	}

	@Test
	void verifyToken_expired_shouldReturnEmpty() {
		// Arrange
		JwtTokenServiceImpl tokenService = createService(SECRET, Duration.ofSeconds(-1));
		String token = tokenService.createToken(easyRandom.nextObject(User.class), credentials).getToken();

		// Act
		Optional<User> result = tokenService.verifyToken(token);

		// Assert
		assertFalse(result.isPresent());
	}

	@Test
	void verifyToken_notAJwt_shouldReturnEmpty() {
		// Arrange
		JwtTokenServiceImpl tokenService = createService(SECRET, Duration.ofHours(1));

		// Act
		Optional<User> result = tokenService.verifyToken("not-a-jwt");

		// Assert
		assertNotNull(result);
		assertFalse(result.isPresent());
	}

	@Test
	void logout_shouldDoNothing() {
		JwtTokenServiceImpl tokenService = createService(SECRET, Duration.ofHours(1));

		assertDoesNotThrow(() -> tokenService.logout("any"));
	}
}
