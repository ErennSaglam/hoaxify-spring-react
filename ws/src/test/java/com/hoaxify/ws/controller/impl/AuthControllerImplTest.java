package com.hoaxify.ws.controller.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.hoaxify.ws.dto.DtoAuthResponse;
import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoMessage;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.services.IAuthService;
import com.hoaxify.ws.utils.MessageResolver;

import jakarta.servlet.http.Cookie;

/**
 * Tespit edilen senaryolar:
 *  login  : success (body + httpOnly cookie) | geçersiz bilgiler
 *  logout : cookie'de token | Authorization başlığında token | token yok (servis çağrılmaz)
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerImplTest {

	@Mock
	private IAuthService authService;

	@Mock
	private MessageResolver messageResolver;

	@InjectMocks
	private AuthControllerImpl authController;

	private EasyRandom easyRandom;

	@BeforeEach
	void setUp() {
		easyRandom = new EasyRandom();
	}

	@Test
	void login_success_shouldReturnBodyAndHttpOnlyCookie() {
		// Arrange
		DtoCredentialsIU credentials = easyRandom.nextObject(DtoCredentialsIU.class);
		DtoAuthResponse response = easyRandom.nextObject(DtoAuthResponse.class);
		response.getToken().setToken("abc");
		when(authService.authenticate(credentials)).thenReturn(response);

		// Act
		ResponseEntity<DtoAuthResponse> result = authController.login(credentials);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(response, result.getBody());
		String cookie = result.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
		assertTrue(cookie.startsWith("hoax-token=abc"));
		assertTrue(cookie.contains("HttpOnly"));
		assertTrue(cookie.contains("SameSite=Lax"));
		verify(authService, times(1)).authenticate(credentials);
	}

	@Test
	void login_invalidCredentials_shouldPropagate() {
		// Arrange
		DtoCredentialsIU credentials = easyRandom.nextObject(DtoCredentialsIU.class);
		when(authService.authenticate(credentials)).thenThrow(new BaseException(MessageType.INVALID_CREDENTIALS));

		// Act & Assert
		assertThrows(BaseException.class, () -> authController.login(credentials));
	}

	@Test
	void logout_tokenInCookie_shouldLogoutAndExpireCookie() {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("hoax-token", "cookie-token"));
		when(messageResolver.get("hoaxify.logout.success")).thenReturn("Logout success");

		// Act
		ResponseEntity<DtoMessage> result = authController.logout(request);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("Logout success", result.getBody().getMessage());
		assertTrue(result.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("Max-Age=0"));
		verify(authService, times(1)).logout("cookie-token");
	}

	@Test
	void logout_tokenInHeader_shouldLogout() {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token");
		when(messageResolver.get("hoaxify.logout.success")).thenReturn("Logout success");

		// Act
		authController.logout(request);

		// Assert
		verify(authService, times(1)).logout("header-token");
	}

	@Test
	void logout_noToken_shouldNotCallService() {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		when(messageResolver.get("hoaxify.logout.success")).thenReturn("Logout success");

		// Act
		ResponseEntity<DtoMessage> result = authController.logout(request);

		// Assert
		assertEquals(HttpStatus.OK, result.getStatusCode());
		verify(authService, never()).logout(any());
		// Token olmasa da tarayıcıdaki cookie temizlenir
		assertTrue(result.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("Max-Age=0"));
	}
}
