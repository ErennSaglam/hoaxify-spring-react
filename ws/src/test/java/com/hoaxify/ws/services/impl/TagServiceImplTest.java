package com.hoaxify.ws.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hoaxify.ws.dto.DtoTag;
import com.hoaxify.ws.entities.Tag;
import com.hoaxify.ws.mapper.TagMapper;
import com.hoaxify.ws.repository.TagRepository;

/**
 * Tespit edilen senaryolar:
 *  getTags     : success (sıra korunur) | boş liste | repository hata
 *  findOrCreate: null (erken return) | boş liste (erken return) | hepsi mevcut | karışık | hepsi yeni
 *  normalize   : # kaldırma, trim, küçük harf, Türkçe locale tuzağı
 */
@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

	@Mock
	private TagRepository tagRepository;

	@Mock
	private TagMapper tagMapper;

	@InjectMocks
	private TagServiceImpl tagService;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	private static Set<String> names(Set<Tag> tags) {
		return tags.stream().map(Tag::getName).collect(Collectors.toSet());
	}

	// ------------------------------------------------------------------ getTags

	@Test
	void getTags_success() {
		// Arrange
		Tag spring = easyRandom.nextObject(Tag.class);
		Tag java = easyRandom.nextObject(Tag.class);
		DtoTag springDto = easyRandom.nextObject(DtoTag.class);
		DtoTag javaDto = easyRandom.nextObject(DtoTag.class);

		when(tagRepository.findAllOrderByUsage()).thenReturn(List.of(spring, java));
		when(tagMapper.toDto(spring)).thenReturn(springDto);
		when(tagMapper.toDto(java)).thenReturn(javaDto);

		// Act
		List<DtoTag> result = tagService.getTags();

		// Assert: repository'nin sıralaması (kullanım sayısı) bozulmamalı
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(springDto, result.get(0));
		assertEquals(javaDto, result.get(1));
		verify(tagRepository, times(1)).findAllOrderByUsage();
	}

	@Test
	void getTags_emptyList() {
		// Arrange
		when(tagRepository.findAllOrderByUsage()).thenReturn(Collections.emptyList());

		// Act
		List<DtoTag> result = tagService.getTags();

		// Assert
		assertNotNull(result);
		assertTrue(result.isEmpty());
		verifyNoInteractions(tagMapper);
	}

	@Test
	void getTags_repositoryException() {
		// Arrange
		when(tagRepository.findAllOrderByUsage()).thenThrow(new RuntimeException("DB error"));

		// Act & Assert
		assertThrows(RuntimeException.class, () -> tagService.getTags());
	}

	// ------------------------------------------------------------------ findOrCreate

	@Test
	void findOrCreate_nullNames_shouldReturnEmptyWithoutRepository() {
		// Act
		Set<Tag> result = tagService.findOrCreate(null);

		// Assert
		assertNotNull(result);
		assertTrue(result.isEmpty());
		verifyNoInteractions(tagRepository);
	}

	@Test
	void findOrCreate_emptyList_shouldReturnEmptyWithoutRepository() {
		// Act
		Set<Tag> result = tagService.findOrCreate(Collections.emptyList());

		// Assert
		assertTrue(result.isEmpty());
		verifyNoInteractions(tagRepository);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findOrCreate_allExisting_shouldSaveNothingNew() {
		// Arrange
		Tag java = new Tag("java");
		when(tagRepository.findByNameIn(Set.of("java"))).thenReturn(List.of(java));
		when(tagRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

		// Act
		Set<Tag> result = tagService.findOrCreate(List.of("#Java", "java"));

		// Assert
		assertEquals(Set.of("java"), names(result));
		assertTrue(result.contains(java));

		ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
		verify(tagRepository, times(1)).saveAll(captor.capture());
		assertTrue(captor.getValue().isEmpty());
	}

	@Test
	@SuppressWarnings("unchecked")
	void findOrCreate_mixed_shouldSaveOnlyMissingTags() {
		// Arrange
		Tag java = new Tag("java");
		Tag spring = new Tag("spring");
		when(tagRepository.findByNameIn(Set.of("java", "spring"))).thenReturn(List.of(java));
		when(tagRepository.saveAll(anyList())).thenReturn(List.of(spring));

		// Act
		Set<Tag> result = tagService.findOrCreate(List.of("java", "#Spring", "SPRING"));

		// Assert
		assertEquals(Set.of("java", "spring"), names(result));

		ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
		verify(tagRepository, times(1)).saveAll(captor.capture());
		assertEquals(1, captor.getValue().size());
		assertEquals("spring", captor.getValue().get(0).getName());
	}

	@Test
	@SuppressWarnings("unchecked")
	void findOrCreate_allNew_shouldSaveAll() {
		// Arrange
		when(tagRepository.findByNameIn(Set.of("react", "vite"))).thenReturn(Collections.emptyList());
		when(tagRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		Set<Tag> result = tagService.findOrCreate(List.of("React", "vite"));

		// Assert
		assertEquals(Set.of("react", "vite"), names(result));

		ArgumentCaptor<List<Tag>> captor = ArgumentCaptor.forClass(List.class);
		verify(tagRepository, times(1)).saveAll(captor.capture());
		assertEquals(2, captor.getValue().size());
	}

	@Test
	void findOrCreate_repositoryException_shouldNotSave() {
		// Arrange
		when(tagRepository.findByNameIn(Set.of("java"))).thenThrow(new RuntimeException("DB error"));

		// Act & Assert
		assertThrows(RuntimeException.class, () -> tagService.findOrCreate(List.of("java")));
		verify(tagRepository, never()).saveAll(anyList());
	}

	// ------------------------------------------------------------------ normalize

	@ParameterizedTest(name = "normalize(\"{0}\") = \"{1}\"")
	@CsvSource({
			"java, java",
			"'#Java', java",
			"'  Spring ', spring",
			"'#REACT', react",
			// Locale.ROOT: Türkçe locale ile "I".toLowerCase() "ı" olurdu
			"ISTANBUL, istanbul"
	})
	void normalize_success(String input, String expected) {
		// Act
		String result = tagService.normalize(input);

		// Assert
		assertEquals(expected, result);
	}
}
