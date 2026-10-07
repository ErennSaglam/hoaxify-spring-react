package com.hoaxify.ws.controller.impl;

import static com.hoaxify.ws.support.TestData.dtoUser;
import static org.assertj.core.api.Assertions.assertThat;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;

import com.hoaxify.ws.dto.DtoHoax;
import com.hoaxify.ws.dto.DtoHoaxIU;
import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.dto.DtoTag;
import com.hoaxify.ws.exception.BaseException;
import com.hoaxify.ws.exception.MessageType;
import com.hoaxify.ws.services.IHoaxService;
import com.hoaxify.ws.services.ITagService;
import com.hoaxify.ws.support.WebMvcTestSupport;

@WebMvcTest({ HoaxControllerImpl.class, TagControllerImpl.class })
class HoaxControllerImplWebMvcTest extends WebMvcTestSupport {

	@MockBean
	private IHoaxService hoaxService;
	@MockBean
	private ITagService tagService;

	private final DtoHoax sample = new DtoHoax(10L, "Hello", LocalDateTime.of(2026, 1, 1, 12, 0),
			dtoUser(1L, "user1"), List.of(new DtoTag(1L, "java")));

	@Test
	void create_returns401_whenAnonymous() throws Exception {
		mockMvc.perform(post("/api/v1/hoaxes").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hi\"}"))
				.andExpect(status().isUnauthorized());

		verify(hoaxService, never()).createHoax(any(), any());
	}

	@Test
	void create_returns201_andUsesLoggedInUserAsAuthor() throws Exception {
		given(hoaxService.createHoax(eq(1L), any(DtoHoaxIU.class))).willReturn(sample);

		mockMvc.perform(post("/api/v1/hoaxes").with(loggedInAs(1L))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"Hello\",\"tags\":[\"java\"]}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/v1/hoaxes/10")))
				.andExpect(jsonPath("$.user.username").value("user1"))
				.andExpect(jsonPath("$.tags[0].name").value("java"))
				.andExpect(jsonPath("$.createdAt").value("2026-01-01T12:00:00"));
	}

	@Test
	void create_returns400_forBlankContentAndInvalidTag() throws Exception {
		mockMvc.perform(post("/api/v1/hoaxes").with(loggedInAs(1L)).header("Accept-Language", "en")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"  \",\"tags\":[\"bad tag!\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationErrors.content").exists())
				.andExpect(jsonPath("$.validationErrors['tags[0]']")
						.value("Tags can only contain letters, numbers and _"));
	}

	@Test
	void list_defaultsToNewestFirst_andPassesTagFilter() throws Exception {
		given(hoaxService.getHoaxes(eq("java"), any(Pageable.class)))
				.willReturn(new DtoPage<>(List.of(sample), 0, 10, 1, 1, true, true));

		mockMvc.perform(get("/api/v1/hoaxes").param("tag", "java"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(10));

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(hoaxService).getHoaxes(eq("java"), captor.capture());
		assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void list_withoutTag_passesNull() throws Exception {
		given(hoaxService.getHoaxes(isNull(), any(Pageable.class)))
				.willReturn(new DtoPage<>(List.of(), 0, 10, 0, 0, true, true));

		mockMvc.perform(get("/api/v1/hoaxes")).andExpect(status().isOk());
	}

	@Test
	void hoaxesOfUser_returns404_whenUserMissing() throws Exception {
		given(hoaxService.getHoaxesOfUser(eq(99L), any(Pageable.class)))
				.willThrow(new BaseException(MessageType.USER_NOT_FOUND, "99"));

		mockMvc.perform(get("/api/v1/users/99/hoaxes")).andExpect(status().isNotFound());
	}

	@Test
	void getById_returns404_whenMissing() throws Exception {
		given(hoaxService.getHoaxById(5L)).willThrow(new BaseException(MessageType.HOAX_NOT_FOUND, "5"));

		mockMvc.perform(get("/api/v1/hoaxes/5").header("Accept-Language", "en"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Hoax with ID 5 does not exist"));
	}

	@Test
	void delete_returns204_forOwner() throws Exception {
		mockMvc.perform(delete("/api/v1/hoaxes/10").with(loggedInAs(1L))).andExpect(status().isNoContent());

		verify(hoaxService).deleteHoax(10L, 1L);
	}

	@Test
	void delete_returns403_whenServiceRejectsNonOwner() throws Exception {
		willThrow(new BaseException(MessageType.HOAX_DELETE_FORBIDDEN)).given(hoaxService).deleteHoax(10L, 2L);

		mockMvc.perform(delete("/api/v1/hoaxes/10").with(loggedInAs(2L)).header("Accept-Language", "en"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You can only delete your own hoaxes"));
	}

	@Test
	void tags_returnsList() throws Exception {
		given(tagService.getTags()).willReturn(List.of(new DtoTag(1L, "java"), new DtoTag(2L, "spring")));

		mockMvc.perform(get("/api/v1/tags"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("java"))
				.andExpect(jsonPath("$[1].name").value("spring"));
	}
}
