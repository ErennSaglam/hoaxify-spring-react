package com.hoaxify.auth.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Giriş bilgileri. id, tüm sistemde "kullanıcı id"si olarak kullanılır: user-service profili,
 * hoax-service gönderileri bu id ile ilişkilendirir (ama veritabanları ayrı olduğu için FK yok).
 */
@Entity
@Table(name = "account", uniqueConstraints = @UniqueConstraint(name = "uk_account_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "activation_token")
	private String activationToken;

	@Column(name = "password_reset_token")
	private String passwordResetToken;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
