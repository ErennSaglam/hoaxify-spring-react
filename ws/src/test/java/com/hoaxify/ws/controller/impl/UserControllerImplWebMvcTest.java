package com.hoaxify.ws.controller.impl;

import static com.hoaxify.ws.support.TestData.TEXT_DATA_URL;
import static com.hoaxify.ws.support.TestData.dtoUser;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.dto.DtoUserIU;
import com.hoaxify.ws.dto.DtoUserUpdateIU;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.repository.UserRepository;
import com.hoaxify.ws.services.IFileService;
import com.hoaxify.ws.services.IUserService;
import com.hoaxify.ws.support.TestData;
import com.hoaxify.ws.support.WebMvcTestSupport;

/**
 * WEB SLICE TEST: Gerçek HTTP isteği simüle edilir (MockMvc), ama servis katmanı mock.
 * Test edilen: URL eşleşmesi, JSON <-> DTO, @Valid, status kodları, güvenlik kuralları,
 * GlobalExceptionHandler'ın ürettiği ApiError formatı (frontend'in beklediği sözleşme).
 */
@WebMvcTest(UserControllerImpl.class)
class UserControllerImplWebMvcTest extends WebMvcTestSupport {

	@MockBean
	private IUserService userService;

	/** @UniqueEmail validator'ının bağımlılığı */
	@MockBean
	private UserRepository userRepository;

	/** @FileType validator'ının bağımlılığı */
	@MockBean
	private IFileService fileService;

	// ------------------------------------------------------------------
	// Kayıt (POST /api/v1/users)
	// ------------------------------------------------------------------

