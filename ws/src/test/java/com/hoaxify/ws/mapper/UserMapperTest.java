package com.hoaxify.ws.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.dto.DtoUserIU;
import com.hoaxify.ws.entities.User;

/**
 * UserMapper'ın bağımlılığı yok; mock gerekmez, doğrudan "new" ile test edilir.
 *
 * Tespit edilen senaryolar:
 *  toDto    : profil var (image/bio düzleştirilir) | profil null
 *  toEntity : alanlar kopyalanır, şifre kopyalanmaz, boş profil çift yönlü bağlanır
 */
class UserMapperTest {

	private final UserMapper userMapper = new UserMapper();

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	@Test
	void toDto_withProfile_shouldFlattenProfileFields() {
		// Arrange
		User user = easyRandom.nextObject(User.class);

		// Act
		DtoUser result = userMapper.toDto(user);

		// Assert
		assertEquals(user.getId(), result.getId());
		assertEquals(user.getUsername(), result.getUsername());
		assertEquals(user.getEmail(), result.getEmail());
		assertEquals(user.getProfile().getImage(), result.getImage());
		assertEquals(user.getProfile().getBio(), result.getBio());
	}

	@Test
	void toDto_profileNull_shouldLeaveImageAndBioNull() {
		// Arrange
		User user = easyRandom.nextObject(User.class);
		user.setProfile(null);

		// Act
		DtoUser result = userMapper.toDto(user);

		// Assert
		assertEquals(user.getId(), result.getId());
		assertNull(result.getImage());
		assertNull(result.getBio());
	}

	@Test
	void toEntity_success() {
		// Arrange
		DtoUserIU request = easyRandom.nextObject(DtoUserIU.class);

		// Act
		User result = userMapper.toEntity(request);

		// Assert
		assertEquals(request.getUsername(), result.getUsername());
		assertEquals(request.getEmail(), result.getEmail());
		assertNull(result.getPassword(), "şifre hash'lenmeden entity'ye kopyalanmamalı");
		assertNull(result.getId());
		assertNotNull(result.getProfile());
		assertSame(result, result.getProfile().getUser());
	}
}
