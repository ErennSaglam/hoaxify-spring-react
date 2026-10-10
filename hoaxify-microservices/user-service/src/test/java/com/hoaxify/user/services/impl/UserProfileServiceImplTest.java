package com.hoaxify.user.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.hoaxify.common.event.UserDeletedEvent;
import com.hoaxify.common.event.UserRegisteredEvent;
import com.hoaxify.common.web.dto.DtoPage;
import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.common.web.exception.CommonErrorType;
import com.hoaxify.user.dto.DtoUser;
import com.hoaxify.user.dto.DtoUserUpdateIU;
import com.hoaxify.user.entities.UserProfile;
import com.hoaxify.user.exception.UserErrorType;
import com.hoaxify.user.mapper.UserMapper;
import com.hoaxify.user.repository.UserProfileRepository;
import com.hoaxify.user.services.IFileService;

/**
 * Tespit edilen senaryolar:
 *  createProfile : yeni olay -> profil | aynı olay tekrar geldi (idempotent, kaydetmez)
 *  getUsers      : anonim -> findAll | giriş yapmış -> findByIdNot | boş sayfa
 *  getUser       : success | not found
 *  getUsersByIds : success | boş/null id listesi (DB'ye gidilmez)
 *  updateUser    : başkasının profili (403, DB'ye gidilmez) | sadece username | resim (önce kaydet sonra sil) | bio null
 *  deleteUser    : başkasının profili | success (olay yayınlanır) | not found (olay yayınlanmaz)
 *
 * Not: record'lar (DtoUser, olaylar) EasyRandom ile üretilemez (final alanlar), elle oluşturuluyor.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

	@Mock
	private UserProfileRepository userProfileRepository;

	@Mock
	private IFileService fileService;

	@Mock
	private UserMapper userMapper;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private UserProfileServiceImpl userProfileService;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	private static DtoUser dto(Long id) {
		return new DtoUser(id, "user" + id, "user" + id + "@mail.com", null, null);
	}

	// ------------------------------------------------------------------ createProfile

	@Test
	void createProfile_success() {
		// Arrange
		UserRegisteredEvent event = new UserRegisteredEvent(7L, "alice", "alice@mail.com", "token", "tr");
		when(userProfileRepository.existsById(7L)).thenReturn(false);

		// Act
		userProfileService.createProfile(event);

		// Assert
		ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
		verify(userProfileRepository, times(1)).save(captor.capture());
		assertEquals(7L, captor.getValue().getId());
		assertEquals("alice", captor.getValue().getUsername());
		assertEquals("alice@mail.com", captor.getValue().getEmail());
	}

	@Test
	void createProfile_duplicateEvent_shouldNotSave() {
		// Arrange
		UserRegisteredEvent event = new UserRegisteredEvent(7L, "alice", "alice@mail.com", "token", "tr");
		when(userProfileRepository.existsById(7L)).thenReturn(true);

		// Act
		userProfileService.createProfile(event);

		// Assert
		verify(userProfileRepository, never()).save(any());
	}

	// ------------------------------------------------------------------ getUsers / getUser

	@Test
	void getUsers_anonymous_shouldUseFindAll() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 3);
		UserProfile profile = easyRandom.nextObject(UserProfile.class);
		when(userProfileRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(profile), pageable, 1));
		when(userMapper.toDto(profile)).thenReturn(dto(1L));

		// Act
		DtoPage<DtoUser> result = userProfileService.getUsers(pageable, null);

		// Assert
		assertEquals(1, result.content().size());
		verify(userProfileRepository, never()).findByIdNot(any(), any());
	}

	@Test
	void getUsers_loggedIn_shouldExcludeCurrentUser() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 3);
		when(userProfileRepository.findByIdNot(5L, pageable))
				.thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

		// Act
		DtoPage<DtoUser> result = userProfileService.getUsers(pageable, 5L);

		// Assert
		assertTrue(result.content().isEmpty());
		verify(userProfileRepository, times(1)).findByIdNot(5L, pageable);
		verifyNoInteractions(userMapper);
	}

	@Test
	void getUser_success() {
		// Arrange
		UserProfile profile = easyRandom.nextObject(UserProfile.class);
		when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
		when(userMapper.toDto(profile)).thenReturn(dto(1L));

		// Act & Assert
		assertEquals(dto(1L), userProfileService.getUser(1L));
	}

	@Test
	void getUser_notFound() {
		// Arrange
		when(userProfileRepository.findById(99L)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> userProfileService.getUser(99L));

		// Assert
		assertEquals(UserErrorType.USER_NOT_FOUND, exception.getErrorType());
		assertEquals("99", exception.getArgs()[0]);
	}

	@Test
	void getUsersByIds_success() {
		// Arrange
		UserProfile p1 = easyRandom.nextObject(UserProfile.class);
		UserProfile p2 = easyRandom.nextObject(UserProfile.class);
		when(userProfileRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(p1, p2));
		when(userMapper.toDto(p1)).thenReturn(dto(1L));
		when(userMapper.toDto(p2)).thenReturn(dto(2L));

		// Act
		List<DtoUser> result = userProfileService.getUsersByIds(List.of(1L, 2L));

		// Assert
		assertEquals(List.of(dto(1L), dto(2L)), result);
	}

	@Test
	void getUsersByIds_emptyOrNull_shouldNotQueryDatabase() {
		assertTrue(userProfileService.getUsersByIds(List.of()).isEmpty());
		assertTrue(userProfileService.getUsersByIds(null).isEmpty());
		verifyNoInteractions(userProfileRepository);
	}

	// ------------------------------------------------------------------ updateUser

	@Test
	void updateUser_otherUser_shouldThrowForbiddenWithoutQuery() {
		// Arrange
		DtoUserUpdateIU request = easyRandom.nextObject(DtoUserUpdateIU.class);

		// Act
		BaseException exception = assertThrows(BaseException.class,
				() -> userProfileService.updateUser(1L, 2L, request));

		// Assert
		assertEquals(CommonErrorType.FORBIDDEN, exception.getErrorType());
		verifyNoInteractions(userProfileRepository, fileService);
	}

	@Test
	void updateUser_onlyUsername_shouldKeepBioAndImage() {
		// Arrange
		UserProfile profile = easyRandom.nextObject(UserProfile.class);
		String oldBio = profile.getBio();
		String oldImage = profile.getImage();
		when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
		when(userProfileRepository.save(profile)).thenReturn(profile);
		when(userMapper.toDto(profile)).thenReturn(dto(1L));

		// Act
		userProfileService.updateUser(1L, 1L, new DtoUserUpdateIU("newname", null, null));

		// Assert
		assertEquals("newname", profile.getUsername());
		assertEquals(oldBio, profile.getBio());
		assertEquals(oldImage, profile.getImage());
		verifyNoInteractions(fileService);
	}

	@Test
	void updateUser_withImageAndBio_shouldSaveNewImageBeforeDeletingOld() {
		// Arrange
		UserProfile profile = easyRandom.nextObject(UserProfile.class);
		profile.setImage("old.png");
		when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
		when(fileService.saveProfileImage("data:image/png;base64,AAA")).thenReturn("new.png");
		when(userProfileRepository.save(profile)).thenReturn(profile);
		when(userMapper.toDto(profile)).thenReturn(dto(1L));

		// Act
		userProfileService.updateUser(1L, 1L, new DtoUserUpdateIU("name", "data:image/png;base64,AAA", "bio"));

		// Assert
		assertEquals("new.png", profile.getImage());
		assertEquals("bio", profile.getBio());
		InOrder inOrder = inOrder(fileService);
		inOrder.verify(fileService).saveProfileImage("data:image/png;base64,AAA");
		inOrder.verify(fileService).deleteProfileImage("old.png");
	}

	// ------------------------------------------------------------------ deleteUser

	@Test
	void deleteUser_otherUser_shouldThrowForbidden() {
		// Act
		BaseException exception = assertThrows(BaseException.class, () -> userProfileService.deleteUser(1L, 2L));

		// Assert
		assertEquals(CommonErrorType.FORBIDDEN, exception.getErrorType());
		verifyNoInteractions(userProfileRepository, eventPublisher);
	}

	@Test
	void deleteUser_success_shouldPublishUserDeletedEvent() {
		// Arrange
		UserProfile profile = easyRandom.nextObject(UserProfile.class);
		profile.setImage("avatar.png");
		when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

		// Act
		userProfileService.deleteUser(1L, 1L);

		// Assert
		verify(userProfileRepository, times(1)).delete(profile);
		verify(fileService, times(1)).deleteProfileImage("avatar.png");
		verify(eventPublisher, times(1)).publishEvent(new UserDeletedEvent(1L));
	}

	@Test
	void deleteUser_notFound_shouldNotPublishEvent() {
		// Arrange
		when(userProfileRepository.findById(1L)).thenReturn(Optional.empty());

		// Act
		assertThrows(BaseException.class, () -> userProfileService.deleteUser(1L, 1L));

		// Assert
		verifyNoInteractions(eventPublisher, fileService);
	}
}
