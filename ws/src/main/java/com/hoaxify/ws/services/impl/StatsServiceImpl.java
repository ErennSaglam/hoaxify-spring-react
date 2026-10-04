package com.hoaxify.ws.services.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.ws.dao.IHoaxStatsDao;
import com.hoaxify.ws.dao.IUserStatsDao;
import com.hoaxify.ws.dto.DtoTagStats;
import com.hoaxify.ws.dto.DtoUserStats;
import com.hoaxify.ws.dto.DtoUserSummary;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.repository.UserRepository;
import com.hoaxify.ws.services.IStatsService;

import lombok.RequiredArgsConstructor;

/**
 * Servis katmanı DAO'yu da repository'yi de aynı şekilde kullanır: sadece arayüzlerini bilir.
 * Yarın SQL yerine başka bir kaynak (ör. rapor veritabanı) gelse servis değişmez.
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements IStatsService {

	static final int MIN_LIMIT = 1;
	static final int MAX_LIMIT = 20;

	private final IUserStatsDao userStatsDao;
	private final IHoaxStatsDao hoaxStatsDao;
	private final UserRepository userRepository;

	/** limit istemciden gelir; aralık dışı değerleri reddetmek yerine sınırlara çekiyoruz */
	@Override
	@Transactional(readOnly = true)
	public List<DtoUserStats> getTopPosters(int limit) {
		int safeLimit = Math.max(MIN_LIMIT, Math.min(limit, MAX_LIMIT));
		return userStatsDao.findTopPosters(safeLimit);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DtoTagStats> getTagUsage() {
		return hoaxStatsDao.findTagUsage();
	}

	@Override
	@Transactional(readOnly = true)
	public DtoUserSummary getUserSummary(Long userId) {
		if (!userRepository.existsById(userId)) {
			throw new BaseException(MessageType.USER_NOT_FOUND, String.valueOf(userId));
		}
		long hoaxCount = userStatsDao.countHoaxesOfUser(userId);
		// Hiç hoax yoksa son tarih sorgusuna gitmeye gerek yok
		if (hoaxCount == 0) {
			return new DtoUserSummary(userId, 0, null);
		}
		return new DtoUserSummary(userId, hoaxCount, userStatsDao.findLastHoaxAt(userId).orElse(null));
	}
}
