package com.hoaxify.ws.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.hoaxify.ws.dto.DtoTagStats;
import com.hoaxify.ws.dto.DtoUserStats;
import com.hoaxify.ws.dto.DtoUserSummary;

public interface IStatsController {

	ResponseEntity<List<DtoUserStats>> getTopPosters(int limit);

	ResponseEntity<List<DtoTagStats>> getTagUsage();

	ResponseEntity<DtoUserSummary> getUserSummary(Long userId);
}
