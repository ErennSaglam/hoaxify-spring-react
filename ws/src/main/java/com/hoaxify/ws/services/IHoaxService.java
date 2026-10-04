package com.hoaxify.ws.services;

import org.springframework.data.domain.Pageable;

import com.hoaxify.ws.dto.DtoHoax;
import com.hoaxify.ws.dto.DtoHoaxIU;
import com.hoaxify.ws.dto.DtoPage;

public interface IHoaxService {

	DtoHoax createHoax(Long authorId, DtoHoaxIU dtoHoaxIU);

	/** tag null ise tüm hoax'lar, değilse o etiketi taşıyanlar */
	DtoPage<DtoHoax> getHoaxes(String tag, Pageable pageable);

	DtoPage<DtoHoax> getHoaxesOfUser(Long userId, Pageable pageable);

	DtoHoax getHoaxById(Long id);

	void deleteHoax(Long id, Long currentUserId);
}
