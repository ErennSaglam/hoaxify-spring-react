package com.hoaxify.notification.mail;

/** Mailin nasıl gönderileceği (log / SMTP) servis mantığından ayrı: strategy pattern */
public interface IMailSender {

	void send(String to, String subject, String htmlBody);
}
