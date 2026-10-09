package com.hoaxify.auth.services.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.auth.client.UserServiceClient;
import com.hoaxify.auth.dto.DtoAuthResponse;
import com.hoaxify.auth.dto.DtoCredentialsIU;
import com.hoaxify.auth.dto.DtoToken;
import com.hoaxify.auth.dto.DtoUser;
import com.hoaxify.auth.entities.Account;
import com.hoaxify.auth.exception.AuthErrorType;
import com.hoaxify.auth.repository.AccountRepository;
import com.hoaxify.auth.services.IAuthService;
import com.hoaxify.auth.store.ITokenStore;
import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.common.web.exception.CommonErrorType;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;
	private final ITokenStore tokenStore;
	private final UserServiceClient userServiceClient;

	@Override
	@Transactional(readOnly = true)
	public DtoAuthResponse login(DtoCredentialsIU credentials) {
		Account account = accountRepository.findByEmail(credentials.getEmail())
				.filter(a -> passwordEncoder.matches(credentials.getPassword(), a.getPasswordHash()))
				.orElseThrow(() -> new BaseException(AuthErrorType.INVALID_CREDENTIALS));

		if (!account.isActive()) {
			throw new BaseException(AuthErrorType.USER_NOT_ACTIVE);
		}

		// Önce profili çekiyoruz, sonra token üretiyoruz: user-service cevap vermezse boşta token kalmasın
		DtoUser user = fetchUser(account.getId());
		String token = tokenStore.create(account.getId());
		return new DtoAuthResponse(user, new DtoToken("Bearer", token));
	}

	@Override
	public void logout(String token) {
		tokenStore.revoke(token);
	}

	@Override
	public Long verify(String token) {
		return tokenStore.findUserId(token)
				.orElseThrow(() -> new BaseException(AuthErrorType.INVALID_SESSION_TOKEN));
	}

	/**
	 * Servisler arası çağrı ağ üzerinden gider ve başarısız olabilir (servis kapalı, zaman aşımı, 404).
	 * FeignException'ı istemciye sızdırmıyoruz; anlamlı bir 503'e çeviriyoruz.
	 * Sonraki adım: Resilience4j circuit breaker (servis sürekli hata veriyorsa bir süre hiç denememek).
	 */
	private DtoUser fetchUser(Long userId) {
		try {
			return userServiceClient.getUser(userId);
		} catch (FeignException ex) {
			log.warn("user-service call failed for userId={} status={}", userId, ex.status());
			throw new BaseException(CommonErrorType.DEPENDENCY_UNAVAILABLE);
		}
	}
}
