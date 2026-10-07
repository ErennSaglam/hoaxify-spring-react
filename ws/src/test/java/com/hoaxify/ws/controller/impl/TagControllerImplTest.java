package com.hoaxify.ws.controller.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hoaxify.ws.dto.DtoTag;
import com.hoaxify.ws.services.ITagService;

@ExtendWith(MockitoExtension.class)
class TagControllerImplTest {

	@Mock
	private ITagService tagService;

	@InjectMocks
	private TagControllerImpl tagController;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	@Test
	void getTags_success() {
		// Arrange
		List<DtoTag> tags = List.of(easyRandom.nextObject(DtoTag.class), easyRandom.nextObject(DtoTag.class));
		when(tagService.getTags()).thenReturn(tags);

		// Act
		ResponseEntity<List<DtoTag>> result = tagController.getTags();

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(2, result.getBody().size());
		assertEquals(tags.get(0).getName(), result.getBody().get(0).getName());
		verify(tagService, times(1)).getTags();
	}

	@Test
	void getTags_emptyList() {
		// Arrange
		when(tagService.getTags()).thenReturn(Collections.emptyList());

		// Act
		ResponseEntity<List<DtoTag>> result = tagController.getTags();

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertTrue(result.getBody().isEmpty());
	}
}
