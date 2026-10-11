package com.hoaxify.hoax.services;

import org.springframework.data.domain.Pageable;

import com.hoaxify.common.web.dto.DtoPage;
import com.hoaxify.hoax.dto.DtoHoax;
import com.hoaxify.hoax.dto.DtoHoaxIU;

public interface IHoaxService {

	DtoHoax createHoax(Long authorId, DtoHoaxIU request);

	DtoPage<DtoHoax> getHoaxes(String tag, Pageable pageable);

	DtoPage<DtoHoax> getHoaxesOfUser(Long userId, Pageable pageable);

	DtoHoax getHoaxById(Long id);

	void deleteHoax(Long id, Long currentUserId);

	/** UserDeletedEvent: kullanıcının tüm hoax'larını siler; hiç yoksa bir şey yapmaz */
	void deleteAllOfUser(Long userId);
}
