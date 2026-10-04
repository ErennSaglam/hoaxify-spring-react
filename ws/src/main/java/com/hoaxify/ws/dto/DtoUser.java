package com.hoaxify.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kullanıcı response DTO'su. Entity'deki password, activationToken gibi alanlar
 * burada yok; client'a sadece göstermek istediğimiz alanlar gider.
 * image ve bio aslında UserProfile entity'sinde durur, burada düzleştirilmiş halde döner.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoUser {

	private Long id;

	private String username;

	private String email;

	private String image;

	private String bio;
}
