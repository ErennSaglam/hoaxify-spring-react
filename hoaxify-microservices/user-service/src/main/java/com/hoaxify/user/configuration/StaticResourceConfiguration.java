package com.hoaxify.user.configuration;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/** Profil resimleri: GET /assets/profile/{dosya} (gateway /assets/** isteklerini buraya yönlendirir) */
@Configuration
@RequiredArgsConstructor
public class StaticResourceConfiguration implements WebMvcConfigurer {

	private final UserProperties userProperties;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String path = Paths.get(userProperties.getStorage().getRoot()).toAbsolutePath() + "/";
		registry.addResourceHandler("/assets/**")
				.addResourceLocations("file:" + path)
				.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
	}
}
