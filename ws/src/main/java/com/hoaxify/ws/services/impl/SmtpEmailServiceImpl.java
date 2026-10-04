package com.hoaxify.ws.services.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.hoaxify.ws.configuration.HoaxifyProperties;
import com.hoaxify.ws.services.IEmailService;
import com.hoaxify.ws.utils.MessageResolver;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

/**
 * Gerçek SMTP ile mail gönderir. JavaMailSender, application.properties'teki
 * spring.mail.* ayarlarından Spring Boot tarafından otomatik oluşturulur.
 */
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class SmtpEmailServiceImpl implements IEmailService {

	private static final String TEMPLATE = """
			<html>
				<body>
					<h1>${title}</h1>
					<a href="${url}">${clickHere}</a>
				</body>
			</html>
			""";

	private final JavaMailSender mailSender;
	private final HoaxifyProperties hoaxifyProperties;
	private final MessageResolver messageResolver;

	@Override
	public void sendActivationEmail(String email, String activationToken) {
		String url = hoaxifyProperties.getClient().getHost() + "/activation/" + activationToken;
		send(email, messageResolver.get("hoaxify.mail.user.created.title"), url);
	}

	@Override
	public void sendPasswordResetEmail(String email, String passwordResetToken) {
		String url = hoaxifyProperties.getClient().getHost() + "/password-reset/set?tk=" + passwordResetToken;
		send(email, messageResolver.get("hoaxify.mail.password.reset.title"), url);
	}

	private void send(String to, String title, String url) {
		String body = TEMPLATE
				.replace("${title}", title)
				.replace("${url}", url)
				.replace("${clickHere}", messageResolver.get("hoaxify.mail.click.here"));

		MimeMessage mimeMessage = mailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
			helper.setFrom(hoaxifyProperties.getEmail().getFrom());
			helper.setTo(to);
			helper.setSubject(title);
			helper.setText(body, true);
		} catch (MessagingException ex) {
			// Spring'in MailException hiyerarşisine çeviriyoruz; çağıran taraf tek tip exception yakalar
			throw new MailPreparationException(ex);
		}
		mailSender.send(mimeMessage);
	}
}
