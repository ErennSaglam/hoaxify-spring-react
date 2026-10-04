package com.hoaxify.ws.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * "user" PostgreSQL'de rezerve kelime olduğu için tablo adı "users".
 *
 * Entity'lerde Lombok Data anotasyonu kullanmıyoruz: çift yönlü ilişkilerde (User <-> UserProfile,
 * User <-> Hoax) Lombok'un ürettiği toString/hashCode birbirini çağırıp
 * StackOverflowError'a yol açar.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "username", nullable = false)
	private String username;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "password", nullable = false)
	private String password;

	@Column(name = "active", nullable = false)
	private boolean active = false;

	@Column(name = "activation_token")
	private String activationToken;

	@Column(name = "password_reset_token")
	private String passwordResetToken;

	/**
	 * @OneToOne (çift yönlü) - ilişkinin sahibi User: users tablosunda profile_id FK'si durur.
	 * cascade ALL: User kaydedilince/silinince profil de kaydedilir/silinir.
	 */
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "profile_id", referencedColumnName = "id")
	private UserProfile profile;

	/**
	 * @OneToMany - ilişkinin sahibi Hoax tarafı (hoax.user_id). mappedBy sayesinde
	 * ara (join) tablo oluşmaz. Kullanıcı silinince hoax'ları da silinir.
	 */
	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<Hoax> hoaxes = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<Token> tokens = new ArrayList<>();

	/** İki yönü birlikte set eden yardımcı metot; ilişkiyi tek yönden kurup diğerini unutmayı önler. */
	public void setProfileBidirectional(UserProfile profile) {
		this.profile = profile;
		if (profile != null) {
			profile.setUser(this);
		}
	}
}
