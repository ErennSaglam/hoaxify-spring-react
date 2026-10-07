package com.hoaxify.ws.integration;

import static com.hoaxify.ws.support.TestData.PNG_DATA_URL;
import static com.hoaxify.ws.support.TestData.VALID_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import com.hoaxify.ws.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Frontend'deki kullanıcı akışlarının uçtan uca karşılığı */
class UserJourneyIntegrationTest extends IntegrationTestBase {

	@Test
	void signUp_activate_login_updateProfile_deleteAccount() throws Exception {
		// 1) Aktivasyon öncesi giriş reddedilir
		mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"alice\",\"email\":\"alice@mail.com\",\"password\":\"P4ssword\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/auth").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"alice@mail.com\",\"password\":\"P4ssword\"}"))
				.andExpect(status().isUnauthorized());

		// 2) Mail ile gelen token'la aktivasyon, sonra giriş (cookie ile)
		ArgumentCaptor<String> activationToken = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendActivationEmail(eq("alice@mail.com"), activationToken.capture());
		mockMvc.perform(patch("/api/v1/users/" + activationToken.getValue() + "/active")).andExpect(status().isOk());

		String loginBody = mockMvc.perform(post("/api/v1/auth").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"alice@mail.com\",\"password\":\"P4ssword\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		Integer id = JsonPath.read(loginBody, "$.user.id");
		String token = JsonPath.read(loginBody, "$.token.token");
		Cookie cookie = new Cookie("hoax-token", token);

		// 3) Profil güncelleme: yeni resim diske yazılır ve /assets altından servis edilir
		String updated = mockMvc.perform(put("/api/v1/users/" + id).cookie(cookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"alice2\",\"bio\":\"hi\",\"image\":\"" + PNG_DATA_URL + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("alice2"))
				.andExpect(jsonPath("$.bio").value("hi"))
				.andReturn().getResponse().getContentAsString();
		String image = JsonPath.read(updated, "$.image");
		mockMvc.perform(get("/assets/profile/" + image)).andExpect(status().isOk());

		// 4) Hesap silme -> kullanıcı yok, eski token artık geçersiz
		mockMvc.perform(delete("/api/v1/users/" + id).cookie(cookie)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/users/" + id)).andExpect(status().isNotFound());
		assertThat(tokenRepository.findById(token)).isEmpty();
	}

	@Test
	void passwordReset_flow() throws Exception {
		registerAndActivate("bobby");

		mockMvc.perform(post("/api/v1/users/password-reset").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"bobby@mail.com\"}"))
				.andExpect(status().isOk());
		ArgumentCaptor<String> resetToken = ArgumentCaptor.forClass(String.class);
		verify(emailService).sendPasswordResetEmail(eq("bobby@mail.com"), resetToken.capture());

		mockMvc.perform(patch("/api/v1/users/" + resetToken.getValue() + "/password")
				.contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"N3wPassword\"}"))
				.andExpect(status().isOk());

		// Aynı token ikinci kez kullanılamaz
		mockMvc.perform(patch("/api/v1/users/" + resetToken.getValue() + "/password")
				.contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"An0therOne\"}"))
				.andExpect(status().isBadRequest());

		login("bobby@mail.com", "N3wPassword");
		mockMvc.perform(post("/api/v1/auth").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"bobby@mail.com\",\"password\":\"" + VALID_PASSWORD + "\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userList_excludesLoggedInUser() throws Exception {
		registerAndActivate("alice");
		registerAndActivate("bobby");
		String token = login("alice@mail.com", VALID_PASSWORD);

		mockMvc.perform(get("/api/v1/users"))
				.andExpect(jsonPath("$.totalElements").value(2));
		mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(token)))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].username").value("bobby"));
	}

	@Test
	void logout_invalidatesToken() throws Exception {
		long id = registerAndActivate("alice");
		String token = login("alice@mail.com", VALID_PASSWORD);
		String body = "{\"username\":\"alice\"}";

		mockMvc.perform(put("/api/v1/users/" + id).header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/logout").header("Authorization", bearer(token))).andExpect(status().isOk());

		mockMvc.perform(put("/api/v1/users/" + id).header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
	}
}
