package com.hoaxify.ws.controller.impl;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hoaxify.ws.controller.IStatsController;
import com.hoaxify.ws.dto.DtoTagStats;
import com.hoaxify.ws.dto.DtoUserStats;
import com.hoaxify.ws.dto.DtoUserSummary;
import com.hoaxify.ws.services.IStatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StatsControllerImpl implements IStatsController {

	private final IStatsService statsService;

	/** ?limit=5 (1-20 arasına çekilir) */
	@GetMapping("/stats/top-users")
	@Override
	public ResponseEntity<List<DtoUserStats>> getTopPosters(@RequestParam(defaultValue = "5") int limit) {
		return ResponseEntity.ok(statsService.getTopPosters(limit));
	}

	@GetMapping("/stats/tags")
	@Override
	public ResponseEntity<List<DtoTagStats>> getTagUsage() {
		return ResponseEntity.ok(statsService.getTagUsage());
	}

	@GetMapping("/users/{userId}/summary")
	@Override
	public ResponseEntity<DtoUserSummary> getUserSummary(@PathVariable Long userId) {
		return ResponseEntity.ok(statsService.getUserSummary(userId));
	}
}
