package com.hoaxify.notification.mail.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.hoaxify.notification.configuration.NotificationProperties;
import com.hoaxify.notification.mail.IMailSender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

/** hoaxify.mail.mode=smtp: spring.mail.* ayarlarıyla gerçek gönderim */
@Component
@ConditionalOnProperty(name = "hoaxify.mail.mode", havingValue = "smtp")
@RequiredArgsConstructor
public class SmtpMailSender implements IMailSender {

	private final JavaMailSender javaMailSender;
	private final NotificationProperties properties;

	@Override
	public void send(String to, String subject, String htmlBody) {
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
			helper.setFrom(properties.getMail().getFrom());
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlBody, true);
		} catch (MessagingException ex) {
			throw new MailPreparationException(ex);
		}
		// Hata fırlarsa RabbitMQ listener'ı mesajı yeniden dener, 3 denemeden sonra DLQ'ya düşer
		javaMailSender.send(mimeMessage);
	}
}
