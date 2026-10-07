package com.hoaxify.ws.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.Cookie;

/**
 * TokenResolver statik yardımcı sınıf, bağımlılığı yok. MockHttpServletRequest: Spring'in sahte HTTP isteği.
 *
 * Tespit edilen senaryolar:
 *  resolve : cookie var | cookie ve başlık var (cookie öncelikli) | cookie boş -> başlığa düş
 *            | alakasız cookie | Bearer başlığı | Basic başlığı | başlık yok | sadece prefix | hiç cookie yok
 */
class TokenResolverTest {

	@Test
	void resolve_cookie_success() {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("hoax-token", "from-cookie"));

		// Act
		Optional<String> result = TokenResolver.resolve(request);

		// Assert
		assertTrue(result.isPresent());
		assertEquals("from-cookie", result.get());
	}

	@Test
	void resolve_cookieAndHeader_shouldPreferCookie() {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("hoax-token", "from-cookie"));
		request.addHeader("Authorization", "Bearer from-header");

		// Act & Assert
		assertEquals("from-cookie", TokenResolver.resolve(request).get());
	}

	@Test
	void resolve_blankCookie_shouldFallBackToHeader() {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("hoax-token", ""), new Cookie("other", "x"));
		request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

		// Act & Assert
		assertEquals("dXNlcjpwYXNz", TokenResolver.resolve(request).get());
	}

	@Test
	void resolve_bearerHeader_shouldStripPrefix() {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer abc.def");

		// Act & Assert
		assertEquals("abc.def", TokenResolver.resolve(request).get());
	}

	@Test
	void resolve_noCookieNoHeader_shouldReturnEmpty() {
		assertFalse(TokenResolver.resolve(new MockHttpServletRequest()).isPresent());
	}

	@Test
	void resolve_onlyUnrelatedCookie_andNoHeader_shouldReturnEmpty() {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("JSESSIONID", "x"));

		// Act & Assert
		assertFalse(TokenResolver.resolve(request).isPresent());
	}

	@Test
	void resolve_headerWithOnlyPrefix_shouldReturnEmpty() {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer");

		// Act & Assert
		assertFalse(TokenResolver.resolve(request).isPresent());
	}

	@Test
	void resolve_cookieWithNullValue_shouldFallBackToHeader() {
		// Arrange
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("hoax-token", null));
		request.addHeader("Authorization", "Bearer from-header");

		// Act & Assert
		assertEquals("from-header", TokenResolver.resolve(request).get());
	}
}
