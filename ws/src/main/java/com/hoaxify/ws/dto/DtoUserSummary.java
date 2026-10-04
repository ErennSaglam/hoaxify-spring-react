package com.hoaxify.ws.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Profil sayfasındaki özet: toplam hoax sayısı ve son paylaşım zamanı */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoUserSummary {

	private Long userId;

	private long hoaxCount;

	/** Hiç hoax yoksa null */
	private LocalDateTime lastHoaxAt;
}
