package com.hoaxify.user.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "hoaxify")
public class UserProperties {

	private Storage storage = new Storage();

	@Data
	public static class Storage {
		private String root = "uploads";
		private String profile = "profile";
	}
}
