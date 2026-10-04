package com.hoaxify.ws.controller.impl;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hoaxify.ws.controller.ITagController;
import com.hoaxify.ws.dto.DtoTag;
import com.hoaxify.ws.services.ITagService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagControllerImpl implements ITagController {

	private final ITagService tagService;

	/** Etiketler kullanım sayısına göre sıralı. Bir etiketin hoax'ları: GET /api/v1/hoaxes?tag={name} */
	@GetMapping
	@Override
	public ResponseEntity<List<DtoTag>> getTags() {
		return ResponseEntity.ok(tagService.getTags());
	}
}
