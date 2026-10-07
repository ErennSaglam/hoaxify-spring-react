package com.hoaxify.ws.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoToken;
import com.hoaxify.ws.entities.Token;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.repository.TokenRepository;

/**
 * Tespit edilen senaryolar:
 *  createToken : success (UUID üretilir, kullanıcıya bağlanır) | repository hata
 *  verifyToken : token var | token yok (Optional.empty)
 *  logout      : success | repository hata
 */
@ExtendWith(MockitoExtension.class)
class OpaqueTokenServiceImplTest {

	@Mock
	private TokenRepository tokenRepository;

	@InjectMocks
	private OpaqueTokenServiceImpl tokenService;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	@Test
	void createToken_success() {
		// Arrange
		User user = easyRandom.nextObject(User.class);
		DtoCredentialsIU credentials = easyRandom.nextObject(DtoCredentialsIU.class);
		when(tokenRepository.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		DtoToken result = tokenService.createToken(user, credentials);

		// Assert
		assertNotNull(result);
		assertEquals("Bearer", result.getPrefix());
		assertEquals(36, result.getToken().length()); // UUID

		ArgumentCaptor<Token> captor = ArgumentCaptor.forClass(Token.class);
		verify(tokenRepository, times(1)).save(captor.capture());
		assertEquals(result.getToken(), captor.getValue().getToken());
		assertSame(user, captor.getValue().getUser());
	}

	@Test
	void createToken_repositoryException() {
		// Arrange
		User user = easyRandom.nextObject(User.class);
		DtoCredentialsIU credentials = easyRandom.nextObject(DtoCredentialsIU.class);
		when(tokenRepository.save(any(Token.class))).thenThrow(new RuntimeException("DB error"));

		// Act & Assert
		assertThrows(RuntimeException.class, () -> tokenService.createToken(user, credentials));
	}

	@Test
	void verifyToken_success() {
		// Arrange
		Token token = easyRandom.nextObject(Token.class);
		when(tokenRepository.findById(token.getToken())).thenReturn(Optional.of(token));

		// Act
		Optional<User> result = tokenService.verifyToken(token.getToken());

		// Assert
		assertTrue(result.isPresent());
		assertSame(token.getUser(), result.get());
		verify(tokenRepository, times(1)).findById(token.getToken());
	}

	@Test
	void verifyToken_notFound() {
		// Arrange
		String token = "unknown";
		when(tokenRepository.findById(token)).thenReturn(Optional.empty());

		// Act
		Optional<User> result = tokenService.verifyToken(token);

		// Assert
		assertFalse(result.isPresent());
		verify(tokenRepository, times(1)).findById(token);
	}

	@Test
	void logout_success() {
		// Arrange
		String token = "token-value";

		// Act
		tokenService.logout(token);

		// Assert
		verify(tokenRepository, times(1)).deleteById(token);
	}

	@Test
	void logout_repositoryException() {
		// Arrange
		String token = "token-value";
		doThrow(new RuntimeException("DB error")).when(tokenRepository).deleteById(token);

		// Act & Assert
		assertThrows(RuntimeException.class, () -> tokenService.logout(token));
	}
}
