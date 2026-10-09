package com.hoaxify.gateway.security;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.hoaxify.gateway.configuration.GatewayProperties;

import reactor.core.publisher.Mono;

/**
 * auth-service'in /internal/auth/verify ucunu WebClient (reaktif HTTP istemcisi) ile çağırır.
 * Gateway reaktif olduğu için burada Feign (bloklayan) değil WebClient kullanılır.
 */
@Component
public class AuthServiceTokenVerifier implements ITokenVerifier {

	private final WebClient webClient;
	private final GatewayProperties properties;

	public AuthServiceTokenVerifier(WebClient.Builder webClientBuilder, GatewayProperties properties) {
		this.webClient = webClientBuilder.baseUrl(properties.getAuthServiceUrl()).build();
		this.properties = properties;
	}

	@Override
	public Mono<Optional<Long>> verify(String token) {
		return webClient.post()
				.uri("/internal/auth/verify")
				.bodyValue(Map.of("token", token))
				.exchangeToMono(response -> {
					if (response.statusCode().is2xxSuccessful()) {
						return response.bodyToMono(VerifyResponse.class).map(body -> Optional.of(body.userId()));
					}
					if (response.statusCode() == HttpStatus.UNAUTHORIZED) {
						return response.releaseBody().thenReturn(Optional.<Long>empty());
					}
					return response.createError();
				})
				.timeout(properties.getVerifyTimeout());
	}

	record VerifyResponse(Long userId) {
	}
}
