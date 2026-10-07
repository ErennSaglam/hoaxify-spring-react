package com.hoaxify.ws.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hoaxify.ws.dto.DtoHoax;
import com.hoaxify.ws.dto.DtoHoaxIU;
import com.hoaxify.ws.dto.DtoTag;
import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.entities.Hoax;
import com.hoaxify.ws.entities.Tag;

/**
 * Tespit edilen senaryolar:
 *  toDto    : yazar ve etiketler alt mapper'lara devredilir, etiketler isme göre sıralanır | etiketsiz hoax
 *  toEntity : içerik trim'lenir, ilişkiler boş bırakılır
 */
@ExtendWith(MockitoExtension.class)
class HoaxMapperTest {

	@Mock
	private UserMapper userMapper;

	@Mock
	private TagMapper tagMapper;

	@InjectMocks
	private HoaxMapper hoaxMapper;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	@Test
	void toDto_success_shouldSortTagsByName() {
		// Arrange
		Hoax hoax = easyRandom.nextObject(Hoax.class);
		Tag spring = new Tag("spring");
		Tag java = new Tag("java");
		hoax.getTags().addAll(List.of(spring, java));
		DtoUser dtoUser = easyRandom.nextObject(DtoUser.class);
		DtoTag springDto = new DtoTag(2L, "spring");
		DtoTag javaDto = new DtoTag(1L, "java");

		when(userMapper.toDto(hoax.getUser())).thenReturn(dtoUser);
		when(tagMapper.toDto(spring)).thenReturn(springDto);
		when(tagMapper.toDto(java)).thenReturn(javaDto);

		// Act
		DtoHoax result = hoaxMapper.toDto(hoax);

		// Assert
		assertEquals(hoax.getId(), result.getId());
		assertEquals(hoax.getContent(), result.getContent());
		assertEquals(hoax.getCreatedAt(), result.getCreatedAt());
		assertEquals(dtoUser, result.getUser());
		assertEquals(List.of(javaDto, springDto), result.getTags());

		verify(userMapper, times(1)).toDto(hoax.getUser());
	}

	@Test
	void toDto_withoutTags_shouldReturnEmptyTagList() {
		// Arrange
		Hoax hoax = easyRandom.nextObject(Hoax.class);
		hoax.getTags().clear();
		when(userMapper.toDto(hoax.getUser())).thenReturn(easyRandom.nextObject(DtoUser.class));

		// Act
		DtoHoax result = hoaxMapper.toDto(hoax);

		// Assert
		assertTrue(result.getTags().isEmpty());
		verifyNoInteractions(tagMapper);
	}

	@Test
	void toEntity_shouldTrimContentAndLeaveRelationsEmpty() {
		// Arrange
		DtoHoaxIU request = new DtoHoaxIU("   hello world  ", List.of("java"));

		// Act
		Hoax result = hoaxMapper.toEntity(request);

		// Assert
		assertEquals("hello world", result.getContent());
		assertNull(result.getUser());
		assertTrue(result.getTags().isEmpty());
		verifyNoInteractions(userMapper, tagMapper);
	}
}
