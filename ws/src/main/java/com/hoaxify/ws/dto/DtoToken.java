package com.hoaxify.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Authorization başlığında kullanılacak hali: "{prefix} {token}" */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoToken {

	private String prefix;

	private String token;
}
