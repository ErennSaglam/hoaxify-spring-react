package com.hoaxify.ws.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Opaque token stratejisinde veritabanında saklanan oturum token'ı.
 * Logout olunca satır silinir, böylece token gerçekten geçersiz olur.
 */
@Entity
@Table(name = "token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Token {

	@Id
	@Column(name = "token")
	private String token;

	/**
	 * EAGER: TokenFilter transaction dışında çalışır ve kullanıcının active alanını okur.
	 * LAZY olsaydı LazyInitializationException alırdık.
	 */
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
}
