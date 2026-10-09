package com.hoaxify.auth.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.auth.entities.Account;
import com.hoaxify.auth.repository.AccountRepository;
import com.hoaxify.common.event.UserRegisteredEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Geliştirme verisi: 10 aktif hesap. Profilleri user-service, olayı dinleyerek kendisi oluşturur
 * (activationToken = null olduğu için notification-service mail göndermez).
 * Böylece örnek veri bile servisler arasında gerçek akışla yayılır.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hoaxify.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public void run(String... args) {
		if (accountRepository.count() > 0) {
			return;
		}
		String hash = passwordEncoder.encode("P4ssword");
		for (int i = 1; i <= 10; i++) {
			Account account = new Account();
			account.setEmail("user" + i + "@mail.com");
			account.setPasswordHash(hash);
			account.setActive(true);
			accountRepository.save(account);
			eventPublisher.publishEvent(
					new UserRegisteredEvent(account.getId(), "user" + i, account.getEmail(), null, "en"));
		}
		log.info("Seeded 10 accounts (user1..user10@mail.com / P4ssword)");
	}
}
