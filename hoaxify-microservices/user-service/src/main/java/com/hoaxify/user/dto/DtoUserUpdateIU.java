package com.hoaxify.user.dto;

import com.hoaxify.user.validation.FileType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoUserUpdateIU {

	@NotBlank(message = "{hoaxify.constraint.username.notblank}")
	@Size(min = 4, max = 255)
	private String username;

	@FileType(types = { "jpeg", "png" })
	private String image;

	@Size(max = 255)
	private String bio;
}
