package com.hoaxify.notification.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mail.MailSendException;

import com.hoaxify.common.event.PasswordResetRequestedEvent;
import com.hoaxify.common.event.UserRegisteredEvent;
import com.hoaxify.notification.configuration.NotificationProperties;
import com.hoaxify.notification.mail.IMailSender;

/**
 * Tespit edilen senaryolar:
 *  sendActivationMail    : token var -> olayın dilinde mail | token yok (önceden aktif) -> mail yok
 *                          | dil bilgisi yok -> İngilizce | mail hatası -> yükselir (RabbitMQ retry + DLQ)
 *  sendPasswordResetMail : link doğru
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

	@Mock
	private IMailSender mailSender;

	@Spy
	private StaticMessageSource messageSource = new StaticMessageSource();

	@Spy
	private NotificationProperties properties = new NotificationProperties();

	@InjectMocks
	private NotificationServiceImpl notificationService;

	@BeforeEach
	void setUp() {
		properties.setClientHost("http://localhost:5173");
		messageSource.addMessage("hoaxify.mail.user.created.title", Locale.ENGLISH, "Activate your Account");
		messageSource.addMessage("hoaxify.mail.user.created.title", Locale.forLanguageTag("tr"), "Hesabinizi aktiflestirin");
		messageSource.addMessage("hoaxify.mail.password.reset.title", Locale.ENGLISH, "Reset your password");
		messageSource.addMessage("hoaxify.mail.click.here", Locale.ENGLISH, "Click here");
		messageSource.addMessage("hoaxify.mail.click.here", Locale.forLanguageTag("tr"), "Buraya tiklayin");
	}

	@Test
	void sendActivationMail_success_inEventLanguage() {
		// Arrange
		UserRegisteredEvent event = new UserRegisteredEvent(1L, "alice", "alice@mail.com", "tok-1", "tr");

		// Act
		notificationService.sendActivationMail(event);

		// Assert
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(mailSender, times(1)).send(eq("alice@mail.com"), eq("Hesabinizi aktiflestirin"), body.capture());
		assertTrue(body.getValue().contains("http://localhost:5173/activation/tok-1"));
		assertTrue(body.getValue().contains("Buraya tiklayin"));
	}

	@Test
	void sendActivationMail_preActivatedUser_shouldNotSend() {
		// Act
		notificationService.sendActivationMail(new UserRegisteredEvent(1L, "user1", "u1@mail.com", null, "en"));

		// Assert
		verifyNoInteractions(mailSender);
	}

	@Test
	void sendActivationMail_noLocale_shouldFallBackToEnglish() {
		// Act
		notificationService.sendActivationMail(new UserRegisteredEvent(1L, "a", "a@mail.com", "tok", null));

		// Assert
		verify(mailSender, times(1)).send(eq("a@mail.com"), eq("Activate your Account"), anyString());
	}

	@Test
	void sendActivationMail_mailFailure_shouldPropagateForRetry() {
		// Arrange
		doThrow(new MailSendException("SMTP down")).when(mailSender).send(anyString(), anyString(), any());

		// Act & Assert: yakalamıyoruz; RabbitMQ listener'ı 3 kez dener, sonra mesaj DLQ'ya düşer
		assertThrows(MailSendException.class, () -> notificationService
				.sendActivationMail(new UserRegisteredEvent(1L, "a", "a@mail.com", "tok", "en")));
	}

	@Test
	void sendPasswordResetMail_success() {
		// Act
		notificationService.sendPasswordResetMail(new PasswordResetRequestedEvent("a@mail.com", "reset-1", "en"));

		// Assert
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(mailSender, times(1)).send(eq("a@mail.com"), eq("Reset your password"), body.capture());
		assertTrue(body.getValue().contains("http://localhost:5173/password-reset/set?tk=reset-1"));
	}
}
