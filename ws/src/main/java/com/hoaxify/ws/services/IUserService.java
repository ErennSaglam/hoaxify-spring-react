package com.hoaxify.ws.services;

import org.springframework.data.domain.Pageable;

import com.hoaxify.ws.dto.DtoPage;
import com.hoaxify.ws.dto.DtoPasswordResetIU;
import com.hoaxify.ws.dto.DtoPasswordUpdateIU;
import com.hoaxify.ws.dto.DtoUser;
import com.hoaxify.ws.dto.DtoUserIU;
import com.hoaxify.ws.dto.DtoUserUpdateIU;

public interface IUserService {

	DtoUser createUser(DtoUserIU dtoUserIU);

	void activateUser(String activationToken);

	/** currentUserId null değilse giriş yapmış kullanıcı listeden çıkarılır */
	DtoPage<DtoUser> getUsers(Pageable pageable, Long currentUserId);

	DtoUser getUserById(Long id);

	DtoUser updateUser(Long id, DtoUserUpdateIU dtoUserUpdateIU);

	void deleteUser(Long id);

	void requestPasswordReset(DtoPasswordResetIU dtoPasswordResetIU);

	void resetPassword(String passwordResetToken, DtoPasswordUpdateIU dtoPasswordUpdateIU);
}
