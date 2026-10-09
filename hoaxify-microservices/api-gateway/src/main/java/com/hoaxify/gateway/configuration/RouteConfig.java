package com.hoaxify.gateway.configuration;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Route'lar Java ile (YAML yerine): derleme zamanında kontrol edilir ve debug edilebilir.
 *
 * Frontend'in URL'leri monolith'le AYNI. Aynı /api/v1/users öneki birden fazla servise gidiyor;
 * ayrım HTTP metodu ve yolun devamıyla yapılıyor. Özelden genele sıralı: ilk eşleşen kazanır.
 */
@Configuration
public class RouteConfig {

	@Bean
	public RouteLocator hoaxifyRoutes(RouteLocatorBuilder routes, GatewayProperties properties) {
		String auth = properties.getAuthServiceUrl();
		String user = properties.getUserServiceUrl();
		String hoax = properties.getHoaxServiceUrl();

		return routes.routes()
				// --- auth-service: hesap ve oturum ---
				.route("auth-login-logout", r -> r.path("/api/v1/auth", "/api/v1/logout").uri(auth))
				.route("auth-register", r -> r.path("/api/v1/users").and().method(HttpMethod.POST).uri(auth))
				.route("auth-password-reset-request",
						r -> r.path("/api/v1/users/password-reset").and().method(HttpMethod.POST).uri(auth))
				.route("auth-activation-and-password",
						r -> r.path("/api/v1/users/*/active", "/api/v1/users/*/password")
								.and().method(HttpMethod.PATCH).uri(auth))

				// --- hoax-service: gönderi, etiket, istatistik (kullanıcıya bağlı olanlar dahil) ---
				.route("hoax-of-user", r -> r.path("/api/v1/users/*/hoaxes", "/api/v1/users/*/summary").uri(hoax))
				.route("hoax", r -> r.path("/api/v1/hoaxes", "/api/v1/hoaxes/**", "/api/v1/tags", "/api/v1/stats/**")
						.uri(hoax))

				// --- user-service: profil ve profil resimleri ---
				.route("user-profile", r -> r.path("/api/v1/users", "/api/v1/users/*").uri(user))
				.route("user-assets", r -> r.path("/assets/**").uri(user))
				.build();
	}
}
