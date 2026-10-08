package com.hoaxify.common.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import com.hoaxify.common.web.HoaxifyHeaders;
import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.common.web.exception.CommonErrorType;

/**
 * Tespit edilen senaryolar:
 *  supportsParameter : @CurrentUserId Long -> evet | anotasyonsuz -> hayır
 *  resolveArgument   : başlık var | başlık yok + required -> 401 | başlık yok + optional -> null | sayı değil -> 401
 */
class CurrentUserIdArgumentResolverTest {

	private final CurrentUserIdArgumentResolver resolver = new CurrentUserIdArgumentResolver();

	/** Anotasyonları gerçek bir metottan okumak için örnek controller metotları */
	@SuppressWarnings("unused")
	static class SampleController {
		void required(@CurrentUserId Long userId) {
		}

		void optional(@CurrentUserId(required = false) Long userId) {
		}

		void plain(Long userId) {
		}
	}

	private MethodParameter parameterOf(String methodName) throws Exception {
		Method method = SampleController.class.getDeclaredMethod(methodName, Long.class);
		return new MethodParameter(method, 0);
	}

	private ServletWebRequest requestWithHeader(String value) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		if (value != null) {
			request.addHeader(HoaxifyHeaders.USER_ID, value);
		}
		return new ServletWebRequest(request);
	}

	@Test
	void supportsParameter() throws Exception {
		assertTrue(resolver.supportsParameter(parameterOf("required")));
		assertFalse(resolver.supportsParameter(parameterOf("plain")));
	}

	@Test
	void resolveArgument_headerPresent() throws Exception {
		assertEquals(42L, resolver.resolveArgument(parameterOf("required"), null, requestWithHeader("42"), null));
	}

	@Test
	void resolveArgument_missingAndRequired_shouldThrow401() throws Exception {
		MethodParameter parameter = parameterOf("required");
		ServletWebRequest request = requestWithHeader(null);

		BaseException exception = assertThrows(BaseException.class,
				() -> resolver.resolveArgument(parameter, null, request, null));

		assertEquals(CommonErrorType.UNAUTHORIZED, exception.getErrorType());
	}

	@Test
	void resolveArgument_missingAndOptional_shouldReturnNull() throws Exception {
		assertNull(resolver.resolveArgument(parameterOf("optional"), null, requestWithHeader(null), null));
	}

	@Test
	void resolveArgument_notANumber_shouldThrow401() throws Exception {
		MethodParameter parameter = parameterOf("required");
		ServletWebRequest request = requestWithHeader("abc");

		assertThrows(BaseException.class, () -> resolver.resolveArgument(parameter, null, request, null));
	}
}
