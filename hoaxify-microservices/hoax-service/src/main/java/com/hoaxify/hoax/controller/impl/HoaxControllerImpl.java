package com.hoaxify.hoax.controller.impl;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hoaxify.common.web.dto.DtoPage;
import com.hoaxify.common.web.security.CurrentUserId;
import com.hoaxify.hoax.controller.IHoaxController;
import com.hoaxify.hoax.dto.DtoHoax;
import com.hoaxify.hoax.dto.DtoHoaxIU;
import com.hoaxify.hoax.dto.DtoTag;
import com.hoaxify.hoax.services.IHoaxService;
import com.hoaxify.hoax.services.ITagService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HoaxControllerImpl implements IHoaxController {

	private final IHoaxService hoaxService;
	private final ITagService tagService;

	/** Yazar, gövdeden değil gateway'in doğruladığı token'dan (X-User-Id) gelir */
	@PostMapping("/hoaxes")
	@Override
	public ResponseEntity<DtoHoax> createHoax(@Valid @RequestBody DtoHoaxIU request,
			@CurrentUserId Long currentUserId) {
		DtoHoax created = hoaxService.createHoax(currentUserId, request);
		return ResponseEntity.created(URI.create("/api/v1/hoaxes/" + created.id())).body(created);
	}

	@GetMapping("/hoaxes")
	@Override
	public ResponseEntity<DtoPage<DtoHoax>> getHoaxes(@RequestParam(required = false) String tag,
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(hoaxService.getHoaxes(tag, pageable));
	}

	@GetMapping("/users/{userId}/hoaxes")
	@Override
	public ResponseEntity<DtoPage<DtoHoax>> getHoaxesOfUser(@PathVariable Long userId,
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(hoaxService.getHoaxesOfUser(userId, pageable));
	}

	@GetMapping("/hoaxes/{id}")
	@Override
	public ResponseEntity<DtoHoax> getHoaxById(@PathVariable Long id) {
		return ResponseEntity.ok(hoaxService.getHoaxById(id));
	}

	@DeleteMapping("/hoaxes/{id}")
	@Override
	public ResponseEntity<Void> deleteHoax(@PathVariable Long id, @CurrentUserId Long currentUserId) {
		hoaxService.deleteHoax(id, currentUserId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/tags")
	@Override
	public ResponseEntity<List<DtoTag>> getTags() {
		return ResponseEntity.ok(tagService.getTags());
	}
}
