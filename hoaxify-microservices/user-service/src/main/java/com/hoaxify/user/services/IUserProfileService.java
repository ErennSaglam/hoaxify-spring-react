package com.hoaxify.user.services;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.hoaxify.common.event.UserRegisteredEvent;
import com.hoaxify.common.web.dto.DtoPage;
import com.hoaxify.user.dto.DtoUser;
import com.hoaxify.user.dto.DtoUserUpdateIU;

public interface IUserProfileService {

	/** UserRegisteredEvent gelince çağrılır. Aynı olay iki kez gelirse ikinci seferde hiçbir şey yapmaz. */
	void createProfile(UserRegisteredEvent event);

	DtoPage<DtoUser> getUsers(Pageable pageable, Long currentUserId);

	DtoUser getUser(Long id);

	/** gRPC toplu sorgusu; bulunamayan id'ler sonuçta yer almaz */
	List<DtoUser> getUsersByIds(Collection<Long> ids);

	DtoUser updateUser(Long id, Long currentUserId, DtoUserUpdateIU request);

	void deleteUser(Long id, Long currentUserId);
}
