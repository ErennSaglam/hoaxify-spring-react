package com.hoaxify.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Frontend'in konuştuğu TEK adres (http://localhost:8000). Görevleri:
 *  1) Yönlendirme (routing): /api/v1/hoaxes -> hoax-service, /api/v1/auth -> auth-service ...
 *  2) Kimlik doğrulama: token'ı auth-service'e doğrulatır, arkadaki servislere X-User-Id iletir
 *  3) /internal/** uçlarını dışarıya KAPATIR (route tanımı olmadığı için 404)
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}
