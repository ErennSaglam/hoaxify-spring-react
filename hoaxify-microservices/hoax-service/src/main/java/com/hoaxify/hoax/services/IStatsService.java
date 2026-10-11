package com.hoaxify.hoax.services;

import java.util.List;

import com.hoaxify.hoax.dto.DtoTagStats;
import com.hoaxify.hoax.dto.DtoUserStats;
import com.hoaxify.hoax.dto.DtoUserSummary;

public interface IStatsService {

	List<DtoUserStats> getTopPosters(int limit);

	List<DtoTagStats> getTagUsage();

	DtoUserSummary getUserSummary(Long userId);
}
