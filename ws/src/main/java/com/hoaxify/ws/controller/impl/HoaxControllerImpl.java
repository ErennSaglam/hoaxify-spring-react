package com.hoaxify.ws.controller.impl;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hoaxify.ws.controller.IHoaxController;
import com.hoaxify.ws.dto.DtoHoax;
import com.hoaxify.ws.dto.DtoHoaxIU;
import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.security.CurrentUser;
import com.hoaxify.ws.services.IHoaxService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HoaxControllerImpl implements IHoaxController {

	private final IHoaxService hoaxService;

	/** SecurityConfiguration'da authenticated olarak işaretli, currentUser null gelmez */
	@PostMapping("/hoaxes")
	@Override
	public ResponseEntity<DtoHoax> createHoax(@Valid @RequestBody DtoHoaxIU dtoHoaxIU,
			@AuthenticationPrincipal CurrentUser currentUser) {
		DtoHoax created = hoaxService.createHoax(currentUser.getId(), dtoHoaxIU);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(created.getId()).toUri();
		return ResponseEntity.created(location).body(created);
	}

	/** ?tag=java&page=0&size=10 - en yeni hoax'lar önce */
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
	public ResponseEntity<Void> deleteHoax(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser) {
		hoaxService.deleteHoax(id, currentUser.getId());
		return ResponseEntity.noContent().build();
	}
}
