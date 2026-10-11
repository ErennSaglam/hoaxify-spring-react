package com.hoaxify.hoax.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.hoaxify.common.web.dto.DtoPage;
import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.hoax.client.IUserQueryClient;
import com.hoaxify.hoax.dto.DtoHoax;
import com.hoaxify.hoax.dto.DtoHoaxIU;
import com.hoaxify.hoax.dto.DtoUser;
import com.hoaxify.hoax.entities.Hoax;
import com.hoaxify.hoax.entities.Tag;
import com.hoaxify.hoax.exception.HoaxErrorType;
import com.hoaxify.hoax.mapper.HoaxMapper;
import com.hoaxify.hoax.repository.HoaxRepository;
import com.hoaxify.hoax.services.ITagService;

/**
 * Tespit edilen senaryolar:
 *  createHoax      : success | yazar yok (gRPC NOT_FOUND -> 404, kaydedilmez)
 *  getHoaxes       : tag null -> findAll | tag dolu -> normalize + findByTagName
 *                    | yazarlar TEK toplu çağrıyla gelir (id'ler tekilleştirilir) | yazarı silinmiş hoax -> "[deleted]"
 *  getHoaxesOfUser : kullanıcı yok (hoax sorgulanmaz)
 *  getHoaxById     : success | yazar yok -> "[deleted]" | not found
 *  deleteHoax      : sahibi | sahibi değil
 *  deleteAllOfUser : hoax'ları var | hiç yok
 */
@ExtendWith(MockitoExtension.class)
class HoaxServiceImplTest {

	@Mock
	private HoaxRepository hoaxRepository;

	@Mock
	private ITagService tagService;

	@Mock
	private IUserQueryClient userQueryClient;

	@Mock
	private HoaxMapper hoaxMapper;

	@InjectMocks
	private HoaxServiceImpl hoaxService;

	private EasyRandom easyRandom;

