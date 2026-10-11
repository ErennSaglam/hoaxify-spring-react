package com.hoaxify.notification.services.impl;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.hoaxify.common.event.PasswordResetRequestedEvent;
import com.hoaxify.common.event.UserRegisteredEvent;
import com.hoaxify.notification.configuration.NotificationProperties;
import com.hoaxify.notification.mail.IMailSender;
import com.hoaxify.notification.services.INotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

	static final String TEMPLATE = """
			<html>
				<body>
					<h1>${title}</h1>
					<a href="${url}">${clickHere}</a>
				</body>
			</html>
			""";

	private final IMailSender mailSender;
	private final MessageSource messageSource;
	private final NotificationProperties properties;

	/** Aktivasyon token'ı yoksa (dev verisi gibi önceden aktif hesaplar) mail gönderilmez */
	@Override
	public void sendActivationMail(UserRegisteredEvent event) {
		if (event.activationToken() == null) {
			log.debug("User {} is pre-activated, no activation mail", event.userId());
			return;
		}
		String url = properties.getClientHost() + "/activation/" + event.activationToken();
		send(event.email(), "hoaxify.mail.user.created.title", url, event.locale());
	}

	@Override
	public void sendPasswordResetMail(PasswordResetRequestedEvent event) {
		String url = properties.getClientHost() + "/password-reset/set?tk=" + event.passwordResetToken();
		send(event.email(), "hoaxify.mail.password.reset.title", url, event.locale());
	}

	/** Olay, kullanıcının istek attığı dili taşır; mail o dilde hazırlanır */
	private void send(String to, String titleKey, String url, String languageTag) {
		Locale locale = languageTag == null ? Locale.ENGLISH : Locale.forLanguageTag(languageTag);
		String title = messageSource.getMessage(titleKey, null, locale);
		String body = TEMPLATE
				.replace("${title}", title)
				.replace("${url}", url)
				.replace("${clickHere}", messageSource.getMessage("hoaxify.mail.click.here", null, locale));
		mailSender.send(to, title, body);
	}
}
