package com.hoaxify.user.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;

import com.hoaxify.grpc.user.v1.UserSummary;
import com.hoaxify.user.dto.DtoUser;
import com.hoaxify.user.entities.UserProfile;

/**
 * Tespit edilen senaryolar:
 *  toDto   : tüm alanlar kopyalanır
 *  toProto : image/bio dolu -> set edilir | null -> set EDİLMEZ (protobuf null kabul etmez, hasImage() false)
 */
class UserMapperTest {

	private final UserMapper userMapper = new UserMapper();

	private final EasyRandom easyRandom = new EasyRandom();

	@Test
	void toDto_success() {
		// Arrange
		UserProfile profile = easyRandom.nextObject(UserProfile.class);

		// Act
		DtoUser result = userMapper.toDto(profile);

		// Assert
		assertEquals(profile.getId(), result.id());
		assertEquals(profile.getUsername(), result.username());
		assertEquals(profile.getImage(), result.image());
	}

	@Test
	void toProto_withOptionalFields() {
		// Act
		UserSummary result = userMapper.toProto(new DtoUser(1L, "user1", "u@mail.com", "img.png", "bio"));

		// Assert
		assertEquals(1L, result.getId());
		assertTrue(result.hasImage());
		assertEquals("img.png", result.getImage());
		assertTrue(result.hasBio());
	}

	@Test
	void toProto_nullOptionalFields_shouldNotBeSet() {
		// Act
		UserSummary result = userMapper.toProto(new DtoUser(1L, "user1", "u@mail.com", null, null));

		// Assert
		assertFalse(result.hasImage());
		assertFalse(result.hasBio());
		assertEquals("", result.getImage()); // proto3: set edilmemiş string "" döner, null değil
	}
}
