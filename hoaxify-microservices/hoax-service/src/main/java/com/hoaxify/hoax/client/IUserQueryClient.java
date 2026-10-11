package com.hoaxify.hoax.client;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.hoaxify.hoax.dto.DtoUser;

/**
 * user-service'e sorulan sorular. Servis katmanı bunun gRPC ile yapıldığını bilmez;
 * yarın REST'e dönülse sadece impl değişir (unit testlerde de kolayca mock'lanır).
 */
public interface IUserQueryClient {

	/** Kullanıcı yoksa boş */
	Optional<DtoUser> findUser(Long userId);

	/** Tek çağrıda birden çok kullanıcı; bulunamayanlar map'te yer almaz */
	Map<Long, DtoUser> findUsers(Collection<Long> userIds);
}
