package com.hoaxify.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Profil servisi: kullanıcı adı, resim, bio. Üç şekilde konuşur:
 *  - REST     : frontend (gateway üzerinden) ve auth-service (Feign, /internal/users)
 *  - gRPC     : hoax-service (yazar bilgisi, port 9095)
 *  - RabbitMQ : user.registered dinler, user.deleted yayınlar
 */
@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}
}
