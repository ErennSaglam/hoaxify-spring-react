package com.hoaxify.ws.services;

import java.util.List;

import com.hoaxify.ws.dto.DtoTagStats;
import com.hoaxify.ws.dto.DtoUserStats;
import com.hoaxify.ws.dto.DtoUserSummary;

public interface IStatsService {

	List<DtoUserStats> getTopPosters(int limit);

	List<DtoTagStats> getTagUsage();

	DtoUserSummary getUserSummary(Long userId);
}
