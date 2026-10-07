package com.hoaxify.ws.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton container pattern: tüm test sınıfları için Docker'da TEK bir PostgreSQL
 * container'ı başlatılır (her test sınıfı için ayrı container açmak yavaş olurdu).
 * JVM kapanınca Testcontainers (Ryuk) container'ı otomatik siler.
 *
 * @ServiceConnection: Spring Boot, spring.datasource.url/username/password değerlerini
 * bu container'dan alır. application.properties'teki değerlerle uğraşmaya gerek kalmaz.
 *
 * Kullanım: test sınıfı bu arayüzü implement eder -> "implements PostgresTestContainer"
 */
public interface PostgresTestContainer {

	@ServiceConnection
	PostgreSQLContainer<?> POSTGRES = createAndStart();

	private static PostgreSQLContainer<?> createAndStart() {
		PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine");
		container.start();
		return container;
	}
}
