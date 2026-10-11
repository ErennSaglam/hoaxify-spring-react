package com.hoaxify.notification.mail.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hoaxify.notification.mail.IMailSender;

import lombok.extern.slf4j.Slf4j;

/** hoaxify.mail.mode=log (varsayılan): mail yerine içeriği loglar; linke konsoldan tıklanır */
@Slf4j
@Component
@ConditionalOnProperty(name = "hoaxify.mail.mode", havingValue = "log", matchIfMissing = true)
public class LoggingMailSender implements IMailSender {

	@Override
	public void send(String to, String subject, String htmlBody) {
		log.info("[DEV MAIL] to={} subject=\"{}\"\n{}", to, subject, htmlBody);
	}
}
