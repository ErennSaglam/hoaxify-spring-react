package com.hoaxify.user.configuration;

import java.time.Duration;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoaxify.user.dto.DtoUser;

/**
 * "users" cache'i: Redis'te JSON olarak, 10 dakika TTL ile.
 * Java serileştirmesi yerine JSON: Redis arayüzünde okunabilir ve sınıf değişince bozulmaz.
 * Profil her gösterildiğinde (hoax listesi, giriş cevabı) veritabanına gitmek yerine Redis'ten okunur.
 */
@Configuration
public class CacheConfig {

	public static final String USERS_CACHE = "users";

	@Bean
	public RedisCacheManagerBuilderCustomizer usersCacheCustomizer(ObjectMapper objectMapper) {
		RedisCacheConfiguration usersCache = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(10))
				.disableCachingNullValues()
				.serializeValuesWith(SerializationPair.fromSerializer(
						new Jackson2JsonRedisSerializer<>(objectMapper, DtoUser.class)));
		return builder -> builder.withCacheConfiguration(USERS_CACHE, usersCache);
	}
}
