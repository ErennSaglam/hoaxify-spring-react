package com.hoaxify.ws.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hoaxify.ws.entities.Hoax;
import com.hoaxify.ws.entities.Tag;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.repository.HoaxRepository;
import com.hoaxify.ws.repository.TagRepository;
import com.hoaxify.ws.repository.UserRepository;

/**
 * Tespit edilen senaryolar:
 *  run : veritabanı dolu (erken return, hiçbir şey yazılmaz) | veritabanı boş (25 kullanıcı + 3 etiket + 4 hoax)
 */
@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private TagRepository tagRepository;

	@Mock
	private HoaxRepository hoaxRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private DataInitializer dataInitializer;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	@Test
	void run_databaseNotEmpty_shouldReturnEarly() {
		// Arrange
		when(userRepository.count()).thenReturn(10L);

		// Act
		dataInitializer.run();

		// Assert
		verify(userRepository, times(1)).count();
		verify(userRepository, times(0)).save(any());
		verifyNoInteractions(tagRepository, hoaxRepository, passwordEncoder);
	}

	@Test
	@SuppressWarnings("unchecked")
	void run_databaseEmpty_shouldCreateSampleData() {
		// Arrange
		List<User> savedUsers = IntStream.range(0, 25).mapToObj(i -> easyRandom.nextObject(User.class)).toList();
		when(userRepository.count()).thenReturn(0L);
		when(passwordEncoder.encode("P4ssword")).thenReturn("hashed");
		when(tagRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
		when(userRepository.findAll()).thenReturn(savedUsers);

		// Act
		dataInitializer.run();

		// Assert
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository, times(25)).save(userCaptor.capture());
		User firstUser = userCaptor.getAllValues().get(0);
		assertEquals("user1", firstUser.getUsername());
		assertEquals("user1@mail.com", firstUser.getEmail());
		assertEquals("hashed", firstUser.getPassword());
		assertTrue(firstUser.isActive());
		assertEquals(firstUser, firstUser.getProfile().getUser());

		// Şifre 25 kez değil, bir kez hash'lenir (BCrypt yavaştır)
		verify(passwordEncoder, times(1)).encode("P4ssword");

		ArgumentCaptor<List<Tag>> tagCaptor = ArgumentCaptor.forClass(List.class);
		verify(tagRepository, times(1)).saveAll(tagCaptor.capture());
		assertEquals(List.of("java", "spring", "react"), tagCaptor.getValue().stream().map(Tag::getName).toList());

		verify(hoaxRepository, times(4)).save(any(Hoax.class));
	}
}
