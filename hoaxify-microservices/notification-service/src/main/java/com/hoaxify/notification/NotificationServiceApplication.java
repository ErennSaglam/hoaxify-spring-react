package com.hoaxify.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * "Alt servis" (supporting service): kendi veritabanı yok, dışarıya açık API'si yok.
 * Sadece olay dinler ve mail gönderir. Diğer servisler onun varlığından habersizdir;
 * kapalıyken gelen olaylar kuyrukta bekler, açılınca gönderilir.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}
}
