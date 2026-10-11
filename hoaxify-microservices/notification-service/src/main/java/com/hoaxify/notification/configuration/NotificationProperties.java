package com.hoaxify.notification.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "hoaxify")
public class NotificationProperties {

	/** Maillerdeki linklerin gideceği frontend adresi */
	private String clientHost = "http://localhost:5173";

	private Mail mail = new Mail();

	@Data
	public static class Mail {
		private String from = "noreply@my-app.com";
		/** log | smtp */
		private String mode = "log";
	}
}
