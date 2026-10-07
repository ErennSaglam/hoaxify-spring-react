package com.hoaxify.ws.controller.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

import com.hoaxify.ws.dto.DtoMessage;
import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.dto.DtoPasswordResetIU;
import com.hoaxify.ws.dto.DtoPasswordUpdateIU;
import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.dto.DtoUserIU;
import com.hoaxify.ws.dto.DtoUserUpdateIU;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.security.CurrentUser;
import com.hoaxify.ws.services.IUserService;
import com.hoaxify.ws.utils.MessageResolver;

/**
 * SAF UNIT TEST: Spring yok, HTTP yok. Controller metodu doğrudan çağrılır.
 * Business logic test edilmez; sadece controller'ın servisi doğru argümanlarla çağırıp
 * doğru HTTP status + body döndürdüğü kontrol edilir.
 * (Güvenlik, validation ve JSON sözleşmesi UserControllerImplWebMvcTest'te.)
 */
@ExtendWith(MockitoExtension.class)
class UserControllerImplTest {

	@Mock
	private IUserService userService;

	@Mock
	private MessageResolver messageResolver;

	@InjectMocks
	private UserControllerImpl userController;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
		// createUser Location başlığını mevcut istekten üretir (ServletUriComponentsBuilder.fromCurrentRequest)
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void createUser_success() {
		// Arrange
		DtoUserIU request = easyRandom.nextObject(DtoUserIU.class);
		DtoUser created = easyRandom.nextObject(DtoUser.class);
		created.setId(7L);

		when(userService.createUser(request)).thenReturn(created);
		when(messageResolver.get("hoaxify.create.user.success.message")).thenReturn("Check your mailbox");

		// Act
		ResponseEntity<DtoMessage> result = userController.createUser(request);

		// Assert
		assertEquals(HttpStatus.CREATED, result.getStatusCode());
		assertEquals("Check your mailbox", result.getBody().getMessage());
		assertEquals("http://localhost/api/v1/users/7", result.getHeaders().getLocation().toString());
		verify(userService, times(1)).createUser(request);
	}

	@Test
	void createUser_serviceException_shouldNotResolveSuccessMessage() {
		// Arrange
		DtoUserIU request = easyRandom.nextObject(DtoUserIU.class);
		when(userService.createUser(request)).thenThrow(new BaseException(MessageType.ACTIVATION_EMAIL_FAILURE));

		// Act & Assert
		assertThrows(BaseException.class, () -> userController.createUser(request));
		verifyNoInteractions(messageResolver);
	}

	@Test
	void activateUser_success() {
		// Arrange
		String token = "activation-token";
		when(messageResolver.get("hoaxify.activate.user.success.message")).thenReturn("Account is activated");

		// Act
		ResponseEntity<DtoMessage> result = userController.activateUser(token);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Account is activated", result.getBody().getMessage());
		verify(userService, times(1)).activateUser(token);
	}

	@Test
	void activateUser_invalidToken_shouldPropagate() {
		// Arrange
		String token = "bad";
		doThrow(new BaseException(MessageType.INVALID_ACTIVATION_TOKEN)).when(userService).activateUser(token);

		// Act & Assert
		assertThrows(BaseException.class, () -> userController.activateUser(token));
		verifyNoInteractions(messageResolver);
	}

	@Test
	void getUsers_anonymous_shouldPassNullUserId() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 3);
		DtoPage<DtoUser> page = new DtoPage<>(List.of(easyRandom.nextObject(DtoUser.class)), 0, 3, 1, 1, true, true);
		when(userService.getUsers(pageable, null)).thenReturn(page);

		// Act
		ResponseEntity<DtoPage<DtoUser>> result = userController.getUsers(pageable, null);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(page, result.getBody());
		verify(userService, times(1)).getUsers(pageable, null);
	}

	@Test
	void getUsers_loggedIn_shouldPassCurrentUserId() {
		// Arrange
		Pageable pageable = PageRequest.of(0, 3);
		User user = easyRandom.nextObject(User.class);
		user.setId(5L);
		CurrentUser currentUser = new CurrentUser(user);
		DtoPage<DtoUser> page = new DtoPage<>(List.of(), 0, 3, 0, 0, true, true);
		when(userService.getUsers(pageable, 5L)).thenReturn(page);

		// Act
		ResponseEntity<DtoPage<DtoUser>> result = userController.getUsers(pageable, currentUser);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		verify(userService, times(1)).getUsers(pageable, 5L);
	}

	@Test
	void getUserById_success() {
		// Arrange
		Long id = 1L;
		DtoUser response = easyRandom.nextObject(DtoUser.class);
		when(userService.getUserById(id)).thenReturn(response);

		// Act
		ResponseEntity<DtoUser> result = userController.getUserById(id);

		// Assert
		assertNotNull(result);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(response, result.getBody());
		verify(userService, times(1)).getUserById(id);
	}

	@Test
	void getUserById_notFound_shouldPropagate() {
		// Arrange
		Long id = 99L;
		when(userService.getUserById(id)).thenThrow(new BaseException(MessageType.USER_NOT_FOUND, "99"));

		// Act & Assert: HTTP 404'e çevirmek GlobalExceptionHandler'ın işi, controller'ın değil
		assertThrows(BaseException.class, () -> userController.getUserById(id));
	}

	@Test
	void updateUser_success() {
		// Arrange
		Long id = 1L;
		DtoUserUpdateIU request = easyRandom.nextObject(DtoUserUpdateIU.class);
		DtoUser response = easyRandom.nextObject(DtoUser.class);
		when(userService.updateUser(id, request)).thenReturn(response);

		// Act
		ResponseEntity<DtoUser> result = userController.updateUser(id, request);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(response, result.getBody());
		verify(userService, times(1)).updateUser(id, request);
	}

	@Test
	void deleteUser_success_shouldReturnNoContent() {
		// Arrange
		Long id = 1L;

		// Act
		ResponseEntity<Void> result = userController.deleteUser(id);

		// Assert
		assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
		assertNull(result.getBody());
		verify(userService, times(1)).deleteUser(id);
	}

	@Test
	void requestPasswordReset_success() {
		// Arrange
		DtoPasswordResetIU request = easyRandom.nextObject(DtoPasswordResetIU.class);
		when(messageResolver.get("hoaxify.password.reset.request.success")).thenReturn("Check your email");

		// Act
		ResponseEntity<DtoMessage> result = userController.requestPasswordReset(request);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Check your email", result.getBody().getMessage());
		verify(userService, times(1)).requestPasswordReset(request);
	}

	@Test
	void resetPassword_success() {
		// Arrange
		String token = "reset-token";
		DtoPasswordUpdateIU request = easyRandom.nextObject(DtoPasswordUpdateIU.class);
		when(messageResolver.get("hoaxify.password.reset.success")).thenReturn("Password updated");

		// Act
		ResponseEntity<DtoMessage> result = userController.resetPassword(token, request);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Password updated", result.getBody().getMessage());
		verify(userService, times(1)).resetPassword(token, request);
	}

	@Test
	void resetPassword_invalidToken_shouldPropagate() {
		// Arrange
		String token = "bad";
		DtoPasswordUpdateIU request = easyRandom.nextObject(DtoPasswordUpdateIU.class);
		doThrow(new BaseException(MessageType.INVALID_PASSWORD_RESET_TOKEN))
				.when(userService).resetPassword(token, request);

		// Act & Assert
		assertThrows(BaseException.class, () -> userController.resetPassword(token, request));
		verifyNoInteractions(messageResolver);
	}
}
