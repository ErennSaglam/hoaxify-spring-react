package com.hoaxify.user.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * id OTOMATİK ÜRETİLMEZ: auth-service'teki hesap id'sinin aynısıdır (UserRegisteredEvent ile gelir).
 * Böylece tüm servisler aynı kullanıcıyı aynı id ile tanır.
 * email burada bir KOPYA: sahibi auth-service. Gösterim için tutuluyor (denormalizasyon).
 */
@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

	@Id
	private Long id;

	@Column(name = "username", nullable = false)
	private String username;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "image")
	private String image;

	@Column(name = "bio", length = 255)
	private String bio;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
