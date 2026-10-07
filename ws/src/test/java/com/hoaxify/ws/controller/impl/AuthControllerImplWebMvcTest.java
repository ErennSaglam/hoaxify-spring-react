package com.hoaxify.ws.controller.impl;

import static com.hoaxify.ws.support.TestData.dtoUser;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import com.hoaxify.ws.dto.DtoAuthResponse;
import com.hoaxify.ws.dto.DtoCredentialsIU;
import com.hoaxify.ws.dto.DtoToken;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.services.IAuthService;
import com.hoaxify.ws.support.WebMvcTestSupport;

import jakarta.servlet.http.Cookie;

@WebMvcTest(AuthControllerImpl.class)
class AuthControllerImplWebMvcTest extends WebMvcTestSupport {

	private static final String CREDENTIALS = """
			{"email":"user1@mail.com","password":"P4ssword"}
			""";

	@MockBean
	private IAuthService authService;

	@Test
	void login_returnsUserAndToken_andSetsHttpOnlyCookie() throws Exception {
		given(authService.authenticate(any(DtoCredentialsIU.class)))
				.willReturn(new DtoAuthResponse(dtoUser(1L, "user1"), new DtoToken("Bearer", "abc")));

		mockMvc.perform(post("/api/v1/auth").contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.id").value(1))
				.andExpect(jsonPath("$.token.token").value("abc"))
				.andExpect(cookie().value("hoax-token", "abc"))
				.andExpect(cookie().httpOnly("hoax-token", true))
				.andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
	}

	@Test
	void login_returns401_forInvalidCredentials() throws Exception {
		given(authService.authenticate(any())).willThrow(new BaseException(MessageType.INVALID_CREDENTIALS));

		mockMvc.perform(post("/api/v1/auth").header("Accept-Language", "en")
				.contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Invalid credentials"))
				.andExpect(cookie().doesNotExist("hoax-token"));
	}

	@Test
	void login_returns400_whenFieldsBlank() throws Exception {
		mockMvc.perform(post("/api/v1/auth").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationErrors.email").exists())
				.andExpect(jsonPath("$.validationErrors.password").exists());

		verify(authService, never()).authenticate(any());
	}

	@Test
	void logout_invalidatesCookieToken_andExpiresCookie() throws Exception {
		mockMvc.perform(post("/api/v1/logout").cookie(new Cookie("hoax-token", "abc")))
				.andExpect(status().isOk())
				.andExpect(cookie().maxAge("hoax-token", 0));

		verify(authService).logout("abc");
	}

	@Test
	void logout_withoutToken_stillSucceeds() throws Exception {
		mockMvc.perform(post("/api/v1/logout")).andExpect(status().isOk());

		verify(authService, never()).logout(any());
	}
}
