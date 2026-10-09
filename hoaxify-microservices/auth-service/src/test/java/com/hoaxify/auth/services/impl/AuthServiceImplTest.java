package com.hoaxify.auth.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import com.hoaxify.auth.client.UserServiceClient;
import com.hoaxify.auth.dto.DtoAuthResponse;
import com.hoaxify.auth.dto.DtoCredentialsIU;
import com.hoaxify.auth.dto.DtoUser;
import com.hoaxify.auth.entities.Account;
import com.hoaxify.auth.exception.AuthErrorType;
import com.hoaxify.auth.repository.AccountRepository;
import com.hoaxify.auth.store.ITokenStore;
import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.common.web.exception.CommonErrorType;

import feign.FeignException;
import feign.Request;
import feign.Request.HttpMethod;

/**
 * Tespit edilen senaryolar:
 *  login  : success | e-posta yok | şifre yanlış | aktif değil | user-service cevap vermiyor (Feign catch, token üretilmez)
 *  logout : token store'a iletilir
 *  verify : geçerli | geçersiz
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ITokenStore tokenStore;

	@Mock
	private UserServiceClient userServiceClient;

	@InjectMocks
	private AuthServiceImpl authService;

	private EasyRandom easyRandom;

	private DtoCredentialsIU credentials;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
		credentials = easyRandom.nextObject(DtoCredentialsIU.class);
	}

	private Account activeAccount() {
		Account account = easyRandom.nextObject(Account.class);
		account.setId(1L);
		account.setActive(true);
		return account;
	}

	@Test
	void login_success() {
		// Arrange
		Account account = activeAccount();
		DtoUser user = new DtoUser(1L, "user1", "user1@mail.com", null, null);
		when(accountRepository.findByEmail(credentials.getEmail())).thenReturn(Optional.of(account));
		when(passwordEncoder.matches(credentials.getPassword(), account.getPasswordHash())).thenReturn(true);
		when(userServiceClient.getUser(1L)).thenReturn(user);
		when(tokenStore.create(1L)).thenReturn("token-1");

		// Act
		DtoAuthResponse result = authService.login(credentials);

		// Assert
		assertEquals(user, result.user());
		assertEquals("Bearer", result.token().prefix());
		assertEquals("token-1", result.token().token());
		verify(userServiceClient, times(1)).getUser(1L);
	}

	@Test
	void login_emailNotFound() {
		// Arrange
		when(accountRepository.findByEmail(credentials.getEmail())).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> authService.login(credentials));

		// Assert
		assertEquals(AuthErrorType.INVALID_CREDENTIALS, exception.getErrorType());
		verifyNoInteractions(tokenStore, userServiceClient);
	}

	@Test
	void login_wrongPassword() {
		// Arrange
		Account account = activeAccount();
		when(accountRepository.findByEmail(credentials.getEmail())).thenReturn(Optional.of(account));
		when(passwordEncoder.matches(credentials.getPassword(), account.getPasswordHash())).thenReturn(false);

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> authService.login(credentials));

		// Assert
		assertEquals(AuthErrorType.INVALID_CREDENTIALS, exception.getErrorType());
		verifyNoInteractions(tokenStore, userServiceClient);
	}

	@Test
	void login_inactiveAccount() {
		// Arrange
		Account account = activeAccount();
		account.setActive(false);
		when(accountRepository.findByEmail(credentials.getEmail())).thenReturn(Optional.of(account));
		when(passwordEncoder.matches(credentials.getPassword(), account.getPasswordHash())).thenReturn(true);

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> authService.login(credentials));

		// Assert
		assertEquals(AuthErrorType.USER_NOT_ACTIVE, exception.getErrorType());
		verifyNoInteractions(tokenStore);
	}

	@Test
	void login_userServiceUnavailable_shouldNotCreateToken() {
		// Arrange
		Account account = activeAccount();
		when(accountRepository.findByEmail(credentials.getEmail())).thenReturn(Optional.of(account));
		when(passwordEncoder.matches(credentials.getPassword(), account.getPasswordHash())).thenReturn(true);
		Request request = Request.create(HttpMethod.GET, "/internal/users/1", java.util.Map.of(), null, null, null);
		when(userServiceClient.getUser(1L))
				.thenThrow(new FeignException.ServiceUnavailable("down", request, null, null));

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> authService.login(credentials));

		// Assert: FeignException istemciye sızmaz, 503'e çevrilir; boşta token kalmaz
		assertEquals(CommonErrorType.DEPENDENCY_UNAVAILABLE, exception.getErrorType());
		verify(tokenStore, never()).create(any());
	}

	@Test
	void logout_success() {
		// Act
		authService.logout("token-1");

		// Assert
		verify(tokenStore, times(1)).revoke("token-1");
	}

	@Test
	void verify_validToken() {
		// Arrange
		when(tokenStore.findUserId("token-1")).thenReturn(Optional.of(9L));

		// Act & Assert
		assertEquals(9L, authService.verify("token-1"));
	}

	@Test
	void verify_invalidToken() {
		// Arrange
		when(tokenStore.findUserId("bad")).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> authService.verify("bad"));

		// Assert
		assertEquals(AuthErrorType.INVALID_SESSION_TOKEN, exception.getErrorType());
	}
}
