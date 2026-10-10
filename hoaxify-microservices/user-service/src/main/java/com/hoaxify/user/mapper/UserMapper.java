package com.hoaxify.user.mapper;

import org.springframework.stereotype.Component;

import com.hoaxify.grpc.user.v1.UserSummary;
import com.hoaxify.user.dto.DtoUser;
import com.hoaxify.user.entities.UserProfile;

@Component
public class UserMapper {

	public DtoUser toDto(UserProfile profile) {
		return new DtoUser(profile.getId(), profile.getUsername(), profile.getEmail(), profile.getImage(),
				profile.getBio());
	}

	/**
	 * DTO -> protobuf mesajı. Protobuf builder'ları null kabul etmez (setImage(null) NullPointerException fırlatır);
	 * bu yüzden opsiyonel alanlar sadece doluysa set edilir. Karşı taraf hasImage() ile kontrol eder.
	 */
	public UserSummary toProto(DtoUser user) {
		UserSummary.Builder builder = UserSummary.newBuilder()
				.setId(user.id())
				.setUsername(user.username())
				.setEmail(user.email());
		if (user.image() != null) {
			builder.setImage(user.image());
		}
		if (user.bio() != null) {
			builder.setBio(user.bio());
		}
		return builder.build();
	}
}
