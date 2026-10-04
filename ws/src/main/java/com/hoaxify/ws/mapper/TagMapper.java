package com.hoaxify.ws.mapper;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.hoaxify.ws.dto.DtoTag;
import com.hoaxify.ws.entities.Tag;

@Component
public class TagMapper {

	public DtoTag toDto(Tag tag) {
		// Aynı isimli basit alanlar (id, name) için BeanUtils yeterli.
		// İlişkili alanları (hoaxes) kopyalamaz, bu yüzden ilişkiler her zaman elle map'lenir.
		DtoTag dto = new DtoTag();
		BeanUtils.copyProperties(tag, dto);
		return dto;
	}
}
