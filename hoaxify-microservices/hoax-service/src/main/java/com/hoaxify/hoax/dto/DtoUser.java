package com.hoaxify.hoax.dto;

/** Yazar bilgisi: user-service'ten gRPC ile gelir */
public record DtoUser(Long id, String username, String email, String image, String bio) {

	/** Kullanıcı silinmiş (ve UserDeletedEvent henüz işlenmemiş) olabilir; ekranda boş kalmasın */
	public static DtoUser unknown(Long id) {
		return new DtoUser(id, "[deleted]", null, null, null);
	}
}