	private Pageable pageable;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
		pageable = PageRequest.of(0, 10);
	}

	private static DtoUser user(Long id) {
		return new DtoUser(id, "user" + id, "u@mail.com", null, null);
	}

	private static DtoHoax dtoHoax(Long id, DtoUser author) {
		return new DtoHoax(id, "content", LocalDateTime.now(), author, List.of());
	}

	private Hoax hoaxOf(Long userId) {
		Hoax hoax = easyRandom.nextObject(Hoax.class);
		hoax.setUserId(userId);
		return hoax;
	}

	@Test
	void createHoax_success() {
		// Arrange
		DtoHoaxIU request = new DtoHoaxIU("Hello", List.of("java"));
		Hoax hoax = hoaxOf(1L);
		Set<Tag> tags = Set.of(new Tag("java"));
		when(userQueryClient.findUser(1L)).thenReturn(Optional.of(user(1L)));
		when(hoaxMapper.toEntity(request, 1L)).thenReturn(hoax);
		when(tagService.findOrCreate(request.getTags())).thenReturn(tags);
		when(hoaxRepository.save(hoax)).thenReturn(hoax);
		when(hoaxMapper.toDto(hoax, user(1L))).thenReturn(dtoHoax(10L, user(1L)));

		// Act
		DtoHoax result = hoaxService.createHoax(1L, request);

		// Assert
		assertEquals(10L, result.id());
		assertEquals("user1", result.user().username());
		assertEquals(tags, hoax.getTags());
		verify(hoaxRepository, times(1)).save(hoax);
	}

	@Test
	void createHoax_authorNotFound_shouldNotSave() {
		// Arrange
		when(userQueryClient.findUser(1L)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class,
				() -> hoaxService.createHoax(1L, new DtoHoaxIU("Hello", List.of())));

		// Assert
		assertEquals(HoaxErrorType.USER_NOT_FOUND, exception.getErrorType());
		verifyNoInteractions(hoaxRepository, tagService, hoaxMapper);
	}

	@Test
	void getHoaxes_shouldFetchAuthorsWithOneBatchCall() {
		// Arrange: iki hoax aynı yazarın, biri başka yazarın
		Hoax h1 = hoaxOf(1L);
		Hoax h2 = hoaxOf(1L);
		Hoax h3 = hoaxOf(2L);
		when(hoaxRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(h1, h2, h3), pageable, 3));
		when(userQueryClient.findUsers(Set.of(1L, 2L))).thenReturn(Map.of(1L, user(1L), 2L, user(2L)));
		when(hoaxMapper.toDto(any(Hoax.class), any(DtoUser.class))).thenReturn(dtoHoax(1L, user(1L)));

		// Act
		DtoPage<DtoHoax> result = hoaxService.getHoaxes(null, pageable);

		// Assert: 3 hoax, 2 farklı yazar, TEK gRPC çağrısı
		assertEquals(3, result.content().size());
		verify(userQueryClient, times(1)).findUsers(Set.of(1L, 2L));
		verify(userQueryClient, never()).findUser(any());
		verifyNoInteractions(tagService);
	}

	@Test
	void getHoaxes_withTag_shouldFilterByNormalizedTag() {
		// Arrange
		when(tagService.normalize("#Java")).thenReturn("java");
		when(hoaxRepository.findByTagName("java", pageable))
				.thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
		when(userQueryClient.findUsers(Set.of())).thenReturn(Map.of());

		// Act
		DtoPage<DtoHoax> result = hoaxService.getHoaxes("#Java", pageable);

		// Assert
		assertTrue(result.content().isEmpty());
		verify(hoaxRepository, never()).findAll(any(Pageable.class));
	}

	@Test
	void getHoaxes_authorDeleted_shouldUseUnknownUser() {
		// Arrange
		Hoax hoax = hoaxOf(9L);
		when(hoaxRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(hoax), pageable, 1));
		when(userQueryClient.findUsers(Set.of(9L))).thenReturn(Map.of());
		when(hoaxMapper.toDto(hoax, DtoUser.unknown(9L))).thenReturn(dtoHoax(1L, DtoUser.unknown(9L)));

		// Act
		DtoPage<DtoHoax> result = hoaxService.getHoaxes(null, pageable);

		// Assert
		assertEquals("[deleted]", result.content().get(0).user().username());
	}

	@Test
	void getHoaxesOfUser_userNotFound_shouldNotQueryHoaxes() {
		// Arrange
		when(userQueryClient.findUser(5L)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class,
				() -> hoaxService.getHoaxesOfUser(5L, pageable));

		// Assert
		assertEquals(HoaxErrorType.USER_NOT_FOUND, exception.getErrorType());
		verifyNoInteractions(hoaxRepository);
	}

	@Test
	void getHoaxById_success() {
		// Arrange
		Hoax hoax = hoaxOf(1L);
		when(hoaxRepository.findById(10L)).thenReturn(Optional.of(hoax));
		when(userQueryClient.findUser(1L)).thenReturn(Optional.of(user(1L)));
		when(hoaxMapper.toDto(hoax, user(1L))).thenReturn(dtoHoax(10L, user(1L)));

		// Act & Assert
		assertEquals(10L, hoaxService.getHoaxById(10L).id());
	}

	@Test
	void getHoaxById_authorMissing_shouldUseUnknownUser() {
		// Arrange
		Hoax hoax = hoaxOf(1L);
		when(hoaxRepository.findById(10L)).thenReturn(Optional.of(hoax));
		when(userQueryClient.findUser(1L)).thenReturn(Optional.empty());
		when(hoaxMapper.toDto(hoax, DtoUser.unknown(1L))).thenReturn(dtoHoax(10L, DtoUser.unknown(1L)));

		// Act & Assert
		assertEquals("[deleted]", hoaxService.getHoaxById(10L).user().username());
	}

	@Test
	void getHoaxById_notFound() {
		// Arrange
		when(hoaxRepository.findById(10L)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> hoaxService.getHoaxById(10L));

		// Assert
		assertEquals(HoaxErrorType.HOAX_NOT_FOUND, exception.getErrorType());
		verifyNoInteractions(userQueryClient);
	}

	@Test
	void deleteHoax_owner_shouldDelete() {
		// Arrange
		Hoax hoax = hoaxOf(1L);
		when(hoaxRepository.findById(10L)).thenReturn(Optional.of(hoax));

		// Act
		hoaxService.deleteHoax(10L, 1L);

		// Assert
		verify(hoaxRepository, times(1)).delete(hoax);
	}

	@Test
	void deleteHoax_notOwner_shouldThrowForbidden() {
		// Arrange
		when(hoaxRepository.findById(10L)).thenReturn(Optional.of(hoaxOf(1L)));

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> hoaxService.deleteHoax(10L, 2L));

		// Assert
		assertEquals(HoaxErrorType.HOAX_DELETE_FORBIDDEN, exception.getErrorType());
		verify(hoaxRepository, never()).delete(any());
	}

	@Test
	void deleteAllOfUser_success() {
		// Arrange
		List<Hoax> hoaxes = List.of(hoaxOf(3L), hoaxOf(3L));
		when(hoaxRepository.findAllByUserId(3L)).thenReturn(hoaxes);

		// Act
		hoaxService.deleteAllOfUser(3L);

		// Assert
		verify(hoaxRepository, times(1)).deleteAll(hoaxes);
	}

	@Test
	void deleteAllOfUser_noHoaxes() {
		// Arrange
		when(hoaxRepository.findAllByUserId(3L)).thenReturn(Collections.emptyList());

		// Act
		hoaxService.deleteAllOfUser(3L);

		// Assert
		verify(hoaxRepository, times(1)).deleteAll(Collections.emptyList());
	}
}
