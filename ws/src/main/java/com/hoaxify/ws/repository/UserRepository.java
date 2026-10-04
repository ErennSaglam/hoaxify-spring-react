package com.hoaxify.ws.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hoaxify.ws.entities.User;

// JpaRepository'den türeyen arayüzlere stereotype anotasyonu eklemek gereksiz; Spring Data bunları kendisi bean yapar.
public interface UserRepository extends JpaRepository<User, Long> {

	// Derived query: metot adından SQL üretilir -> select ... from users where email = ?
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<User> findByActivationToken(String activationToken);

	Optional<User> findByPasswordResetToken(String passwordResetToken);

	/**
	 * @EntityGraph: profile ilişkisi LAZY olsa da bu sorguda JOIN ile tek seferde gelir.
	 * Olmasaydı listedeki her kullanıcı için ayrı bir profil sorgusu atılırdı (N+1 problemi).
	 */
	@EntityGraph(attributePaths = "profile")
	Page<User> findByIdNot(Long id, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = "profile")
	Page<User> findAll(Pageable pageable);
}
