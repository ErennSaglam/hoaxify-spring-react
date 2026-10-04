package com.hoaxify.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** "En aktif kullanıcılar" raporunun bir satırı */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoUserStats {

	private Long userId;

	private String username;

	private String image;

	private long hoaxCount;
}
