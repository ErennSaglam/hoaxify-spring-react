package com.hoaxify.ws.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hoaxify.ws.dto.DtoAuthResponse;
import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoToken;
import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.mapper.UserMapper;
import com.hoaxify.ws.repository.UserRepository;
import com.hoaxify.ws.services.ITokenService;

/**
 * Tespit edilen senaryolar:
 *  authenticate : success | e-posta yok (Optional.empty) | şifre yanlış (filter false)
 *                 | kullanıcı aktif değil | token servisi hata | repository hata
 *  logout       : token servise iletilir | token servisi hata
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ITokenService tokenService;

	@Mock
	private UserMapper userMapper;

	@InjectMocks
	private AuthServiceImpl authService;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	@Test
	void authenticate_success() {
		// Arrange
		DtoCredentialsIU credentials = easyRandom.nextObject(DtoCredentialsIU.class);
		User user = easyRandom.nextObject(User.class);
		user.setActive(true);
		DtoToken token = easyRandom.nextObject(DtoToken.class);
		DtoUser dtoUser = easyRandom.nextObject(DtoUser.class);

		when(userRepository.findByEmail(credentials.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(credentials.getPassword(), user.getPassword())).thenReturn(true);
		when(tokenService.createToken(user, credentials)).thenReturn(token);
		when(userMapper.toDto(user)).thenReturn(dtoUser);

		// Act
		DtoAuthResponse result = authService.authenticate(credentials);

		// Assert
		assertNotNull(result);
		assertEquals(dtoUser, result.getUser());
		assertEquals(token.getToken(), result.getToken().getToken());
		assertEquals(token.getPrefix(), result.getToken().getPrefix());

		verify(userRepository, times(1)).findByEmail(credentials.getEmail());
		verify(tokenService, times(1)).createToken(user, credentials);
	}

	@Test
	void authenticate_emailNotFound_shouldThrowInvalidCredentials() {
		// Arrange
		DtoCredentialsIU credentials = easyRandom.nextObject(DtoCredentialsIU.class);
		when(userRepository.findByEmail(credentials.getEmail())).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> authService.authenticate(credentials));

		// Assert
		assertEquals(MessageType.INVALID_CREDENTIALS, exception.getMessageType());
		verifyNoInteractions(passwordEncoder, tokenService, userMapper);
	}

	@Test
	void authenticate_wrongPassword_shouldThrowInvalidCredentials() {
		// Arrange
		DtoCredentialsIU credentials = easyRandom.nextObject(DtoCredentialsIU.class);
		User user = easyRandom.nextObject(User.class);

		when(userRepository.findByEmail(credentials.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(credentials.getPassword(), user.getPassword())).thenReturn(false);

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> authService.authenticate(credentials));

		// Assert: e-posta yok ile şifre yanlış aynı hatayı döner (hangisinin yanlış olduğu sızdırılmaz)
		assertEquals(MessageType.INVALID_CREDENTIALS, exception.getMessageType());
		verify(tokenService, never()).createToken(any(), any());
	}

	@Test
	void authenticate_inactiveUser_shouldThrowUserNotActive() {
		// Arrange
		DtoCredentialsIU credentials = easyRandom.nextObject(DtoCredentialsIU.class);
		User user = easyRandom.nextObject(User.class);
		user.setActive(false);

		when(userRepository.findByEmail(credentials.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(credentials.getPassword(), user.getPassword())).thenReturn(true);

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> authService.authenticate(credentials));

		// Assert
		assertEquals(MessageType.USER_NOT_ACTIVE, exception.getMessageType());
		verify(tokenService, never()).createToken(any(), any());
		verifyNoInteractions(userMapper);
	}

	@Test
	void authenticate_tokenServiceException_shouldPropagate() {
		// Arrange
		DtoCredentialsIU credentials = easyRandom.nextObject(DtoCredentialsIU.class);
		User user = easyRandom.nextObject(User.class);
		user.setActive(true);

		when(userRepository.findByEmail(credentials.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(credentials.getPassword(), user.getPassword())).thenReturn(true);
		when(tokenService.createToken(user, credentials)).thenThrow(new RuntimeException("Token store down"));

		// Act & Assert
		assertThrows(RuntimeException.class, () -> authService.authenticate(credentials));
		verifyNoInteractions(userMapper);
	}

	@Test
	void authenticate_repositoryException() {
		// Arrange
		DtoCredentialsIU credentials = easyRandom.nextObject(DtoCredentialsIU.class);
		when(userRepository.findByEmail(credentials.getEmail())).thenThrow(new RuntimeException("DB error"));

		// Act & Assert
		assertThrows(RuntimeException.class, () -> authService.authenticate(credentials));
		verifyNoInteractions(tokenService);
	}

	@Test
	void logout_success() {
		// Arrange
		String token = "token-value";

		// Act
		authService.logout(token);

		// Assert
		verify(tokenService, times(1)).logout(token);
	}

	@Test
	void logout_tokenServiceException_shouldPropagate() {
		// Arrange
		String token = "token-value";
		doThrow(new RuntimeException("DB error")).when(tokenService).logout(token);

		// Act & Assert
		assertThrows(RuntimeException.class, () -> authService.logout(token));
		verify(tokenService, times(1)).logout(token);
	}
}
