package com.hoaxify.ws.controller.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.hoaxify.ws.dto.DtoHoax;
import com.hoaxify.ws.dto.DtoHoaxIU;
import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.security.CurrentUser;
import com.hoaxify.ws.services.IHoaxService;

@ExtendWith(MockitoExtension.class)
class HoaxControllerImplTest {

	@Mock
	private IHoaxService hoaxService;

	@InjectMocks
	private HoaxControllerImpl hoaxController;

	private EasyRandom easyRandom;

	private CurrentUser currentUser;

	private Pageable pageable;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
		User user = easyRandom.nextObject(User.class);
		user.setId(1L);
		currentUser = new CurrentUser(user);
		pageable = PageRequest.of(0, 10);
		RequestContextHolder.setRequestAttributes(
				new ServletRequestAttributes(new MockHttpServletRequest("POST", "/api/v1/hoaxes")));
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void createHoax_success_shouldUseCurrentUserAsAuthor() {
		// Arrange
		DtoHoaxIU request = easyRandom.nextObject(DtoHoaxIU.class);
		DtoHoax created = easyRandom.nextObject(DtoHoax.class);
		created.setId(10L);
		when(hoaxService.createHoax(1L, request)).thenReturn(created);

		// Act
		ResponseEntity<DtoHoax> result = hoaxController.createHoax(request, currentUser);

		// Assert
		assertEquals(HttpStatus.CREATED, result.getStatusCode());
		assertEquals(created, result.getBody());
		assertEquals("http://localhost/api/v1/hoaxes/10", result.getHeaders().getLocation().toString());
		verify(hoaxService, times(1)).createHoax(1L, request);
	}

	@Test
	void getHoaxes_withTag() {
		// Arrange
		DtoPage<DtoHoax> page = new DtoPage<>(List.of(easyRandom.nextObject(DtoHoax.class)), 0, 10, 1, 1, true, true);
		when(hoaxService.getHoaxes("java", pageable)).thenReturn(page);

		// Act
		ResponseEntity<DtoPage<DtoHoax>> result = hoaxController.getHoaxes("java", pageable);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(1, result.getBody().getContent().size());
		verify(hoaxService, times(1)).getHoaxes("java", pageable);
	}

	@Test
	void getHoaxes_withoutTag_emptyList() {
		// Arrange
		DtoPage<DtoHoax> page = new DtoPage<>(Collections.emptyList(), 0, 10, 0, 0, true, true);
		when(hoaxService.getHoaxes(null, pageable)).thenReturn(page);

		// Act
		ResponseEntity<DtoPage<DtoHoax>> result = hoaxController.getHoaxes(null, pageable);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertTrue(result.getBody().getContent().isEmpty());
	}

	@Test
	void getHoaxesOfUser_success() {
		// Arrange
		Long userId = 3L;
		DtoPage<DtoHoax> page = new DtoPage<>(List.of(easyRandom.nextObject(DtoHoax.class)), 0, 10, 1, 1, true, true);
		when(hoaxService.getHoaxesOfUser(userId, pageable)).thenReturn(page);

		// Act
		ResponseEntity<DtoPage<DtoHoax>> result = hoaxController.getHoaxesOfUser(userId, pageable);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(page, result.getBody());
	}

	@Test
	void getHoaxById_success() {
		// Arrange
		Long id = 10L;
		DtoHoax response = easyRandom.nextObject(DtoHoax.class);
		when(hoaxService.getHoaxById(id)).thenReturn(response);

		// Act
		ResponseEntity<DtoHoax> result = hoaxController.getHoaxById(id);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(response.getId(), result.getBody().getId());
		assertEquals(response.getContent(), result.getBody().getContent());
	}

	@Test
	void getHoaxById_notFound_shouldPropagate() {
		// Arrange
		Long id = 10L;
		when(hoaxService.getHoaxById(id)).thenThrow(new BaseException(MessageType.HOAX_NOT_FOUND, "10"));

		// Act & Assert
		assertThrows(BaseException.class, () -> hoaxController.getHoaxById(id));
	}

	@Test
	void deleteHoax_success_shouldReturnNoContent() {
		// Act
		ResponseEntity<Void> result = hoaxController.deleteHoax(10L, currentUser);

		// Assert
		assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
		assertNull(result.getBody());
		verify(hoaxService, times(1)).deleteHoax(10L, 1L);
	}

	@Test
	void deleteHoax_notOwner_shouldPropagate() {
		// Arrange
		doThrow(new BaseException(MessageType.HOAX_DELETE_FORBIDDEN)).when(hoaxService).deleteHoax(10L, 1L);

		// Act & Assert
		assertThrows(BaseException.class, () -> hoaxController.deleteHoax(10L, currentUser));
	}
}
