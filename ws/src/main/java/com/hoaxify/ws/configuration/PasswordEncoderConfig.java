package com.hoaxify.ws.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @Configuration + @Bean: kendi yazmadığımız bir sınıfı (BCryptPasswordEncoder)
 * Spring container'a bean olarak veriyoruz. Artık her yerde constructor'dan
 * PasswordEncoder isteyebiliriz.
 */
@Configuration
public class PasswordEncoderConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
