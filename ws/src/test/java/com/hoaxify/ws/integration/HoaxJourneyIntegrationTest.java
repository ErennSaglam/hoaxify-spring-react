package com.hoaxify.ws.integration;

import static com.hoaxify.ws.support.TestData.VALID_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.hoaxify.ws.support.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;

class HoaxJourneyIntegrationTest extends IntegrationTestBase {

	private String createHoax(String token, String json) throws Exception {
		return mockMvc.perform(post("/api/v1/hoaxes").header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
	}

	@Test
	void createHoaxesWithTags_thenBrowseFeedByTagAndUser() throws Exception {
		long aliceId = registerAndActivate("alice");
		registerAndActivate("bobby");
		String alice = login("alice@mail.com", VALID_PASSWORD);
		String bob = login("bobby@mail.com", VALID_PASSWORD);

		createHoax(alice, "{\"content\":\"first\",\"tags\":[\"#Java\",\"spring\"]}");
		createHoax(alice, "{\"content\":\"second\",\"tags\":[\"java\"]}");
		createHoax(bob, "{\"content\":\"bobby here\",\"tags\":[\"react\"]}");

		// Aynı etiket tekrar oluşturulmadı: #Java ve java -> tek "java" satırı
		assertThat(tagRepository.count()).isEqualTo(3);

		// Akış: en yeni en üstte
		mockMvc.perform(get("/api/v1/hoaxes"))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.content[0].content").value("bobby here"));

		mockMvc.perform(get("/api/v1/hoaxes").param("tag", "JAVA"))
				.andExpect(jsonPath("$.totalElements").value(2));

		mockMvc.perform(get("/api/v1/users/" + aliceId + "/hoaxes"))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].user.username").value("alice"));

		mockMvc.perform(get("/api/v1/tags"))
				.andExpect(jsonPath("$[0].name").value("java"));
	}

	@Test
	void onlyOwnerCanDeleteHoax() throws Exception {
		registerAndActivate("alice");
		registerAndActivate("bobby");
		String alice = login("alice@mail.com", VALID_PASSWORD);
		String bob = login("bobby@mail.com", VALID_PASSWORD);
		Integer hoaxId = JsonPath.read(createHoax(alice, "{\"content\":\"mine\"}"), "$.id");

		mockMvc.perform(delete("/api/v1/hoaxes/" + hoaxId).header("Authorization", bearer(bob)))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/v1/hoaxes/" + hoaxId)).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/v1/hoaxes/" + hoaxId).header("Authorization", bearer(alice)))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/hoaxes/" + hoaxId)).andExpect(status().isNotFound());
	}

	@Test
	void deletingUser_removesTheirHoaxes() throws Exception {
		long aliceId = registerAndActivate("alice");
		String alice = login("alice@mail.com", VALID_PASSWORD);
		createHoax(alice, "{\"content\":\"bye\",\"tags\":[\"java\"]}");

		mockMvc.perform(delete("/api/v1/users/" + aliceId).header("Authorization", bearer(alice)))
				.andExpect(status().isNoContent());

		assertThat(hoaxRepository.count()).isZero();
		assertThat(tagRepository.count()).as("etiketler kullanıcıya ait değil, kalır").isEqualTo(1);
	}
}
