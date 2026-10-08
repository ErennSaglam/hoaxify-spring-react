package com.hoaxify.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Merkezi konfigürasyon sunucusu. Servisler açılırken ayarlarını buradan çeker:
 * GET http://localhost:8071/auth-service/default -> application.yml + auth-service.yml birleşimi
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}
}
