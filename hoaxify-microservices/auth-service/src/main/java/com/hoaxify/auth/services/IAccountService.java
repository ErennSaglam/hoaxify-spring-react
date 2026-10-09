package com.hoaxify.auth.services;

import com.hoaxify.auth.dto.DtoPasswordResetIU;
import com.hoaxify.auth.dto.DtoPasswordUpdateIU;
import com.hoaxify.auth.dto.DtoRegisterIU;

/** Hesap yaşam döngüsü: kayıt, aktivasyon, şifre sıfırlama, silme */
public interface IAccountService {

	/** @return oluşturulan hesabın (= kullanıcının) id'si */
	Long register(DtoRegisterIU request);

	void activate(String activationToken);

	void requestPasswordReset(DtoPasswordResetIU request);

	void resetPassword(String passwordResetToken, DtoPasswordUpdateIU request);

	/** UserDeletedEvent gelince çağrılır; hesap yoksa sessizce geçer (aynı olay iki kez gelebilir) */
	void deleteAccount(Long userId);
}
