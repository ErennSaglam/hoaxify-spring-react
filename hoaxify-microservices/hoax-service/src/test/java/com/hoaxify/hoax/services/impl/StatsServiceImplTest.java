package com.hoaxify.hoax.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.hoax.client.IUserQueryClient;
import com.hoaxify.hoax.dao.IHoaxStatsDao;
import com.hoaxify.hoax.dto.DtoUser;
import com.hoaxify.hoax.dto.DtoUserStats;
import com.hoaxify.hoax.dto.DtoUserSummary;
import com.hoaxify.hoax.dto.UserHoaxCount;
import com.hoaxify.hoax.exception.HoaxErrorType;

/**
 * Tespit edilen senaryolar:
 *  getTopPosters  : DAO sonucu + gRPC ile isim/resim birleşir | limit sınırlara çekilir
 *                   | DAO boş -> gRPC çağrılmaz | kullanıcı bulunamadı -> "[deleted]"
 *  getUserSummary : kullanıcı yok | hoax yok (son tarih sorgulanmaz) | hoax var
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

	@Mock
	private IHoaxStatsDao hoaxStatsDao;

	@Mock
	private IUserQueryClient userQueryClient;

	@InjectMocks
	private StatsServiceImpl statsService;

	@Test
	void getTopPosters_success_shouldEnrichWithUserInfo() {
		// Arrange
		when(hoaxStatsDao.findTopPosters(5)).thenReturn(List.of(new UserHoaxCount(1L, 7), new UserHoaxCount(2L, 3)));
		when(userQueryClient.findUsers(List.of(1L, 2L))).thenReturn(Map.of(
				1L, new DtoUser(1L, "user1", "u1@mail.com", "a.png", null),
				2L, new DtoUser(2L, "user2", "u2@mail.com", null, null)));

		// Act
		List<DtoUserStats> result = statsService.getTopPosters(5);

		// Assert
		assertEquals(List.of(new DtoUserStats(1L, "user1", "a.png", 7), new DtoUserStats(2L, "user2", null, 3)),
				result);
	}

	@ParameterizedTest(name = "limit={0} -> {1}")
	@CsvSource({ "0, 1", "5, 5", "50, 20" })
	void getTopPosters_limitIsClamped(int requested, int expected) {
		// Arrange
		when(hoaxStatsDao.findTopPosters(expected)).thenReturn(Collections.emptyList());

		// Act
		statsService.getTopPosters(requested);

		// Assert
		verify(hoaxStatsDao, times(1)).findTopPosters(expected);
	}

	@Test
	void getTopPosters_emptyList_shouldNotCallUserService() {
		// Arrange
		when(hoaxStatsDao.findTopPosters(5)).thenReturn(Collections.emptyList());

		// Act & Assert
		assertTrue(statsService.getTopPosters(5).isEmpty());
		verifyNoInteractions(userQueryClient);
	}

	@Test
	void getTopPosters_userMissing_shouldUseUnknownUsername() {
		// Arrange
		when(hoaxStatsDao.findTopPosters(5)).thenReturn(List.of(new UserHoaxCount(9L, 1)));
		when(userQueryClient.findUsers(List.of(9L))).thenReturn(Map.of());

		// Act & Assert
		assertEquals("[deleted]", statsService.getTopPosters(5).get(0).username());
	}

	@Test
	void getUserSummary_userNotFound_shouldNotCallDao() {
		// Arrange
		when(userQueryClient.findUser(9L)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> statsService.getUserSummary(9L));

		// Assert
		assertEquals(HoaxErrorType.USER_NOT_FOUND, exception.getErrorType());
		verifyNoInteractions(hoaxStatsDao);
	}

	@Test
	void getUserSummary_noHoaxes() {
		// Arrange
		when(userQueryClient.findUser(1L)).thenReturn(Optional.of(new DtoUser(1L, "u", "e", null, null)));
		when(hoaxStatsDao.countHoaxesOfUser(1L)).thenReturn(0L);

		// Act
		DtoUserSummary result = statsService.getUserSummary(1L);

		// Assert
		assertEquals(0, result.hoaxCount());
		assertNull(result.lastHoaxAt());
		verify(hoaxStatsDao, never()).findLastHoaxAt(any());
	}

	@Test
	void getUserSummary_withHoaxes() {
		// Arrange
		LocalDateTime last = LocalDateTime.of(2026, 10, 1, 9, 0);
		when(userQueryClient.findUser(1L)).thenReturn(Optional.of(new DtoUser(1L, "u", "e", null, null)));
		when(hoaxStatsDao.countHoaxesOfUser(1L)).thenReturn(4L);
		when(hoaxStatsDao.findLastHoaxAt(1L)).thenReturn(Optional.of(last));

		// Act
		DtoUserSummary result = statsService.getUserSummary(1L);

		// Assert
		assertEquals(new DtoUserSummary(1L, 4, last), result);
	}
}
