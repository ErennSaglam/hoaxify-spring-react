package com.hoaxify.ws.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User ile @OneToOne ilişkinin ters (inverse) tarafı. FK users tablosunda
 * olduğundan burada mappedBy = "profile" kullanılır.
 */
@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Diskteki profil resminin dosya adı (uploads/profile/{image}) */
	@Column(name = "image")
	private String image;

	@Column(name = "bio", length = 255)
	private String bio;

	@OneToOne(mappedBy = "profile")
	private User user;
}
