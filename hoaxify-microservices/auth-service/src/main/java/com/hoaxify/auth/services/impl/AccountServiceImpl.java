package com.hoaxify.auth.services.impl;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.auth.dto.DtoPasswordResetIU;
import com.hoaxify.auth.dto.DtoPasswordUpdateIU;
import com.hoaxify.auth.dto.DtoRegisterIU;
import com.hoaxify.auth.entities.Account;
import com.hoaxify.auth.exception.AuthErrorType;
import com.hoaxify.auth.repository.AccountRepository;
import com.hoaxify.auth.services.IAccountService;
import com.hoaxify.auth.store.ITokenStore;
import com.hoaxify.common.event.PasswordResetRequestedEvent;
import com.hoaxify.common.event.UserRegisteredEvent;
import com.hoaxify.common.web.exception.BaseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Monolith'teki UserServiceImpl'in hesapla ilgili kısmı. En önemli fark: mail göndermiyor ve profil
 * oluşturmuyor; sadece "kayıt oldu" olayını yayınlıyor. Mail notification-service'in, profil
 * user-service'in işi. Bu servis onların var olduğunu bile bilmiyor (gevşek bağlılık / loose coupling).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements IAccountService {

	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;
	private final ITokenStore tokenStore;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public Long register(DtoRegisterIU request) {
		if (accountRepository.existsByEmail(request.getEmail())) {
			throw BaseException.forField(AuthErrorType.EMAIL_NOT_UNIQUE, "email");
		}

		Account account = new Account();
		account.setEmail(request.getEmail());
		account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		account.setActive(false);
		account.setActivationToken(UUID.randomUUID().toString());

		try {
			// Aynı anda gelen iki istek existsByEmail kontrolünü birlikte geçebilir; son savunma DB unique constraint'i
			accountRepository.saveAndFlush(account);
		} catch (DataIntegrityViolationException ex) {
			throw BaseException.forField(AuthErrorType.EMAIL_NOT_UNIQUE, "email");
		}

		// Commit'ten sonra DomainEventRelay bunu RabbitMQ'ya gönderir
		eventPublisher.publishEvent(new UserRegisteredEvent(account.getId(), request.getUsername(),
				account.getEmail(), account.getActivationToken(), currentLocale()));
		return account.getId();
	}

	@Override
	@Transactional
	public void activate(String activationToken) {
		Account account = accountRepository.findByActivationToken(activationToken)
				.orElseThrow(() -> new BaseException(AuthErrorType.INVALID_ACTIVATION_TOKEN));
		account.setActive(true);
		account.setActivationToken(null);
	}

	/** E-posta kayıtlı değilse de hata dönmüyoruz (user enumeration koruması) */
	@Override
	@Transactional
	public void requestPasswordReset(DtoPasswordResetIU request) {
		accountRepository.findByEmail(request.getEmail()).ifPresentOrElse(account -> {
			account.setPasswordResetToken(UUID.randomUUID().toString());
			eventPublisher.publishEvent(new PasswordResetRequestedEvent(account.getEmail(),
					account.getPasswordResetToken(), currentLocale()));
		}, () -> log.debug("Password reset requested for unknown e-mail"));
	}

	@Override
	@Transactional
	public void resetPassword(String passwordResetToken, DtoPasswordUpdateIU request) {
		Account account = accountRepository.findByPasswordResetToken(passwordResetToken)
				.orElseThrow(() -> new BaseException(AuthErrorType.INVALID_PASSWORD_RESET_TOKEN));
		account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		account.setPasswordResetToken(null);
		account.setActive(true);
		// Şifre değiştiyse eski oturumlar kapanmalı (şifre çalınmış olabilir)
		tokenStore.revokeAll(account.getId());
	}

	@Override
	@Transactional
	public void deleteAccount(Long userId) {
		if (accountRepository.existsById(userId)) {
			accountRepository.deleteById(userId);
		}
		tokenStore.revokeAll(userId);
	}

	private String currentLocale() {
		return LocaleContextHolder.getLocale().toLanguageTag();
	}
}
