package com.hoaxify.auth.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hoaxify.auth.dto.DtoPasswordResetIU;
import com.hoaxify.auth.dto.DtoPasswordUpdateIU;
import com.hoaxify.auth.dto.DtoRegisterIU;
import com.hoaxify.auth.entities.Account;
import com.hoaxify.auth.exception.AuthErrorType;
import com.hoaxify.auth.repository.AccountRepository;
import com.hoaxify.auth.store.ITokenStore;
import com.hoaxify.common.event.PasswordResetRequestedEvent;
import com.hoaxify.common.event.UserRegisteredEvent;
import com.hoaxify.common.web.exception.BaseException;

/**
 * Tespit edilen senaryolar:
 *  register             : success (olay yayınlanır) | e-posta var (DB'ye gidilmez) | DB unique ihlali (catch)
 *  activate             : success | token yok
 *  requestPasswordReset : kayıtlı e-posta (olay) | kayıtsız e-posta (sessiz)
 *  resetPassword        : success (tüm oturumlar kapanır) | token yok
 *  deleteAccount        : hesap var | hesap yok (idempotent: yine de oturumlar kapanır)
 *
 * Mikroservis testinin kilit noktası: mail gönderilmiyor, profil oluşturulmuyor;
 * doğrulanan şey DOĞRU OLAYIN DOĞRU İÇERİKLE yayınlanması (ArgumentCaptor).
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ITokenStore tokenStore;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private AccountServiceImpl accountService;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	@Test
	void register_success_shouldSaveInactiveAccountAndPublishEvent() {
		// Arrange
		DtoRegisterIU request = easyRandom.nextObject(DtoRegisterIU.class);
		when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);
		when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed");
		when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(invocation -> {
			Account account = invocation.getArgument(0);
			account.setId(42L);
			return account;
		});

		// Act
		Long result = accountService.register(request);

		// Assert
		assertEquals(42L, result);

		ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
		verify(accountRepository, times(1)).saveAndFlush(accountCaptor.capture());
		Account saved = accountCaptor.getValue();
		assertEquals("hashed", saved.getPasswordHash());
		assertFalse(saved.isActive());
		assertNotNull(saved.getActivationToken());

		ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
		verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
		UserRegisteredEvent event = eventCaptor.getValue();
		assertEquals(42L, event.userId());
		assertEquals(request.getUsername(), event.username());
		assertEquals(request.getEmail(), event.email());
		assertEquals(saved.getActivationToken(), event.activationToken());
	}

	@Test
	void register_emailExists_shouldNotSaveOrPublish() {
		// Arrange
		DtoRegisterIU request = easyRandom.nextObject(DtoRegisterIU.class);
		when(accountRepository.existsByEmail(request.getEmail())).thenReturn(true);

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> accountService.register(request));

		// Assert
		assertEquals(AuthErrorType.EMAIL_NOT_UNIQUE, exception.getErrorType());
		assertEquals("email", exception.getField());
		verify(accountRepository, never()).saveAndFlush(any());
		verifyNoInteractions(passwordEncoder, eventPublisher);
	}

	@Test
	void register_uniqueConstraintViolation_shouldThrowFieldError() {
		// Arrange
		DtoRegisterIU request = easyRandom.nextObject(DtoRegisterIU.class);
		when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);
		when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed");
		when(accountRepository.saveAndFlush(any(Account.class)))
				.thenThrow(new DataIntegrityViolationException("uk_account_email"));

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> accountService.register(request));

		// Assert
		assertEquals(AuthErrorType.EMAIL_NOT_UNIQUE, exception.getErrorType());
		verifyNoInteractions(eventPublisher);
	}

	@Test
	void activate_success() {
		// Arrange
		Account account = easyRandom.nextObject(Account.class);
		account.setActive(false);
		account.setActivationToken("token");
		when(accountRepository.findByActivationToken("token")).thenReturn(Optional.of(account));

		// Act
		accountService.activate("token");

		// Assert
		assertTrue(account.isActive());
		assertNull(account.getActivationToken());
	}

	@Test
	void activate_invalidToken() {
		// Arrange
		when(accountRepository.findByActivationToken("bad")).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> accountService.activate("bad"));

		// Assert
		assertEquals(AuthErrorType.INVALID_ACTIVATION_TOKEN, exception.getErrorType());
	}

	@Test
	void requestPasswordReset_knownEmail_shouldPublishEvent() {
		// Arrange
		Account account = easyRandom.nextObject(Account.class);
		DtoPasswordResetIU request = new DtoPasswordResetIU(account.getEmail());
		when(accountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));

		// Act
		accountService.requestPasswordReset(request);

		// Assert
		ArgumentCaptor<PasswordResetRequestedEvent> captor = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
		verify(eventPublisher, times(1)).publishEvent(captor.capture());
		assertEquals(account.getEmail(), captor.getValue().email());
		assertEquals(account.getPasswordResetToken(), captor.getValue().passwordResetToken());
		assertNotNull(account.getPasswordResetToken());
	}

	@Test
	void requestPasswordReset_unknownEmail_shouldNotPublish() {
		// Arrange
		when(accountRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

		// Act
		accountService.requestPasswordReset(new DtoPasswordResetIU("nobody@mail.com"));

		// Assert
		verifyNoInteractions(eventPublisher);
	}

	@Test
	void resetPassword_success_shouldRevokeAllSessions() {
		// Arrange
		Account account = easyRandom.nextObject(Account.class);
		account.setId(7L);
		account.setActive(false);
		when(accountRepository.findByPasswordResetToken("reset")).thenReturn(Optional.of(account));
		when(passwordEncoder.encode("N3wPassword")).thenReturn("new-hash");

		// Act
		accountService.resetPassword("reset", new DtoPasswordUpdateIU("N3wPassword"));

		// Assert
		assertEquals("new-hash", account.getPasswordHash());
		assertNull(account.getPasswordResetToken());
		assertTrue(account.isActive());
		verify(tokenStore, times(1)).revokeAll(7L);
	}

	@Test
	void resetPassword_invalidToken_shouldNotRevokeSessions() {
		// Arrange
		when(accountRepository.findByPasswordResetToken("bad")).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class,
				() -> accountService.resetPassword("bad", new DtoPasswordUpdateIU("N3wPassword")));

		// Assert
		assertEquals(AuthErrorType.INVALID_PASSWORD_RESET_TOKEN, exception.getErrorType());
		verifyNoInteractions(tokenStore, passwordEncoder);
	}

	@Test
	void deleteAccount_existing_shouldDeleteAndRevoke() {
		// Arrange
		when(accountRepository.existsById(5L)).thenReturn(true);

		// Act
		accountService.deleteAccount(5L);

		// Assert
		verify(accountRepository, times(1)).deleteById(5L);
		verify(tokenStore, times(1)).revokeAll(5L);
	}

	@Test
	void deleteAccount_missing_shouldBeIdempotent() {
		// Arrange: aynı UserDeletedEvent ikinci kez geldi
		when(accountRepository.existsById(5L)).thenReturn(false);

		// Act
		accountService.deleteAccount(5L);

		// Assert
		verify(accountRepository, never()).deleteById(any());
		verify(tokenStore, times(1)).revokeAll(5L);
	}
}
