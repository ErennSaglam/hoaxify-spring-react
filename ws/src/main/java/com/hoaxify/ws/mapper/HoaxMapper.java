package com.hoaxify.ws.mapper;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.hoaxify.ws.dto.DtoHoax;
import com.hoaxify.ws.dto.DtoHoaxIU;
import com.hoaxify.ws.entities.Hoax;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HoaxMapper {

	private final UserMapper userMapper;
	private final TagMapper tagMapper;

	public DtoHoax toDto(Hoax hoax) {
		DtoHoax dto = new DtoHoax();
		dto.setId(hoax.getId());
		dto.setContent(hoax.getContent());
		dto.setCreatedAt(hoax.getCreatedAt());
		dto.setUser(userMapper.toDto(hoax.getUser()));
		dto.setTags(hoax.getTags().stream()
				.sorted(Comparator.comparing(tag -> tag.getName()))
				.map(tagMapper::toDto)
				.toList());
		return dto;
	}

	/** Sadece basit alanlar; yazar ve etiketler servis tarafından bağlanır. */
	public Hoax toEntity(DtoHoaxIU dto) {
		Hoax hoax = new Hoax();
		hoax.setContent(dto.getContent().trim());
		return hoax;
	}
}
