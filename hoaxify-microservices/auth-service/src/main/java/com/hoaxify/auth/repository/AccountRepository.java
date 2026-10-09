package com.hoaxify.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hoaxify.auth.entities.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<Account> findByActivationToken(String activationToken);

	Optional<Account> findByPasswordResetToken(String passwordResetToken);
}
