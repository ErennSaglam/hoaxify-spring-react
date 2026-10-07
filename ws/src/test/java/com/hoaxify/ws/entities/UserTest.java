package com.hoaxify.ws.entities;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Entity'ler genelde test edilmez (sadece getter/setter), ama içinde davranış olan yardımcı
 * metotlar test edilir.
 *
 * Tespit edilen senaryolar:
 *  setProfileBidirectional : profil verilirse iki yön bağlanır | null verilirse profil kaldırılır
 */
class UserTest {

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	@Test
	void setProfileBidirectional_shouldLinkBothSides() {
		// Arrange
		User user = easyRandom.nextObject(User.class);
		UserProfile profile = new UserProfile();

		// Act
		user.setProfileBidirectional(profile);

		// Assert
		assertSame(profile, user.getProfile());
		assertSame(user, profile.getUser());
	}

	@Test
	void setProfileBidirectional_null_shouldRemoveProfile() {
		// Arrange
		User user = easyRandom.nextObject(User.class);

		// Act
		user.setProfileBidirectional(null);

		// Assert
		assertNull(user.getProfile());
	}
}
