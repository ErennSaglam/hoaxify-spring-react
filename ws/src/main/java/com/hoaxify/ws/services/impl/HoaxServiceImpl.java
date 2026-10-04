package com.hoaxify.ws.services.impl;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.ws.dto.DtoHoax;
import com.hoaxify.ws.dto.DtoHoaxIU;
import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.entities.Hoax;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.mapper.HoaxMapper;
import com.hoaxify.ws.repository.HoaxRepository;
import com.hoaxify.ws.repository.UserRepository;
import com.hoaxify.ws.services.IHoaxService;
import com.hoaxify.ws.services.ITagService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoaxServiceImpl implements IHoaxService {

	private final HoaxRepository hoaxRepository;
	private final UserRepository userRepository;
	private final ITagService tagService;
	private final HoaxMapper hoaxMapper;

	@Override
	@Transactional
	public DtoHoax createHoax(Long authorId, DtoHoaxIU dtoHoaxIU) {
		User author = userRepository.findById(authorId)
				.orElseThrow(() -> new BaseException(MessageType.USER_NOT_FOUND, String.valueOf(authorId)));

		Hoax hoax = hoaxMapper.toEntity(dtoHoaxIU);
		hoax.setUser(author);
		hoax.setTags(tagService.findOrCreate(dtoHoaxIU.getTags()));

		return hoaxMapper.toDto(hoaxRepository.save(hoax));
	}

	@Override
	@Transactional(readOnly = true)
	public DtoPage<DtoHoax> getHoaxes(String tag, Pageable pageable) {
		Page<Hoax> hoaxes = tag == null || tag.isBlank()
				? hoaxRepository.findAll(pageable)
				: hoaxRepository.findByTagName(tagService.normalize(tag), pageable);
		return DtoPage.of(hoaxes, hoaxMapper::toDto);
	}

	@Override
	@Transactional(readOnly = true)
	public DtoPage<DtoHoax> getHoaxesOfUser(Long userId, Pageable pageable) {
		if (!userRepository.existsById(userId)) {
			throw new BaseException(MessageType.USER_NOT_FOUND, String.valueOf(userId));
		}
		return DtoPage.of(hoaxRepository.findByUserId(userId, pageable), hoaxMapper::toDto);
	}

	@Override
	@Transactional(readOnly = true)
	public DtoHoax getHoaxById(Long id) {
		return hoaxMapper.toDto(findHoaxOrThrow(id));
	}

	/**
	 * Yetki kontrolü servis katmanında: sahiplik bilgisi veritabanındaki kayda bağlı olduğu
	 * için @PreAuthorize ile path'ten kontrol edilemez (UserController'daki durumdan farkı bu).
	 */
	@Override
	@Transactional
	public void deleteHoax(Long id, Long currentUserId) {
		Hoax hoax = findHoaxOrThrow(id);
		if (!Objects.equals(hoax.getUser().getId(), currentUserId)) {
			throw new BaseException(MessageType.HOAX_DELETE_FORBIDDEN);
		}
		hoaxRepository.delete(hoax);
	}

	private Hoax findHoaxOrThrow(Long id) {
		return hoaxRepository.findById(id)
				.orElseThrow(() -> new BaseException(MessageType.HOAX_NOT_FOUND, String.valueOf(id)));
	}
}
