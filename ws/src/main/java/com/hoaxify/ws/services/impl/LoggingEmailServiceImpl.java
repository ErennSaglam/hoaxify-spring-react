package com.hoaxify.ws.services.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.hoaxify.ws.configuration.HoaxifyProperties;
import com.hoaxify.ws.services.IEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * dev profilinde gerçek mail göndermek yerine linki loga yazar.
 * Kayıt olduktan sonra konsoldaki linke tıklayarak hesabı aktifleştirebilirsin.
 *
 * Aynı arayüzün profile göre değişen iki implementasyonu var:
 * dev -> LoggingEmailServiceImpl, diğer profiller -> SmtpEmailServiceImpl.
 */
@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class LoggingEmailServiceImpl implements IEmailService {

	private final HoaxifyProperties hoaxifyProperties;

	@Override
	public void sendActivationEmail(String email, String activationToken) {
		log.info("[DEV MAIL] Activation link for {}: {}/activation/{}", email,
				hoaxifyProperties.getClient().getHost(), activationToken);
	}

	@Override
	public void sendPasswordResetEmail(String email, String passwordResetToken) {
		log.info("[DEV MAIL] Password reset link for {}: {}/password-reset/set?tk={}", email,
				hoaxifyProperties.getClient().getHost(), passwordResetToken);
	}
}
