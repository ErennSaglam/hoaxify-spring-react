package com.hoaxify.gateway.error;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaxify.common.error.ApiError;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/** Gateway'in kendi ürettiği hataları (401, 503) servislerle AYNI ApiError formatında yazar */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String messageKey) {
		Locale locale = exchange.getLocaleContext().getLocale();
		String message = messageSource.getMessage(messageKey, null, messageKey, locale == null ? Locale.ENGLISH : locale);
		ApiError<Void> error = new ApiError<>(UUID.randomUUID().toString(), status.value(), message,
				exchange.getRequest().getPath().value(), Instant.now(), null);

		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(status);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		try {
			DataBuffer buffer = response.bufferFactory().wrap(objectMapper.writeValueAsBytes(error));
			return response.writeWith(Mono.just(buffer));
		} catch (JsonProcessingException ex) {
			return Mono.error(ex);
		}
	}
}
