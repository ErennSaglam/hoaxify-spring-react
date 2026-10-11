package com.hoaxify.hoax.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.hoaxify.common.web.dto.DtoPage;
import com.hoaxify.hoax.dto.DtoHoax;
import com.hoaxify.hoax.dto.DtoHoaxIU;
import com.hoaxify.hoax.dto.DtoTag;

public interface IHoaxController {

	ResponseEntity<DtoHoax> createHoax(DtoHoaxIU request, Long currentUserId);

	ResponseEntity<DtoPage<DtoHoax>> getHoaxes(String tag, Pageable pageable);

	ResponseEntity<DtoPage<DtoHoax>> getHoaxesOfUser(Long userId, Pageable pageable);

	ResponseEntity<DtoHoax> getHoaxById(Long id);

	ResponseEntity<Void> deleteHoax(Long id, Long currentUserId);

	ResponseEntity<List<DtoTag>> getTags();
}
