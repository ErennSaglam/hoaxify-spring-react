package com.hoaxify.auth.store.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.hoaxify.auth.configuration.AuthProperties;

/**
 * Tespit edilen senaryolar:
 *  create     : token anahtarı TTL ile yazılır + kullanıcının token kümesine eklenir
 *  findUserId : var | yok
 *  revoke     : token varsa kümeden de çıkarılır | token yoksa küme değişmez
 *  revokeAll  : kümedeki tüm token'lar silinir | küme boş
 */
@ExtendWith(MockitoExtension.class)
class RedisTokenStoreTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private SetOperations<String, String> setOperations;

	@Spy
	private AuthProperties authProperties = new AuthProperties();

	@InjectMocks
	private RedisTokenStore tokenStore;

	@BeforeEach
	void setUp() {
		authProperties.getToken().setValidity(Duration.ofHours(24));
	}

	@Test
	void create_success() {
		// Arrange
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);

		// Act
		String token = tokenStore.create(5L);

		// Assert
		verify(valueOperations, times(1)).set("auth:token:" + token, "5", Duration.ofHours(24));
		verify(setOperations, times(1)).add("auth:user-tokens:5", token);
		verify(redisTemplate, times(1)).expire("auth:user-tokens:5", Duration.ofHours(24));
	}

	@Test
	void findUserId_found() {
		// Arrange
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("auth:token:abc")).thenReturn("5");

		// Act
		Optional<Long> result = tokenStore.findUserId("abc");

		// Assert
		assertTrue(result.isPresent());
		assertEquals(5L, result.get());
	}

	@Test
	void findUserId_notFound() {
		// Arrange
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("auth:token:abc")).thenReturn(null);

		// Act & Assert
		assertFalse(tokenStore.findUserId("abc").isPresent());
	}

	@Test
	void revoke_existingToken_shouldRemoveFromUserSet() {
		// Arrange
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		when(valueOperations.getAndDelete("auth:token:abc")).thenReturn("5");

		// Act
		tokenStore.revoke("abc");

		// Assert
		verify(setOperations, times(1)).remove("auth:user-tokens:5", "abc");
	}

	@Test
	void revoke_unknownToken_shouldNotTouchUserSet() {
		// Arrange
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.getAndDelete("auth:token:abc")).thenReturn(null);

		// Act
		tokenStore.revoke("abc");

		// Assert
		verify(redisTemplate, never()).opsForSet();
	}

	@Test
	@SuppressWarnings("unchecked")
	void revokeAll_shouldDeleteEveryTokenAndTheSet() {
		// Arrange
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		when(setOperations.members("auth:user-tokens:5")).thenReturn(Set.of("t1", "t2"));

		// Act
		tokenStore.revokeAll(5L);

		// Assert
		ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
		verify(redisTemplate, times(1)).delete(captor.capture());
		assertEquals(Set.of("auth:token:t1", "auth:token:t2"), Set.copyOf(captor.getValue()));
		verify(redisTemplate, times(1)).delete("auth:user-tokens:5");
	}

	@Test
	void revokeAll_noSessions_shouldOnlyDeleteTheSet() {
		// Arrange
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		when(setOperations.members("auth:user-tokens:5")).thenReturn(Set.of());

		// Act
		tokenStore.revokeAll(5L);

		// Assert
		verify(redisTemplate, never()).delete(anyCollection());
		verify(redisTemplate, times(1)).delete("auth:user-tokens:5");
		verify(valueOperations, never()).get(anyString());
		verify(setOperations, never()).remove(anyString(), any());
	}
}
