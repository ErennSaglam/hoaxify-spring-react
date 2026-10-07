package com.hoaxify.ws.repository;

import static com.hoaxify.ws.support.TestData.newUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.hoaxify.ws.entities.Hoax;
import com.hoaxify.ws.entities.Token;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.entities.UserProfile;
import com.hoaxify.ws.support.PostgresTestContainer;

/**
 * REPOSITORY (SLICE) TEST: Sadece JPA katmanı yüklenir (entity'ler + repository'ler),
 * Docker'daki GERÇEK PostgreSQL'e karşı çalışır. Controller/servis yüklenmez.
 *
 * @DataJpaTest her testi bir transaction içinde çalıştırır ve sonunda ROLLBACK eder;
 * testler birbirinin verisini görmez.
 *
 * TestEntityManager.flush() + clear(): bellekteki (1. seviye cache) nesneleri boşaltır.
 * Böylece sonraki okuma gerçekten SQL ile veritabanından gelir; ilişkilerin doğru
 * kaydedildiğini ancak böyle kanıtlayabiliriz.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest implements PostgresTestContainer {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	private User persist(String username) {
		return entityManager.persist(newUser(username));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void derivedQueries_findByEmailAndTokens() {
		User user = newUser("user1");
		user.setActivationToken("act-1");
		user.setPasswordResetToken("reset-1");
		entityManager.persist(user);
		flushAndClear();

		assertThat(userRepository.findByEmail("user1@mail.com")).isPresent();
		assertThat(userRepository.findByEmail("nobody@mail.com")).isEmpty();
		assertThat(userRepository.existsByEmail("user1@mail.com")).isTrue();
		assertThat(userRepository.findByActivationToken("act-1")).isPresent();
		assertThat(userRepository.findByPasswordResetToken("reset-1")).isPresent();
	}

	@Test
	void findByIdNot_excludesGivenUser_andPaginates() {
		User me = persist("user1");
		persist("user2");
		persist("user3");
		persist("user4");
		flushAndClear();

		Page<User> page = userRepository.findByIdNot(me.getId(), PageRequest.of(0, 2, Sort.by("id")));

		assertThat(page.getContent()).extracting(User::getUsername).containsExactly("user2", "user3");
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.isLast()).isFalse();
	}

	@Test
	void entityGraph_loadsProfileInSameQuery() {
		persist("user1");
		flushAndClear();

		User loaded = userRepository.findAll(PageRequest.of(0, 10)).getContent().get(0);

		// @EntityGraph olmasaydı profile LAZY bir proxy olarak kalırdı (initialized = false)
		assertThat(Hibernate.isInitialized(loaded.getProfile())).isTrue();
	}

	@Test
	void oneToOne_isSavedByCascade_andNavigableFromBothSides() {
		User user = persist("user1");
		flushAndClear();

		User loaded = userRepository.findById(user.getId()).orElseThrow();
		UserProfile profile = loaded.getProfile();

		assertThat(profile.getId()).isNotNull();
		assertThat(profile.getBio()).isEqualTo("bio of user1");
		assertThat(profile.getUser().getId()).isEqualTo(user.getId()); // ters taraf (mappedBy)
	}

	@Test
	void uniqueConstraint_rejectsDuplicateEmail() {
		userRepository.saveAndFlush(newUser("user1"));
		User duplicate = newUser("other");
		duplicate.setEmail("user1@mail.com");

		assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletingUser_cascadesToProfileHoaxesAndTokens() {
		User user = persist("user1");
		Hoax hoax = new Hoax();
		hoax.setUser(user);
		hoax.setContent("will be deleted");
		entityManager.persist(hoax);
		entityManager.persist(new Token("tok-1", user));
		Long profileId = user.getProfile().getId();
		flushAndClear();

		userRepository.delete(userRepository.findById(user.getId()).orElseThrow());
		flushAndClear();

		assertThat(entityManager.find(User.class, user.getId())).isNull();
		assertThat(entityManager.find(UserProfile.class, profileId)).isNull();
		assertThat(entityManager.find(Hoax.class, hoax.getId())).isNull();
		assertThat(entityManager.find(Token.class, "tok-1")).isNull();
	}
}
