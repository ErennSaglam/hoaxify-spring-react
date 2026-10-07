package com.hoaxify.ws.support;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.hoaxify.ws.repository.HoaxRepository;
import com.hoaxify.ws.repository.TagRepository;
import com.hoaxify.ws.repository.TokenRepository;
import com.hoaxify.ws.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * ENTEGRASYON TESTİ: Uygulamanın TAMAMI ayağa kalkar (@SpringBootTest): gerçek controller,
 * servis, repository, security filtresi ve Docker'daki gerçek PostgreSQL. Tek mock e-posta
 * servisi: gerçek mail göndermek istemiyoruz, gönderilen token'ı yakalayıp kullanıyoruz.
 *
 * Breakpoint koyup debug etmek için en iyi yer burası: bir isteğin filtreden
 * veritabanına kadar tüm yolculuğunu adım adım izleyebilirsin.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase implements PostgresTestContainer {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected UserRepository userRepository;
	@Autowired
	protected HoaxRepository hoaxRepository;
	@Autowired
	protected TagRepository tagRepository;
	@Autowired
	protected TokenRepository tokenRepository;

	@MockBean
	protected com.hoaxify.ws.services.IEmailService emailService;

	/*
	 * @SpringBootTest rollback yapmaz ve PostgreSQL container'ı tüm test sınıflarınca paylaşılır.
	 * Hem önce hem sonra temizliyoruz: kendi testlerimiz temiz başlasın, sonra çalışan
	 * @DataJpaTest / DAO testleri de bizim bıraktığımız veriyi görmesin.
	 */
	@BeforeEach
	@AfterEach
	void cleanDatabase() {
		tokenRepository.deleteAll();
		hoaxRepository.deleteAll();
		tagRepository.deleteAll();
		userRepository.deleteAll();
	}

	/** API üzerinden kayıt olur, mail ile gelen token'la hesabı aktifleştirir; kullanıcı id'sini döner */
	protected long registerAndActivate(String username) throws Exception {
		String email = username + "@mail.com";
		String body = mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}"
						.formatted(username, email, TestData.VALID_PASSWORD)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");

		ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
		verify(emailService, atLeastOnce()).sendActivationEmail(eq(email), token.capture());
		mockMvc.perform(patch("/api/v1/users/" + token.getValue() + "/active")).andExpect(status().isOk());

		return Long.parseLong(body.substring(body.lastIndexOf('/') + 1));
	}

	/** Giriş yapar ve Authorization başlığında kullanılacak token'ı döner */
	protected String login(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.token.token");
	}

	protected String bearer(String token) {
		return "Bearer " + token;
	}
}
