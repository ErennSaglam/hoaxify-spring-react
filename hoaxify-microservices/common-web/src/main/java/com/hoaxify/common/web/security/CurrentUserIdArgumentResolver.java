package com.hoaxify.common.web.security;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.hoaxify.common.web.HoaxifyHeaders;
import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.common.web.exception.CommonErrorType;

/**
 * Spring MVC'ye "@CurrentUserId ile işaretli parametreyi nasıl dolduracağını" öğreten sınıf.
 * @PathVariable, @RequestParam gibi yerleşik anotasyonlar da aynı mekanizmayla çalışır.
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUserId.class)
				&& Long.class.equals(parameter.getParameterType());
	}

	@Override
	public Long resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		String header = webRequest.getHeader(HoaxifyHeaders.USER_ID);
		boolean required = parameter.getParameterAnnotation(CurrentUserId.class).required();

		if (header == null || header.isBlank()) {
			if (required) {
				throw new BaseException(CommonErrorType.UNAUTHORIZED);
			}
			return null;
		}
		try {
			return Long.valueOf(header.trim());
		} catch (NumberFormatException ex) {
			// Gateway her zaman sayı gönderir; sayı değilse istek gateway'den gelmemiştir
			throw new BaseException(CommonErrorType.UNAUTHORIZED);
		}
	}
}
