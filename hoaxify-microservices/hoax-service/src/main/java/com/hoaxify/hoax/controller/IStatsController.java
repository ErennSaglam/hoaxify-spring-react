package com.hoaxify.hoax.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.hoaxify.hoax.dto.DtoTagStats;
import com.hoaxify.hoax.dto.DtoUserStats;
import com.hoaxify.hoax.dto.DtoUserSummary;

public interface IStatsController {

	ResponseEntity<List<DtoUserStats>> getTopPosters(int limit);

	ResponseEntity<List<DtoTagStats>> getTagUsage();

	ResponseEntity<DtoUserSummary> getUserSummary(Long userId);
}
