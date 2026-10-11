package com.hoaxify.hoax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gönderi servisi: hoax, etiket, istatistik (DAO).
 * Yazar bilgisini (kullanıcı adı, resim) SAKLAMAZ; sadece userId tutar ve gerektiğinde
 * user-service'e gRPC ile sorar.
 */
@SpringBootApplication
public class HoaxServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(HoaxServiceApplication.class, args);
	}
}
