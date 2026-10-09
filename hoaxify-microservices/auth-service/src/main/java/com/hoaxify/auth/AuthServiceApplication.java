package com.hoaxify.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Kimlik servisi: hesap (e-posta + şifre), aktivasyon, şifre sıfırlama ve oturum token'ları.
 * Kullanıcının profil bilgisi (kullanıcı adı, resim, bio) burada DEĞİL, user-service'te.
 */
@SpringBootApplication
@EnableFeignClients
@ConfigurationPropertiesScan
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
}
