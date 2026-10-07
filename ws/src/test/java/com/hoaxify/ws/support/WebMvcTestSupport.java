package com.hoaxify.ws.support;

import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.security.SecurityConfiguration;
import com.hoaxify.ws.services.ITokenService;
import com.hoaxify.ws.utils.MessageResolver;

/**
 * @WebMvcTest sınıfları için ortak ayarlar.
 *
 * @WebMvcTest sadece web katmanını yükler (controller, @ControllerAdvice, filtreler, Jackson).
 * Servisler, repository'ler, @Configuration sınıfları YÜKLENMEZ; ihtiyacımız olanları
 * @Import ile ekliyoruz, geri kalanları @MockBean ile taklit ediyoruz.
 *
 *  - SecurityConfiguration: gerçek güvenlik kuralları (401/403) test edilsin diye
 *  - MessageResolver: GlobalExceptionHandler'ın bağımlılığı (i18n mesajları)
 *  - ITokenService: TokenFilter'ın bağımlılığı; hangi token'ın geçerli olduğunu biz belirliyoruz
 */
@Import({ SecurityConfiguration.class, MessageResolver.class })
public abstract class WebMvcTestSupport {

	protected static final String VALID_TOKEN = "valid-token";

	@Autowired
	protected MockMvc mockMvc;

	@MockBean
	protected ITokenService tokenService;

	/**
	 * İsteği "id numaralı kullanıcı giriş yapmış" gibi gönderir: gerçek TokenFilter çalışır,
	 * token'ı mock ITokenService doğrular.
	 */
	protected RequestPostProcessor loggedInAs(Long id) {
		User user = TestData.user(id, "user" + id);
		given(tokenService.verifyToken(VALID_TOKEN)).willReturn(Optional.of(user));
		return request -> {
			request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
			return request;
		};
	}
}
