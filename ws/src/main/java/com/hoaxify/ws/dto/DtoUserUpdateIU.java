package com.hoaxify.ws.dto;

import com.hoaxify.ws.validation.FileType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Profil güncelleme request DTO'su. Kayıttan farklı alanlara sahip olduğu için ayrı sınıf.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoUserUpdateIU {

	@NotBlank(message = "{hoaxify.constraint.username.notblank}")
	@Size(min = 4, max = 255)
	private String username;

	/** Base64 data URL (data:image/png;base64,...). null ise resim değişmez. */
	@FileType(types = { "jpeg", "png" })
	private String image;

	/** null ise bio değişmez. */
	@Size(max = 255)
	private String bio;
}
