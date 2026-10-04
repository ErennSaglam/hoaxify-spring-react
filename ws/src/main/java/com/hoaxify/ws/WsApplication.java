package com.hoaxify.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.hoaxify.ws.configuration.HoaxifyProperties;

/**
 * Main sınıf kök pakette (com.hoaxify.ws): Spring bu paketi ve tüm alt paketlerini
 * (controller, services, repository, entities ...) otomatik tarar. Bu yüzden
 * @ComponentScan / @EntityScan / @EnableJpaRepositories yazmaya gerek yok.
 *
 * Referans projedeki gibi "starter" alt paketinde olsaydı iki sorun çıkardı:
 *  1) @WebMvcTest / @DataJpaTest gibi test slice'ları bu sınıfı test paketinden yukarı
 *     doğru arar; com.hoaxify.ws.starter'ı bulamaz.
 *  2) Elle yazılmış @ComponentScan, slice testlerinin filtrelerini devre dışı bırakır
 *     (@WebMvcTest'te bile tüm servis ve repository'ler yüklenmeye çalışılır).
 *
 * UserDetailsServiceAutoConfiguration'ı kapatıyoruz: kimlik doğrulamayı kendi
 * TokenFilter'ımız yapıyor, Spring'in in-memory kullanıcı üretmesine gerek yok.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(HoaxifyProperties.class)
public class WsApplication {

	public static void main(String[] args) {
		SpringApplication.run(WsApplication.class, args);
	}

}
