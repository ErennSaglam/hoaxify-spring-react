package com.hoaxify.ws.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.hoaxify.ws.dto.DtoHoax;
import com.hoaxify.ws.dto.DtoHoaxIU;
import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.entities.Hoax;
import com.hoaxify.ws.entities.Tag;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.mapper.HoaxMapper;
import com.hoaxify.ws.repository.HoaxRepository;
import com.hoaxify.ws.repository.UserRepository;
import com.hoaxify.ws.services.ITagService;

/**
 * Tespit edilen senaryolar:
 *  createHoax      : success (yazar + etiketler bağlanır) | yazar bulunamadı | etiket listesi boş | repository hata
 *  getHoaxes       : tag null -> findAll | tag boşluk -> findAll | tag dolu -> normalize + findByTagName | boş sonuç
 *  getHoaxesOfUser : kullanıcı yok (erken hata) | success | boş sonuç
 *  getHoaxById     : success | not found
 *  deleteHoax      : sahibi siliyor | sahibi değil | currentUserId null | not found
 */
@ExtendWith(MockitoExtension.class)
class HoaxServiceImplTest {

	@Mock
	private HoaxRepository hoaxRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ITagService tagService;

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

	// ------------------------------------------------------------------ createHoax

	@Test
	void createHoax_success() {
		// Arrange
		Long authorId = 1L;
		User author = easyRandom.nextObject(User.class);
		author.setId(authorId);
		DtoHoaxIU request = new DtoHoaxIU("Hello", List.of("java", "#spring"));
		Hoax hoax = new Hoax();
		Set<Tag> tags = Set.of(new Tag("java"), new Tag("spring"));
		DtoHoax expected = easyRandom.nextObject(DtoHoax.class);

		when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
		when(hoaxMapper.toEntity(request)).thenReturn(hoax);
		when(tagService.findOrCreate(request.getTags())).thenReturn(tags);
		when(hoaxRepository.save(hoax)).thenReturn(hoax);
		when(hoaxMapper.toDto(hoax)).thenReturn(expected);

		// Act
		DtoHoax result = hoaxService.createHoax(authorId, request);

		// Assert
		assertEquals(expected, result);

		ArgumentCaptor<Hoax> captor = ArgumentCaptor.forClass(Hoax.class);
		verify(hoaxRepository, times(1)).save(captor.capture());
		Hoax saved = captor.getValue();
		assertSame(author, saved.getUser());
		assertEquals(tags, saved.getTags());

		verify(tagService, times(1)).findOrCreate(request.getTags());
	}

	@Test
	void createHoax_authorNotFound_shouldNotSave() {
		// Arrange
		Long authorId = 1L;
		DtoHoaxIU request = easyRandom.nextObject(DtoHoaxIU.class);
		when(userRepository.findById(authorId)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> hoaxService.createHoax(authorId, request));

		// Assert
		assertEquals(MessageType.USER_NOT_FOUND, exception.getMessageType());
		verifyNoInteractions(tagService, hoaxMapper);
		verify(hoaxRepository, never()).save(any());
	}

	@Test
	void createHoax_emptyTagList() {
		// Arrange
		Long authorId = 1L;
		User author = easyRandom.nextObject(User.class);
		DtoHoaxIU request = new DtoHoaxIU("No tags here", Collections.emptyList());
		Hoax hoax = new Hoax();

		when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
		when(hoaxMapper.toEntity(request)).thenReturn(hoax);
		when(tagService.findOrCreate(Collections.emptyList())).thenReturn(Collections.emptySet());
		when(hoaxRepository.save(hoax)).thenReturn(hoax);
		when(hoaxMapper.toDto(hoax)).thenReturn(easyRandom.nextObject(DtoHoax.class));

		// Act
		hoaxService.createHoax(authorId, request);

		// Assert
		assertTrue(hoax.getTags().isEmpty());
		verify(hoaxRepository, times(1)).save(hoax);
	}

