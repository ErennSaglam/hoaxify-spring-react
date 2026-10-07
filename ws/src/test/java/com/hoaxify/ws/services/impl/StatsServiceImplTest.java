package com.hoaxify.ws.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import com.hoaxify.ws.dao.IHoaxStatsDao;
import com.hoaxify.ws.dao.IUserStatsDao;
import com.hoaxify.ws.dto.DtoTagStats;
import com.hoaxify.ws.dto.DtoUserStats;
import com.hoaxify.ws.dto.DtoUserSummary;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.repository.UserRepository;

/**
 * Tespit edilen senaryolar:
 *  getTopPosters  : success | limit alt sınırın altında -> 1 | üst sınırın üstünde -> 20 | boş liste | DAO hatası
 *  getTagUsage    : success | boş liste
 *  getUserSummary : kullanıcı yok (DAO çağrılmaz) | hoax yok (erken return, son tarih sorgulanmaz) | hoax var
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

	@Mock
	private IUserStatsDao userStatsDao;

	@Mock
	private IHoaxStatsDao hoaxStatsDao;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private StatsServiceImpl statsService;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	// ------------------------------------------------------------------ getTopPosters

	@Test
	void getTopPosters_success() {
		// Arrange
		List<DtoUserStats> expected = List.of(easyRandom.nextObject(DtoUserStats.class),
				easyRandom.nextObject(DtoUserStats.class));
		when(userStatsDao.findTopPosters(5)).thenReturn(expected);

		// Act
		List<DtoUserStats> result = statsService.getTopPosters(5);

		// Assert
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(expected.get(0).getUsername(), result.get(0).getUsername());
		verify(userStatsDao, times(1)).findTopPosters(5);
	}

	@ParameterizedTest(name = "limit={0} -> DAO''ya {1} gider")
	@CsvSource({ "0, 1", "-5, 1", "1, 1", "20, 20", "21, 20", "1000, 20" })
	void getTopPosters_limitIsClampedToRange(int requested, int expected) {
		// Arrange
		when(userStatsDao.findTopPosters(expected)).thenReturn(Collections.emptyList());

		// Act
		statsService.getTopPosters(requested);

		// Assert
		verify(userStatsDao, times(1)).findTopPosters(expected);
	}

	@Test
	void getTopPosters_emptyList() {
		// Arrange
		when(userStatsDao.findTopPosters(5)).thenReturn(Collections.emptyList());

		// Act
		List<DtoUserStats> result = statsService.getTopPosters(5);

		// Assert
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void getTopPosters_daoException_shouldPropagate() {
		// Arrange
		when(userStatsDao.findTopPosters(5)).thenThrow(new DataAccessResourceFailureException("DB down"));

		// Act & Assert
		assertThrows(DataAccessResourceFailureException.class, () -> statsService.getTopPosters(5));
	}

	// ------------------------------------------------------------------ getTagUsage

	@Test
	void getTagUsage_success() {
		// Arrange
		List<DtoTagStats> expected = List.of(new DtoTagStats("java", 3), new DtoTagStats("spring", 1));
		when(hoaxStatsDao.findTagUsage()).thenReturn(expected);

		// Act
		List<DtoTagStats> result = statsService.getTagUsage();

		// Assert
		assertEquals(expected, result);
		verify(hoaxStatsDao, times(1)).findTagUsage();
		verifyNoInteractions(userStatsDao, userRepository);
	}

	@Test
	void getTagUsage_emptyList() {
		// Arrange
		when(hoaxStatsDao.findTagUsage()).thenReturn(Collections.emptyList());

		// Act & Assert
		assertTrue(statsService.getTagUsage().isEmpty());
	}

	// ------------------------------------------------------------------ getUserSummary

	@Test
	void getUserSummary_userNotFound_shouldNotCallDao() {
		// Arrange
		Long userId = 99L;
		when(userRepository.existsById(userId)).thenReturn(false);

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> statsService.getUserSummary(userId));

		// Assert
		assertEquals(MessageType.USER_NOT_FOUND, exception.getMessageType());
		assertEquals("99", exception.getArgs()[0]);
		verifyNoInteractions(userStatsDao);
	}

	@Test
	void getUserSummary_noHoaxes_shouldNotQueryLastHoaxDate() {
		// Arrange
		Long userId = 1L;
		when(userRepository.existsById(userId)).thenReturn(true);
		when(userStatsDao.countHoaxesOfUser(userId)).thenReturn(0L);

		// Act
		DtoUserSummary result = statsService.getUserSummary(userId);

		// Assert
		assertEquals(userId, result.getUserId());
		assertEquals(0, result.getHoaxCount());
		assertNull(result.getLastHoaxAt());
		verify(userStatsDao, never()).findLastHoaxAt(any());
	}

	@Test
	void getUserSummary_withHoaxes_success() {
		// Arrange
		Long userId = 1L;
		LocalDateTime lastHoaxAt = LocalDateTime.of(2026, 9, 30, 10, 0);
		when(userRepository.existsById(userId)).thenReturn(true);
		when(userStatsDao.countHoaxesOfUser(userId)).thenReturn(7L);
		when(userStatsDao.findLastHoaxAt(userId)).thenReturn(Optional.of(lastHoaxAt));

		// Act
		DtoUserSummary result = statsService.getUserSummary(userId);

		// Assert
		assertEquals(7, result.getHoaxCount());
		assertEquals(lastHoaxAt, result.getLastHoaxAt());
		verify(userStatsDao, times(1)).countHoaxesOfUser(userId);
		verify(userStatsDao, times(1)).findLastHoaxAt(userId);
	}
}
