package com.hoaxify.auth.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "hoaxify")
public class AuthProperties {

	private Token token = new Token();

	private Seed seed = new Seed();

	@Data
	public static class Token {
		private Duration validity = Duration.ofHours(24);
	}

	@Data
	public static class Seed {
		/** Geliştirme ortamında örnek hesaplar oluşturulsun mu */
		private boolean enabled = false;
	}
}
