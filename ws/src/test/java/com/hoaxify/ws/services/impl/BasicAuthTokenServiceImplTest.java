package com.hoaxify.ws.services.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoToken;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.repository.UserRepository;

/**
 * Tespit edilen senaryolar:
 *  createToken : base64(email:password) üretilir
 *  verifyToken : success | şifre yanlış | e-posta yok | base64 değil (catch, erken return)
 *                | ":" yok (erken return)
 *  logout      : hiçbir şey yapmaz
 */
@ExtendWith(MockitoExtension.class)
class BasicAuthTokenServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private BasicAuthTokenServiceImpl tokenService;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	private static String base64(String value) {
		return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void createToken_success() {
		// Arrange
		User user = easyRandom.nextObject(User.class);
		DtoCredentialsIU credentials = new DtoCredentialsIU("user1@mail.com", "P4ssword");

		// Act
		DtoToken result = tokenService.createToken(user, credentials);

		// Assert
		assertEquals("Basic", result.getPrefix());
		assertEquals(base64("user1@mail.com:P4ssword"), result.getToken());
		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void verifyToken_success() {
		// Arrange
		User user = easyRandom.nextObject(User.class);
		when(userRepository.findByEmail("user1@mail.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("P4ssword", user.getPassword())).thenReturn(true);

		// Act
		Optional<User> result = tokenService.verifyToken(base64("user1@mail.com:P4ssword"));

		// Assert
		assertTrue(result.isPresent());
		assertSame(user, result.get());
		verify(userRepository, times(1)).findByEmail("user1@mail.com");
	}

	@Test
	void verifyToken_wrongPassword() {
		// Arrange
		User user = easyRandom.nextObject(User.class);
		when(userRepository.findByEmail("user1@mail.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

		// Act
		Optional<User> result = tokenService.verifyToken(base64("user1@mail.com:wrong"));

		// Assert
		assertFalse(result.isPresent());
	}

	@Test
	void verifyToken_emailNotFound_shouldNotCheckPassword() {
		// Arrange
		when(userRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

		// Act
		Optional<User> result = tokenService.verifyToken(base64("nobody@mail.com:P4ssword"));

		// Assert
		assertFalse(result.isPresent());
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void verifyToken_invalidBase64_shouldReturnEmptyWithoutRepository() {
		// Act
		Optional<User> result = tokenService.verifyToken("%%%not-base64%%%");

		// Assert
		assertFalse(result.isPresent());
		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void verifyToken_missingColon_shouldReturnEmptyWithoutRepository() {
		// Act
		Optional<User> result = tokenService.verifyToken(base64("no-colon-here"));

		// Assert
		assertFalse(result.isPresent());
		verifyNoInteractions(userRepository);
	}

	@Test
	void logout_shouldDoNothing() {
		// Act & Assert
		assertDoesNotThrow(() -> tokenService.logout("any"));
		verifyNoInteractions(userRepository, passwordEncoder);
	}
}