	@Test
	void createHoax_repositoryException() {
		// Arrange
		Long authorId = 1L;
		User author = easyRandom.nextObject(User.class);
		DtoHoaxIU request = easyRandom.nextObject(DtoHoaxIU.class);
		Hoax hoax = new Hoax();

		when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
		when(hoaxMapper.toEntity(request)).thenReturn(hoax);
		when(tagService.findOrCreate(request.getTags())).thenReturn(Collections.emptySet());
		when(hoaxRepository.save(hoax)).thenThrow(new RuntimeException("DB error"));

		// Act & Assert
		assertThrows(RuntimeException.class, () -> hoaxService.createHoax(authorId, request));
		verify(hoaxMapper, never()).toDto(any());
	}

	// ------------------------------------------------------------------ getHoaxes

	@Test
	void getHoaxes_tagNull_shouldReturnAllHoaxes() {
		// Arrange
		Hoax hoax1 = easyRandom.nextObject(Hoax.class);
		Hoax hoax2 = easyRandom.nextObject(Hoax.class);
		DtoHoax dto1 = easyRandom.nextObject(DtoHoax.class);
		DtoHoax dto2 = easyRandom.nextObject(DtoHoax.class);

		when(hoaxRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(hoax1, hoax2), pageable, 2));
		when(hoaxMapper.toDto(hoax1)).thenReturn(dto1);
		when(hoaxMapper.toDto(hoax2)).thenReturn(dto2);

		// Act
		DtoPage<DtoHoax> result = hoaxService.getHoaxes(null, pageable);

		// Assert
		assertNotNull(result);
		assertEquals(2, result.getContent().size());
		assertEquals(dto1.getId(), result.getContent().get(0).getId());
		assertEquals(dto2.getId(), result.getContent().get(1).getId());

