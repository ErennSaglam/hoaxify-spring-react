package com.hoaxify.ws.controller.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.hoaxify.ws.dto.DtoTagStats;
import com.hoaxify.ws.dto.DtoUserStats;
import com.hoaxify.ws.dto.DtoUserSummary;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.services.IStatsService;

@ExtendWith(MockitoExtension.class)
class StatsControllerImplTest {

	@Mock
	private IStatsService statsService;

	@InjectMocks
	private StatsControllerImpl statsController;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	@Test
	void getTopPosters_success() {
		// Arrange
		List<DtoUserStats> response = List.of(easyRandom.nextObject(DtoUserStats.class));
		when(statsService.getTopPosters(5)).thenReturn(response);

		// Act
		ResponseEntity<List<DtoUserStats>> result = statsController.getTopPosters(5);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(response, result.getBody());
		verify(statsService, times(1)).getTopPosters(5);
	}

	@Test
	void getTagUsage_emptyList() {
		// Arrange
		when(statsService.getTagUsage()).thenReturn(Collections.emptyList());

		// Act
		ResponseEntity<List<DtoTagStats>> result = statsController.getTagUsage();

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertTrue(result.getBody().isEmpty());
	}

	@Test
	void getUserSummary_success() {
		// Arrange
		DtoUserSummary response = easyRandom.nextObject(DtoUserSummary.class);
		when(statsService.getUserSummary(1L)).thenReturn(response);

		// Act
		ResponseEntity<DtoUserSummary> result = statsController.getUserSummary(1L);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(response.getHoaxCount(), result.getBody().getHoaxCount());
	}

	@Test
	void getUserSummary_notFound_shouldPropagate() {
		// Arrange
		when(statsService.getUserSummary(99L)).thenThrow(new BaseException(MessageType.USER_NOT_FOUND, "99"));

		// Act & Assert
		assertThrows(BaseException.class, () -> statsController.getUserSummary(99L));
	}
}
