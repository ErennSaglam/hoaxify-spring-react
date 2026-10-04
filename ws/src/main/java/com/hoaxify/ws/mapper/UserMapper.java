package com.hoaxify.ws.mapper;

import org.springframework.stereotype.Component;

import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.dto.DtoUserIU;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.entities.UserProfile;

/**
 * Entity <-> DTO dönüşümleri tek yerde. Servisler bu sınıfı kullanır, böylece
 * dönüşüm kodu servis metotlarının içine dağılmaz.
 *
 * Not: Lazy ilişkilere (profile) erişildiği için servisin transaction'ı içinde çağrılmalı.
 */
@Component
public class UserMapper {

	public DtoUser toDto(User user) {
		DtoUser dto = new DtoUser();
		dto.setId(user.getId());
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		UserProfile profile = user.getProfile();
		if (profile != null) {
			dto.setImage(profile.getImage());
			dto.setBio(profile.getBio());
		}
		return dto;
	}

	/** Şifre burada set edilmez; hash'leme servisin sorumluluğu. */
	public User toEntity(DtoUserIU dto) {
		User user = new User();
		user.setUsername(dto.getUsername());
		user.setEmail(dto.getEmail());
		user.setProfileBidirectional(new UserProfile());
		return user;
	}
}