		verify(hoaxRepository, times(1)).findAll(pageable);
		verify(hoaxRepository, never()).findByTagName(any(), any());
		verifyNoInteractions(tagService);
	}

	@Test
	void getHoaxes_tagBlank_shouldReturnAllHoaxes() {
		// Arrange
		when(hoaxRepository.findAll(pageable)).thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

		// Act
		hoaxService.getHoaxes("   ", pageable);

		// Assert
		verify(hoaxRepository, times(1)).findAll(pageable);
		verifyNoInteractions(tagService);
	}

	@Test
	void getHoaxes_withTag_shouldFilterByNormalizedTag() {
		// Arrange
		Hoax hoax = easyRandom.nextObject(Hoax.class);
		DtoHoax dto = easyRandom.nextObject(DtoHoax.class);

		when(tagService.normalize("#Java")).thenReturn("java");
		when(hoaxRepository.findByTagName("java", pageable)).thenReturn(new PageImpl<>(List.of(hoax), pageable, 1));
		when(hoaxMapper.toDto(hoax)).thenReturn(dto);

		// Act
		DtoPage<DtoHoax> result = hoaxService.getHoaxes("#Java", pageable);

		// Assert
		assertEquals(1, result.getContent().size());
		assertEquals(dto, result.getContent().get(0));

		verify(tagService, times(1)).normalize("#Java");
		verify(hoaxRepository, times(1)).findByTagName("java", pageable);
		verify(hoaxRepository, never()).findAll(any(Pageable.class));
	}

	@Test
	void getHoaxes_emptyList() {
		// Arrange
		when(hoaxRepository.findAll(pageable)).thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

		// Act
		DtoPage<DtoHoax> result = hoaxService.getHoaxes(null, pageable);

		// Assert
		assertNotNull(result);
		assertTrue(result.getContent().isEmpty());
		verifyNoInteractions(hoaxMapper);
	}

	// ------------------------------------------------------------------ getHoaxesOfUser

	@Test
	void getHoaxesOfUser_success() {
		// Arrange
		Long userId = 7L;
		Hoax hoax = easyRandom.nextObject(Hoax.class);
		DtoHoax dto = easyRandom.nextObject(DtoHoax.class);

		when(userRepository.existsById(userId)).thenReturn(true);
		when(hoaxRepository.findByUserId(userId, pageable)).thenReturn(new PageImpl<>(List.of(hoax), pageable, 1));
		when(hoaxMapper.toDto(hoax)).thenReturn(dto);

		// Act
		DtoPage<DtoHoax> result = hoaxService.getHoaxesOfUser(userId, pageable);

		// Assert
		assertEquals(1, result.getContent().size());
		assertEquals(dto, result.getContent().get(0));
		verify(hoaxRepository, times(1)).findByUserId(userId, pageable);
	}

	@Test
	void getHoaxesOfUser_userNotFound_shouldNotQueryHoaxes() {
		// Arrange
		Long userId = 7L;
		when(userRepository.existsById(userId)).thenReturn(false);

		// Act
		BaseException exception = assertThrows(BaseException.class,
				() -> hoaxService.getHoaxesOfUser(userId, pageable));

		// Assert
		assertEquals(MessageType.USER_NOT_FOUND, exception.getMessageType());
		assertEquals("7", exception.getArgs()[0]);
		verifyNoInteractions(hoaxRepository);
	}

	@Test
	void getHoaxesOfUser_emptyList() {
		// Arrange
		Long userId = 7L;
		when(userRepository.existsById(userId)).thenReturn(true);
		when(hoaxRepository.findByUserId(userId, pageable))
				.thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

		// Act
		DtoPage<DtoHoax> result = hoaxService.getHoaxesOfUser(userId, pageable);

		// Assert
		assertTrue(result.getContent().isEmpty());
		assertEquals(0, result.getTotalElements());
	}

	// ------------------------------------------------------------------ getHoaxById

	@Test
	void getHoaxById_success() {
		// Arrange
		Long hoaxId = 5L;
		Hoax hoax = easyRandom.nextObject(Hoax.class);
		DtoHoax expected = easyRandom.nextObject(DtoHoax.class);
		expected.setId(hoaxId);

		when(hoaxRepository.findById(hoaxId)).thenReturn(Optional.of(hoax));
		when(hoaxMapper.toDto(hoax)).thenReturn(expected);

		// Act
		DtoHoax result = hoaxService.getHoaxById(hoaxId);

		// Assert
		assertEquals(hoaxId, result.getId());
		assertEquals(expected.getContent(), result.getContent());
		verify(hoaxRepository, times(1)).findById(hoaxId);
	}

	@Test
	void getHoaxById_notFound() {
		// Arrange
		Long hoaxId = 5L;
		when(hoaxRepository.findById(hoaxId)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> hoaxService.getHoaxById(hoaxId));

		// Assert
		assertEquals(MessageType.HOAX_NOT_FOUND, exception.getMessageType());
		verifyNoInteractions(hoaxMapper);
	}

	// ------------------------------------------------------------------ deleteHoax

	@Test
	void deleteHoax_owner_shouldDelete() {
		// Arrange
		Long hoaxId = 5L;
		Long ownerId = 1L;
		Hoax hoax = easyRandom.nextObject(Hoax.class);
		hoax.getUser().setId(ownerId);

		when(hoaxRepository.findById(hoaxId)).thenReturn(Optional.of(hoax));

		// Act
		hoaxService.deleteHoax(hoaxId, ownerId);

		// Assert
		verify(hoaxRepository, times(1)).delete(hoax);
	}

	@Test
	void deleteHoax_notOwner_shouldThrowForbidden() {
		// Arrange
		Long hoaxId = 5L;
		Hoax hoax = easyRandom.nextObject(Hoax.class);
		hoax.getUser().setId(1L);

		when(hoaxRepository.findById(hoaxId)).thenReturn(Optional.of(hoax));

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> hoaxService.deleteHoax(hoaxId, 2L));

		// Assert
		assertEquals(MessageType.HOAX_DELETE_FORBIDDEN, exception.getMessageType());
		verify(hoaxRepository, never()).delete(any());
	}

	@Test
	void deleteHoax_currentUserNull_shouldThrowForbidden() {
		// Arrange
		Long hoaxId = 5L;
		Hoax hoax = easyRandom.nextObject(Hoax.class);

		when(hoaxRepository.findById(hoaxId)).thenReturn(Optional.of(hoax));

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> hoaxService.deleteHoax(hoaxId, null));

		// Assert
		assertEquals(MessageType.HOAX_DELETE_FORBIDDEN, exception.getMessageType());
		verify(hoaxRepository, never()).delete(any());
	}

	@Test
	void deleteHoax_notFound() {
		// Arrange
		Long hoaxId = 5L;
		when(hoaxRepository.findById(hoaxId)).thenReturn(Optional.empty());

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> hoaxService.deleteHoax(hoaxId, 1L));

		// Assert
		assertEquals(MessageType.HOAX_NOT_FOUND, exception.getMessageType());
		verify(hoaxRepository, never()).delete(any());
	}
}
