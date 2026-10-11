package com.hoaxify.hoax.services.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.hoax.client.IUserQueryClient;
import com.hoaxify.hoax.dao.IHoaxStatsDao;
import com.hoaxify.hoax.dto.DtoTagStats;
import com.hoaxify.hoax.dto.DtoUser;
import com.hoaxify.hoax.dto.DtoUserStats;
import com.hoaxify.hoax.dto.DtoUserSummary;
import com.hoaxify.hoax.dto.UserHoaxCount;
import com.hoaxify.hoax.exception.HoaxErrorType;
import com.hoaxify.hoax.services.IStatsService;

import lombok.RequiredArgsConstructor;

/**
 * Monolith'te tek SQL (JOIN users) ile gelen rapor burada iki adımda oluşuyor:
 *   1) DAO: kendi veritabanımızdan userId + sayı
 *   2) gRPC: bu id'lerin kullanıcı adı ve resmi (tek toplu çağrı)
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements IStatsService {

	static final int MIN_LIMIT = 1;
	static final int MAX_LIMIT = 20;

	private final IHoaxStatsDao hoaxStatsDao;
	private final IUserQueryClient userQueryClient;

	@Override
	@Transactional(readOnly = true)
	public List<DtoUserStats> getTopPosters(int limit) {
		int safeLimit = Math.max(MIN_LIMIT, Math.min(limit, MAX_LIMIT));
		List<UserHoaxCount> counts = hoaxStatsDao.findTopPosters(safeLimit);
		if (counts.isEmpty()) {
			return List.of();
		}
		Map<Long, DtoUser> users = userQueryClient.findUsers(counts.stream().map(UserHoaxCount::userId).toList());
		return counts.stream()
				.map(count -> {
					DtoUser user = users.getOrDefault(count.userId(), DtoUser.unknown(count.userId()));
					return new DtoUserStats(count.userId(), user.username(), user.image(), count.hoaxCount());
				})
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<DtoTagStats> getTagUsage() {
		return hoaxStatsDao.findTagUsage();
	}

	@Override
	@Transactional(readOnly = true)
	public DtoUserSummary getUserSummary(Long userId) {
		if (userQueryClient.findUser(userId).isEmpty()) {
			throw new BaseException(HoaxErrorType.USER_NOT_FOUND, String.valueOf(userId));
		}
		long hoaxCount = hoaxStatsDao.countHoaxesOfUser(userId);
		if (hoaxCount == 0) {
			return new DtoUserSummary(userId, 0, null);
		}
		return new DtoUserSummary(userId, hoaxCount, hoaxStatsDao.findLastHoaxAt(userId).orElse(null));
	}
}
