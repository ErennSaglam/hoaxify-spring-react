package com.hoaxify.hoax.services.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.common.web.dto.DtoPage;
import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.hoax.client.IUserQueryClient;
import com.hoaxify.hoax.dto.DtoHoax;
import com.hoaxify.hoax.dto.DtoHoaxIU;
import com.hoaxify.hoax.dto.DtoUser;
import com.hoaxify.hoax.entities.Hoax;
import com.hoaxify.hoax.exception.HoaxErrorType;
import com.hoaxify.hoax.mapper.HoaxMapper;
import com.hoaxify.hoax.repository.HoaxRepository;
import com.hoaxify.hoax.services.IHoaxService;
import com.hoaxify.hoax.services.ITagService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoaxServiceImpl implements IHoaxService {

	private final HoaxRepository hoaxRepository;
	private final ITagService tagService;
	private final IUserQueryClient userQueryClient;
	private final HoaxMapper hoaxMapper;

	/**
	 * Monolith'te userRepository.findById(authorId) idi. Artık kullanıcı başka serviste:
	 * gRPC ile "bu kullanıcı var mı" diye soruyoruz ve cevaptaki bilgiyi response'ta da kullanıyoruz.
	 */
	@Override
	@Transactional
	public DtoHoax createHoax(Long authorId, DtoHoaxIU request) {
		DtoUser author = userQueryClient.findUser(authorId)
				.orElseThrow(() -> new BaseException(HoaxErrorType.USER_NOT_FOUND, String.valueOf(authorId)));

		Hoax hoax = hoaxMapper.toEntity(request, authorId);
		hoax.setTags(tagService.findOrCreate(request.getTags()));
		return hoaxMapper.toDto(hoaxRepository.save(hoax), author);
	}

	@Override
	@Transactional(readOnly = true)
	public DtoPage<DtoHoax> getHoaxes(String tag, Pageable pageable) {
		Page<Hoax> page = tag == null || tag.isBlank()
				? hoaxRepository.findAll(pageable)
				: hoaxRepository.findByTagName(tagService.normalize(tag), pageable);
		return withAuthors(page);
	}

	@Override
	@Transactional(readOnly = true)
	public DtoPage<DtoHoax> getHoaxesOfUser(Long userId, Pageable pageable) {
		if (userQueryClient.findUser(userId).isEmpty()) {
			throw new BaseException(HoaxErrorType.USER_NOT_FOUND, String.valueOf(userId));
		}
		return withAuthors(hoaxRepository.findByUserId(userId, pageable));
	}

	@Override
	@Transactional(readOnly = true)
	public DtoHoax getHoaxById(Long id) {
		Hoax hoax = findOrThrow(id);
		DtoUser author = userQueryClient.findUser(hoax.getUserId()).orElse(DtoUser.unknown(hoax.getUserId()));
		return hoaxMapper.toDto(hoax, author);
	}

	@Override
	@Transactional
	public void deleteHoax(Long id, Long currentUserId) {
		Hoax hoax = findOrThrow(id);
		if (!Objects.equals(hoax.getUserId(), currentUserId)) {
			throw new BaseException(HoaxErrorType.HOAX_DELETE_FORBIDDEN);
		}
		hoaxRepository.delete(hoax);
	}

	@Override
	@Transactional
	public void deleteAllOfUser(Long userId) {
		List<Hoax> hoaxes = hoaxRepository.findAllByUserId(userId);
		hoaxRepository.deleteAll(hoaxes);
		log.info("Deleted {} hoaxes of deleted user {}", hoaxes.size(), userId);
	}

	/**
	 * Sayfadaki hoax'ların yazarlarını TEK gRPC çağrısıyla getirir (10 hoax için 10 çağrı değil).
	 * Aynı kullanıcının birden fazla hoax'ı varsa id'ler Set ile tekilleştirilir.
	 */
	private DtoPage<DtoHoax> withAuthors(Page<Hoax> page) {
		Set<Long> authorIds = page.getContent().stream().map(Hoax::getUserId).collect(Collectors.toSet());
		Map<Long, DtoUser> authors = userQueryClient.findUsers(authorIds);
		return DtoPage.of(page, hoax -> hoaxMapper.toDto(hoax,
				authors.getOrDefault(hoax.getUserId(), DtoUser.unknown(hoax.getUserId()))));
	}

	private Hoax findOrThrow(Long id) {
		return hoaxRepository.findById(id)
				.orElseThrow(() -> new BaseException(HoaxErrorType.HOAX_NOT_FOUND, String.valueOf(id)));
	}
}