	@Test
	void returns201WithLocationAndMessage() throws Exception {
		given(userService.createUser(any(DtoUserIU.class))).willReturn(dtoUser(7L, "user1"));

		mockMvc.perform(post("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Accept-Language", "en")
				.content("""
						{"username":"user1","email":"user1@mail.com","password":"P4ssword"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/v1/users/7")))
				.andExpect(jsonPath("$.message").value("Please check your mailbox to activate your account"));
	}

	@Test
	void returns400WithFieldErrors_whenBodyInvalid() throws Exception {
		mockMvc.perform(post("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Accept-Language", "en")
				.content("""
						{"username":"ab","email":"not-an-email","password":"weak"}
						"""))
				.andExpect(status().isBadRequest())
				// Frontend bu üç alanı okuyor: status, message, validationErrors
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validation error"))
				.andExpect(jsonPath("$.path").value("/api/v1/users"))
				.andExpect(jsonPath("$.validationErrors.username").exists())
				.andExpect(jsonPath("$.validationErrors.email").exists())
				.andExpect(jsonPath("$.validationErrors.password").exists());

		verify(userService, never()).createUser(any());
	}

	@Test
	void returns400OnEmailField_whenEmailAlreadyRegistered() throws Exception {
		given(userRepository.existsByEmail("taken@mail.com")).willReturn(true);

		mockMvc.perform(post("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Accept-Language", "en")
				.content("""
						{"username":"user1","email":"taken@mail.com","password":"P4ssword"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationErrors.email").value("E-mail in use"));
	}

	@Test
	void localizesMessages_fromAcceptLanguageHeader() throws Exception {
		given(userRepository.existsByEmail("taken@mail.com")).willReturn(true);

		mockMvc.perform(post("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Accept-Language", "tr")
				.content("""
						{"username":"user1","email":"taken@mail.com","password":"P4ssword"}
						"""))
				.andExpect(jsonPath("$.message").value("Dogrulama hatasi"))
				.andExpect(jsonPath("$.validationErrors.email").value("E-posta adresi kullaniliyor"));
	}

	@Test
	void returns400_whenJsonMalformed() throws Exception {
		mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	// ------------------------------------------------------------------
	// Aktivasyon
	// ------------------------------------------------------------------

	@Test
	void returns200_forValidToken() throws Exception {
		mockMvc.perform(patch("/api/v1/users/abc/active").header("Accept-Language", "en"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Account is activated"));

		verify(userService).activateUser("abc");
	}

	@Test
	void returns400_forInvalidToken() throws Exception {
		willThrow(new BaseException(MessageType.INVALID_ACTIVATION_TOKEN)).given(userService).activateUser("bad");

		mockMvc.perform(patch("/api/v1/users/bad/active").header("Accept-Language", "en"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid activation token"));
	}

	// ------------------------------------------------------------------
	// Listeleme ve tekil kullanıcı
	// ------------------------------------------------------------------

	@Test
	void listUsers_passesNullCurrentUser_whenAnonymous() throws Exception {
		given(userService.getUsers(any(Pageable.class), isNull()))
				.willReturn(new DtoPage<>(List.of(dtoUser(1L, "user1")), 0, 3, 1, 1, true, true));

		mockMvc.perform(get("/api/v1/users").param("page", "0").param("size", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].username").value("user1"))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true));
	}

	@Test
	void listUsers_passesLoggedInUserId() throws Exception {
		given(userService.getUsers(any(Pageable.class), eq(1L)))
				.willReturn(new DtoPage<>(List.of(), 0, 3, 0, 0, true, true));

		mockMvc.perform(get("/api/v1/users").with(loggedInAs(1L))).andExpect(status().isOk());

		verify(userService).getUsers(any(Pageable.class), eq(1L));
	}

	@Test
	void getUser_returnsUserJson() throws Exception {
		given(userService.getUserById(5L)).willReturn(new DtoUser(5L, "user5", "user5@mail.com", "img", "bio"));

		mockMvc.perform(get("/api/v1/users/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.image").value("img"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void getUser_returns404WithLocalizedMessage() throws Exception {
		given(userService.getUserById(99L)).willThrow(new BaseException(MessageType.USER_NOT_FOUND, "99"));

		mockMvc.perform(get("/api/v1/users/99").header("Accept-Language", "en"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("User with ID 99 does not exist"));
	}

	@Test
	void getUser_returns400_forNonNumericId() throws Exception {
		mockMvc.perform(get("/api/v1/users/abc")).andExpect(status().isBadRequest());
	}

	// ------------------------------------------------------------------
	// Güncelleme ve silme (güvenlik kuralları)
	// ------------------------------------------------------------------

	private static final String BODY = """
			{"username":"newname","bio":"hello"}
			""";

	@Test
	void update_returns401_whenAnonymous() throws Exception {
		mockMvc.perform(put("/api/v1/users/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		verify(userService, never()).updateUser(any(), any());
	}

	@Test
	void update_returns403_whenUpdatingSomeoneElse() throws Exception {
		mockMvc.perform(put("/api/v1/users/2").with(loggedInAs(1L))
				.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));

		verify(userService, never()).updateUser(any(), any());
	}

	@Test
	void update_returns200_whenUpdatingSelf() throws Exception {
		given(userService.updateUser(eq(1L), any(DtoUserUpdateIU.class)))
				.willReturn(new DtoUser(1L, "newname", "user1@mail.com", null, "hello"));

		mockMvc.perform(put("/api/v1/users/1").with(loggedInAs(1L))
				.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("newname"))
				.andExpect(jsonPath("$.bio").value("hello"));
	}

	@Test
	void update_returns400_whenImageIsNotAnImage() throws Exception {
		given(fileService.detectType(TEXT_DATA_URL)).willReturn("text/plain");

		mockMvc.perform(put("/api/v1/users/1").with(loggedInAs(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"newname\",\"image\":\"" + TEXT_DATA_URL + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationErrors.image").value("Only jpeg, png are allowed"));
	}

	@Test
	void update_returns401_whenUserNotActive() throws Exception {
		User inactive = TestData.user(1L, "user1");
		inactive.setActive(false);
		given(tokenService.verifyToken("inactive-token")).willReturn(Optional.of(inactive));

		mockMvc.perform(put("/api/v1/users/1").header("Authorization", "Bearer inactive-token")
				.header("Accept-Language", "en")
				.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Account is not activated yet"));
	}

	@Test
	void delete_returns204_forSelf() throws Exception {
		mockMvc.perform(delete("/api/v1/users/1").with(loggedInAs(1L)))
				.andExpect(status().isNoContent());

		verify(userService).deleteUser(1L);
	}

	@Test
	void delete_returns403_forSomeoneElse() throws Exception {
		mockMvc.perform(delete("/api/v1/users/2").with(loggedInAs(1L)))
				.andExpect(status().isForbidden());

		verify(userService, never()).deleteUser(any());
	}

	// ------------------------------------------------------------------
	// Şifre sıfırlama
	// ------------------------------------------------------------------

	@Test
	void request_returns200() throws Exception {
		mockMvc.perform(post("/api/v1/users/password-reset").header("Accept-Language", "en")
				.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"user1@mail.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Check your email address to reset your password"));
	}

	@Test
	void request_returns400_forInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/v1/users/password-reset")
				.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nope\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationErrors.email").exists());
	}

	@Test
	void setPassword_returns400_forWeakPassword() throws Exception {
		mockMvc.perform(patch("/api/v1/users/tk/password").header("Accept-Language", "en")
				.contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"alllowercase\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationErrors.password")
						.value("Must have uppercase, lowercase letters and number"));
	}

	@Test
	void setPassword_returns200() throws Exception {
		mockMvc.perform(patch("/api/v1/users/tk/password")
				.contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"N3wPassword\"}"))
				.andExpect(status().isOk());

		verify(userService).resetPassword(eq("tk"), any());
	}
}
