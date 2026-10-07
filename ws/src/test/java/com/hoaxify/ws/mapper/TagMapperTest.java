package com.hoaxify.ws.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.Test;

import com.hoaxify.ws.dto.DtoTag;
import com.hoaxify.ws.entities.Tag;

class TagMapperTest {

	private final TagMapper tagMapper = new TagMapper();

	private final EasyRandom easyRandom = new EasyRandom();

	@Test
	void toDto_success() {
		// Arrange
		Tag tag = easyRandom.nextObject(Tag.class);

		// Act
		DtoTag result = tagMapper.toDto(tag);

		// Assert
		assertEquals(tag.getId(), result.getId());
		assertEquals(tag.getName(), result.getName());
	}
}
