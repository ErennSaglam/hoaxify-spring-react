package com.hoaxify.gateway.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "hoaxify.gateway")
public class GatewayProperties {

	private String authServiceUrl = "http://localhost:8081";

	private String userServiceUrl = "http://localhost:8082";

	private String hoaxServiceUrl = "http://localhost:8083";

	/** Token doğrulama çağrısının zaman sınırı */
	private Duration verifyTimeout = Duration.ofSeconds(2);
}
