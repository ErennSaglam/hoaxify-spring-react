package com.hoaxify.hoax.mapper;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.hoaxify.hoax.dto.DtoHoax;
import com.hoaxify.hoax.dto.DtoHoaxIU;
import com.hoaxify.hoax.dto.DtoTag;
import com.hoaxify.hoax.dto.DtoUser;
import com.hoaxify.hoax.entities.Hoax;
import com.hoaxify.hoax.entities.Tag;

/**
 * Monolith'te yazar hoax.getUser()'dan geliyordu. Burada iki kaynağı birleştiriyoruz:
 * hoax kendi veritabanımızdan, yazar user-service'ten (gRPC). Bu birleştirmeye "API composition" denir.
 */
@Component
public class HoaxMapper {

	public DtoHoax toDto(Hoax hoax, DtoUser author) {
		return new DtoHoax(
				hoax.getId(),
				hoax.getContent(),
				hoax.getCreatedAt(),
				author,
				hoax.getTags().stream()
						.sorted(Comparator.comparing(Tag::getName))
						.map(this::toDto)
						.toList());
	}

	public DtoTag toDto(Tag tag) {
		return new DtoTag(tag.getId(), tag.getName());
	}

	public Hoax toEntity(DtoHoaxIU request, Long userId) {
		Hoax hoax = new Hoax();
		hoax.setContent(request.getContent().trim());
		hoax.setUserId(userId);
		return hoax;
	}
}
