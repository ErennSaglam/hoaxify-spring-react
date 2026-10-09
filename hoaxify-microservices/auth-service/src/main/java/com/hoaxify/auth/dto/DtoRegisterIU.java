package com.hoaxify.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kayıt isteği (monolith'teki DtoUserIU ile aynı JSON, frontend değişmesin diye).
 * username auth-service'te saklanmaz; UserRegisteredEvent ile user-service'e iletilir.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoRegisterIU {

	@NotBlank(message = "{hoaxify.constraint.username.notblank}")
	@Size(min = 4, max = 255)
	private String username;

	@NotBlank
	@Email
	private String email;

	@NotBlank
	@Size(min = 8, max = 255)
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "{hoaxify.constraint.password.pattern}")
	private String password;
}
