package com.hoaxify.auth.store.impl;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.hoaxify.auth.configuration.AuthProperties;
import com.hoaxify.auth.store.ITokenStore;

import lombok.RequiredArgsConstructor;

/**
 * Redis'teki anahtarlar:
 *   auth:token:{token}        -> userId              (TTL = token süresi)
 *   auth:user-tokens:{userId} -> {token1, token2 ...} (kullanıcının tüm oturumları, toplu kapatmak için)
 */
@Component
@RequiredArgsConstructor
public class RedisTokenStore implements ITokenStore {

	static final String TOKEN_KEY = "auth:token:";
	static final String USER_TOKENS_KEY = "auth:user-tokens:";

	private final StringRedisTemplate redisTemplate;
	private final AuthProperties authProperties;

	@Override
	public String create(Long userId) {
		String token = UUID.randomUUID().toString();
		Duration validity = authProperties.getToken().getValidity();
		redisTemplate.opsForValue().set(TOKEN_KEY + token, String.valueOf(userId), validity);
		redisTemplate.opsForSet().add(USER_TOKENS_KEY + userId, token);
		redisTemplate.expire(USER_TOKENS_KEY + userId, validity);
		return token;
	}

	@Override
	public Optional<Long> findUserId(String token) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(TOKEN_KEY + token)).map(Long::valueOf);
	}

	@Override
	public void revoke(String token) {
		String userId = redisTemplate.opsForValue().getAndDelete(TOKEN_KEY + token);
		if (userId != null) {
			redisTemplate.opsForSet().remove(USER_TOKENS_KEY + userId, token);
		}
	}

	@Override
	public void revokeAll(Long userId) {
		Set<String> tokens = redisTemplate.opsForSet().members(USER_TOKENS_KEY + userId);
		if (tokens != null && !tokens.isEmpty()) {
			redisTemplate.delete(tokens.stream().map(token -> TOKEN_KEY + token).toList());
		}
		redisTemplate.delete(USER_TOKENS_KEY + userId);
	}
}
