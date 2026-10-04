package com.hoaxify.ws.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * application.properties'teki "hoaxify.*" ayarlarını tip güvenli şekilde okur.
 * @Value("${...}") ile tek tek okumak yerine tek bir nesnede toplar.
 * Kayıt: WsApplication üzerindeki @EnableConfigurationProperties.
 */
@Data
@ConfigurationProperties(prefix = "hoaxify")
public class HoaxifyProperties {

	private Client client = new Client();

	private Email email = new Email();

	private Storage storage = new Storage();

	private Jwt jwt = new Jwt();

	/** basic | jwt | opaque -> hangi ITokenService implementasyonunun kullanılacağını belirler */
	private String tokenType = "opaque";

	@Data
	public static class Client {
		/** Maillerdeki linklerin gideceği frontend adresi */
		private String host;
	}

	@Data
	public static class Email {
		private String from;
	}

	@Data
	public static class Storage {
		private String root = "uploads";
		private String profile = "profile";
	}

	@Data
	public static class Jwt {
		private String secret;
		private Duration validity = Duration.ofHours(24);
	}
}
