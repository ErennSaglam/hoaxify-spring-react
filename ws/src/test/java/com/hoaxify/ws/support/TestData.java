package com.hoaxify.ws.support;

import java.time.LocalDateTime;
import java.util.List;

import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.entities.Hoax;
import com.hoaxify.ws.entities.Tag;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.entities.UserProfile;

/**
 * Testlerde tekrar tekrar ihtiyaç duyulan nesneler (Object Mother pattern).
 * Her test "hazırlık" kısmında bunları kullanır, böylece test gövdesi sadece
 * test ettiği davranışa odaklanır.
 */
public final class TestData {

	/** 1x1 piksel geçerli PNG, data URL formatında (frontend'in gönderdiği biçim) */
	public static final String PNG_DATA_URL = "data:image/png;base64,"
			+ "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

	/** Base64 "hello" -> düz metin, resim değil */
	public static final String TEXT_DATA_URL = "data:text/plain;base64,aGVsbG8=";

	public static final String VALID_PASSWORD = "P4ssword";

	private TestData() {
	}

	/** id'si olmayan (henüz kaydedilmemiş) aktif kullanıcı + boş profil */
	public static User newUser(String username) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(username + "@mail.com");
		user.setPassword("encoded-password");
		user.setActive(true);
		UserProfile profile = new UserProfile();
		profile.setBio("bio of " + username);
		user.setProfileBidirectional(profile);
		return user;
	}

	/** Veritabanından gelmiş gibi id'si set edilmiş kullanıcı */
	public static User user(Long id, String username) {
		User user = newUser(username);
		user.setId(id);
		return user;
	}

	public static Tag tag(Long id, String name) {
		Tag tag = new Tag(name);
		tag.setId(id);
		return tag;
	}

	public static Hoax hoax(Long id, User author, String content, Tag... tags) {
		Hoax hoax = new Hoax();
		hoax.setId(id);
		hoax.setUser(author);
		hoax.setContent(content);
		hoax.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
		hoax.getTags().addAll(List.of(tags));
		return hoax;
	}

	public static DtoUser dtoUser(Long id, String username) {
		return new DtoUser(id, username, username + "@mail.com", null, null);
	}
}
