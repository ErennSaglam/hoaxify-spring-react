package com.hoaxify.ws.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import com.hoaxify.ws.configuration.HoaxifyProperties;
import com.hoaxify.ws.utils.MessageResolver;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * Tespit edilen senaryolar:
 *  sendActivationEmail    : success (alıcı, gönderen, başlık, link) | SMTP gönderim hatası
 *  sendPasswordResetEmail : success
 *  send (private)         : MessagingException catch bloğu -> MailPreparationException
 *                           (geçersiz "from" adresiyle tetiklenir)
 */
@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceImplTest {

	private static final String ACTIVATION_TITLE_KEY = "hoaxify.mail.user.created.title";
	private static final String RESET_TITLE_KEY = "hoaxify.mail.password.reset.title";
	private static final String CLICK_HERE_KEY = "hoaxify.mail.click.here";

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private MessageResolver messageResolver;

	/** @Spy: gerçek nesne, @InjectMocks onu da constructor'a verir. Veri taşıyıcı sınıfları mock'lamayız. */
	@Spy
	private HoaxifyProperties hoaxifyProperties = new HoaxifyProperties();

	@InjectMocks
	private SmtpEmailServiceImpl emailService;

	@BeforeEach
	void setUp() {
		hoaxifyProperties.getClient().setHost("http://localhost:5173");
		hoaxifyProperties.getEmail().setFrom("noreply@my-app.com");
	}

	private MimeMessage newMimeMessage() {
		return new MimeMessage(Session.getInstance(new Properties()));
	}

	private MimeMessage capturedMessage() {
		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender, times(1)).send(captor.capture());
		return captor.getValue();
	}

	@Test
	void sendActivationEmail_success() throws Exception {
		// Arrange
		when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
		when(messageResolver.get(ACTIVATION_TITLE_KEY)).thenReturn("Activate your Account");
		when(messageResolver.get(CLICK_HERE_KEY)).thenReturn("Click here");

		// Act
		emailService.sendActivationEmail("user1@mail.com", "token-123");

		// Assert
		MimeMessage message = capturedMessage();
		assertEquals("user1@mail.com", message.getAllRecipients()[0].toString());
		assertEquals("noreply@my-app.com", message.getFrom()[0].toString());
		assertEquals("Activate your Account", message.getSubject());
		String body = message.getContent().toString();
		assertTrue(body.contains("http://localhost:5173/activation/token-123"));
		assertTrue(body.contains("Click here"));
	}

	@Test
	void sendPasswordResetEmail_success() throws Exception {
		// Arrange
		when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
		when(messageResolver.get(RESET_TITLE_KEY)).thenReturn("Reset your password");
		when(messageResolver.get(CLICK_HERE_KEY)).thenReturn("Click here");

		// Act
		emailService.sendPasswordResetEmail("user1@mail.com", "reset-1");

		// Assert
		MimeMessage message = capturedMessage();
		assertEquals("Reset your password", message.getSubject());
		assertTrue(message.getContent().toString().contains("http://localhost:5173/password-reset/set?tk=reset-1"));
	}

	@Test
	void sendActivationEmail_smtpException_shouldPropagate() {
		// Arrange
		when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
		when(messageResolver.get(ACTIVATION_TITLE_KEY)).thenReturn("Activate your Account");
		when(messageResolver.get(CLICK_HERE_KEY)).thenReturn("Click here");
		doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

		// Act & Assert: UserServiceImpl bu MailException'ı yakalayıp ACTIVATION_EMAIL_FAILURE'a çevirir
		assertThrows(MailSendException.class, () -> emailService.sendActivationEmail("user1@mail.com", "token"));
	}

	@Test
	void sendActivationEmail_invalidFromAddress_shouldThrowMailPreparationException() {
		// Arrange: geçersiz adres MimeMessageHelper.setFrom içinde MessagingException fırlatır -> catch bloğu
		hoaxifyProperties.getEmail().setFrom("invalid address <<");
		when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
		when(messageResolver.get(ACTIVATION_TITLE_KEY)).thenReturn("Activate your Account");
		when(messageResolver.get(CLICK_HERE_KEY)).thenReturn("Click here");

		// Act & Assert
		assertThrows(MailPreparationException.class,
				() -> emailService.sendActivationEmail("user1@mail.com", "token"));
		verify(mailSender, never()).send(any(MimeMessage.class));
	}
}
