package com.hoaxify.ws.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hoax response DTO'su. İlişkiler de DTO olarak döner (entity asla dışarı çıkmaz).
 * Döngü yok: DtoUser içinde hoax listesi, DtoTag içinde hoax listesi tutulmuyor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoHoax {

	private Long id;

	private String content;

	private LocalDateTime createdAt;

	private DtoUser user;

	private List<DtoTag> tags = new ArrayList<>();
}
