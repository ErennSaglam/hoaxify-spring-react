package com.hoaxify.ws.configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hoaxify.ws.entities.Hoax;
import com.hoaxify.ws.entities.Tag;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.entities.UserProfile;
import com.hoaxify.ws.repository.HoaxRepository;
import com.hoaxify.ws.repository.TagRepository;
import com.hoaxify.ws.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sadece dev profilinde, veritabanı boşsa örnek veri oluşturur.
 * Giriş: user1@mail.com ... user25@mail.com / P4ssword
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private static final String DEFAULT_PASSWORD = "P4ssword";

	private final UserRepository userRepository;
	private final TagRepository tagRepository;
	private final HoaxRepository hoaxRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			return;
		}

		String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
		for (int i = 1; i <= 25; i++) {
			User user = new User();
			user.setUsername("user" + i);
			user.setEmail("user" + i + "@mail.com");
			user.setPassword(encodedPassword);
			user.setActive(true);
			UserProfile profile = new UserProfile();
			profile.setBio("Hi, I am user" + i);
			user.setProfileBidirectional(profile);
			userRepository.save(user);
		}

		Map<String, Tag> tags = tagRepository.saveAll(List.of(new Tag("java"), new Tag("spring"), new Tag("react")))
				.stream()
				.collect(Collectors.toMap(Tag::getName, Function.identity()));

		List<User> authors = userRepository.findAll().subList(0, 3);
		createHoax(authors.get(0), "Constructor injection > field injection", tags.get("java"), tags.get("spring"));
		createHoax(authors.get(0), "Spring Data JPA derived queries are neat", tags.get("spring"));
		createHoax(authors.get(1), "useReducer + Context for auth state", tags.get("react"));
		createHoax(authors.get(2), "Full-stack: Spring Boot backend, React frontend", tags.get("java"),
				tags.get("react"));

		log.info("Dev data created: 25 users (password: {}), {} hoaxes", DEFAULT_PASSWORD, hoaxRepository.count());
	}

	private void createHoax(User author, String content, Tag... tags) {
		Hoax hoax = new Hoax();
		hoax.setUser(author);
		hoax.setContent(content);
		hoax.getTags().addAll(List.of(tags));
		hoaxRepository.save(hoax);
	}
}
