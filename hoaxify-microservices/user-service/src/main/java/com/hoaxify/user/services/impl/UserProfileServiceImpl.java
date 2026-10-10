package com.hoaxify.user.services.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.common.event.UserDeletedEvent;
import com.hoaxify.common.event.UserRegisteredEvent;
import com.hoaxify.common.web.dto.DtoPage;
import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.common.web.exception.CommonErrorType;
import com.hoaxify.user.configuration.CacheConfig;
import com.hoaxify.user.dto.DtoUser;
import com.hoaxify.user.dto.DtoUserUpdateIU;
import com.hoaxify.user.entities.UserProfile;
import com.hoaxify.user.exception.UserErrorType;
import com.hoaxify.user.mapper.UserMapper;
import com.hoaxify.user.repository.UserProfileRepository;
import com.hoaxify.user.services.IFileService;
import com.hoaxify.user.services.IUserProfileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements IUserProfileService {

	private final UserProfileRepository userProfileRepository;
	private final IFileService fileService;
	private final UserMapper userMapper;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * İdempotent tüketici (idempotent consumer): RabbitMQ "en az bir kez" (at-least-once) teslim eder,
	 * yani aynı mesaj iki kez gelebilir. İkinci gelişte hata fırlatırsak mesaj sonsuza dek yeniden denenir.
	 */
	@Override
	@Transactional
	public void createProfile(UserRegisteredEvent event) {
		if (userProfileRepository.existsById(event.userId())) {
			log.info("Profile {} already exists, skipping duplicate event", event.userId());
			return;
		}
		UserProfile profile = new UserProfile();
		profile.setId(event.userId());
		profile.setUsername(event.username());
		profile.setEmail(event.email());
		userProfileRepository.save(profile);
	}

	@Override
	@Transactional(readOnly = true)
	public DtoPage<DtoUser> getUsers(Pageable pageable, Long currentUserId) {
		Page<UserProfile> page = currentUserId == null
				? userProfileRepository.findAll(pageable)
				: userProfileRepository.findByIdNot(currentUserId, pageable);
		return DtoPage.of(page, userMapper::toDto);
	}

	/** İlk çağrıda veritabanından, sonrakilerde (10 dk boyunca) Redis'ten gelir */
	@Override
	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheConfig.USERS_CACHE, key = "#id")
	public DtoUser getUser(Long id) {
		return userMapper.toDto(findOrThrow(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<DtoUser> getUsersByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return userProfileRepository.findAllByIdIn(ids).stream().map(userMapper::toDto).toList();
	}

	/** @CachePut: metot her zaman çalışır ve dönen değer cache'e yazılır; eski (bayat) profil Redis'te kalmaz */
	@Override
	@Transactional
	@CachePut(cacheNames = CacheConfig.USERS_CACHE, key = "#id")
	public DtoUser updateUser(Long id, Long currentUserId, DtoUserUpdateIU request) {
		checkOwnership(id, currentUserId);
		UserProfile profile = findOrThrow(id);
		profile.setUsername(request.getUsername());
		if (request.getBio() != null) {
			profile.setBio(request.getBio());
		}
		if (request.getImage() != null) {
			String newImage = fileService.saveProfileImage(request.getImage());
			fileService.deleteProfileImage(profile.getImage());
			profile.setImage(newImage);
		}
		return userMapper.toDto(userProfileRepository.save(profile));
	}

	/**
	 * Monolith'te tek transaction'da cascade ile silinen hoax'lar ve token'lar artık başka servislerde.
	 * Profili silip UserDeletedEvent yayınlıyoruz; auth-service ve hoax-service kendi verilerini siler.
	 */
	@Override
	@Transactional
	@CacheEvict(cacheNames = CacheConfig.USERS_CACHE, key = "#id")
	public void deleteUser(Long id, Long currentUserId) {
		checkOwnership(id, currentUserId);
		UserProfile profile = findOrThrow(id);
		userProfileRepository.delete(profile);
		fileService.deleteProfileImage(profile.getImage());
		eventPublisher.publishEvent(new UserDeletedEvent(id));
	}

	/** Monolith'teki @PreAuthorize("#id == principal.id") kontrolünün karşılığı */
	private void checkOwnership(Long id, Long currentUserId) {
		if (!Objects.equals(id, currentUserId)) {
			throw new BaseException(CommonErrorType.FORBIDDEN);
		}
	}

	private UserProfile findOrThrow(Long id) {
		return userProfileRepository.findById(id)
				.orElseThrow(() -> new BaseException(UserErrorType.USER_NOT_FOUND, String.valueOf(id)));
	}
}
