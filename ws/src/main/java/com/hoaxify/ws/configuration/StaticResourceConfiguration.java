package com.hoaxify.ws.configuration;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Diskteki yüklenen dosyaları /assets/** altından servis eder.
 * Ör: uploads-dev/profile/abc -> GET /assets/profile/abc
 */
@Configuration
@RequiredArgsConstructor
public class StaticResourceConfiguration implements WebMvcConfigurer {

	private final HoaxifyProperties hoaxifyProperties;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String path = Paths.get(hoaxifyProperties.getStorage().getRoot()).toAbsolutePath() + "/";
		registry.addResourceHandler("/assets/**")
				.addResourceLocations("file:" + path)
				.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
	}
}
