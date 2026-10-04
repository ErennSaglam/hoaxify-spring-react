package com.hoaxify.ws.dto;

import com.hoaxify.ws.validation.UniqueEmail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kayıt (insert) request DTO'su. Validation anotasyonları entity'de değil burada.
 * String uzunluğu için @Size kullanılır; @Min/@Max sayısal alanlar içindir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoUserIU {

	@NotBlank(message = "{hoaxify.constraint.username.notblank}")
	@Size(min = 4, max = 255)
	private String username;

	@NotBlank
	@Email
	@UniqueEmail
	private String email;

	@NotBlank
	@Size(min = 8, max = 255)
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "{hoaxify.constraint.password.pattern}")
	private String password;
}
