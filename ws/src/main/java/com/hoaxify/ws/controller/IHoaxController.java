package com.hoaxify.ws.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.hoaxify.ws.dto.DtoHoax;
import com.hoaxify.ws.dto.DtoHoaxIU;
import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.security.CurrentUser;

public interface IHoaxController {

	ResponseEntity<DtoHoax> createHoax(DtoHoaxIU dtoHoaxIU, CurrentUser currentUser);

	ResponseEntity<DtoPage<DtoHoax>> getHoaxes(String tag, Pageable pageable);

	ResponseEntity<DtoPage<DtoHoax>> getHoaxesOfUser(Long userId, Pageable pageable);

	ResponseEntity<DtoHoax> getHoaxById(Long id);

	ResponseEntity<Void> deleteHoax(Long id, CurrentUser currentUser);
}
