package com.hoaxify.ws.repository;

import static com.hoaxify.ws.support.TestData.newUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.hoaxify.ws.entities.Hoax;
import com.hoaxify.ws.entities.Tag;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.support.PostgresTestContainer;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HoaxAndTagRepositoryTest implements PostgresTestContainer {

	@Autowired
	private HoaxRepository hoaxRepository;
	@Autowired
	private TagRepository tagRepository;
	@Autowired
	private TestEntityManager entityManager;

	private User alice;
	private User bob;
	private Tag java;
	private Tag spring;
	private Tag react;

	@BeforeEach
	void setUp() {
		alice = entityManager.persist(newUser("alice"));
		bob = entityManager.persist(newUser("bob"));
		java = entityManager.persist(new Tag("java"));
		spring = entityManager.persist(new Tag("spring"));
		react = entityManager.persist(new Tag("react"));

		hoax(alice, "a1", java, spring);
		hoax(alice, "a2", java);
		hoax(bob, "b1", java, react);
		entityManager.flush();
		entityManager.clear();
	}

	private Hoax hoax(User author, String content, Tag... tags) {
		Hoax hoax = new Hoax();
		hoax.setUser(author);
		hoax.setContent(content);
		hoax.getTags().addAll(List.of(tags));
		return entityManager.persist(hoax);
	}

	@Test
	void creationTimestamp_isFilledAutomatically() {
		assertThat(hoaxRepository.findAll()).allSatisfy(h -> assertThat(h.getCreatedAt()).isNotNull());
	}

	@Test
	void findByUserId_returnsOnlyThatUsersHoaxes() {
		Page<Hoax> page = hoaxRepository.findByUserId(alice.getId(), PageRequest.of(0, 10));

		assertThat(page.getContent()).extracting(Hoax::getContent).containsExactlyInAnyOrder("a1", "a2");
	}

	@Test
	void findByTagName_usesJoinTable_andCountsCorrectly() {
		Page<Hoax> javaPage = hoaxRepository.findByTagName("java", PageRequest.of(0, 2, Sort.by("content")));

		assertThat(javaPage.getTotalElements()).isEqualTo(3);
		assertThat(javaPage.getContent()).extracting(Hoax::getContent).containsExactly("a1", "a2");
		assertThat(hoaxRepository.findByTagName("react", PageRequest.of(0, 10)).getContent())
				.extracting(Hoax::getContent).containsExactly("b1");
		assertThat(hoaxRepository.findByTagName("unknown", PageRequest.of(0, 10))).isEmpty();
	}

	@Test
	void manyToMany_isReadableFromBothSides() {
		Hoax a1 = hoaxRepository.findAll().stream().filter(h -> h.getContent().equals("a1")).findFirst().orElseThrow();
		assertThat(a1.getTags()).extracting(Tag::getName).containsExactlyInAnyOrder("java", "spring");

		Tag loadedJava = tagRepository.findById(java.getId()).orElseThrow();
		assertThat(loadedJava.getHoaxes()).hasSize(3); // ters taraf (mappedBy = "tags")
	}

	@Test
	void findByNameIn_returnsMatchingTags() {
		assertThat(tagRepository.findByNameIn(Set.of("java", "react", "nope")))
				.extracting(Tag::getName).containsExactlyInAnyOrder("java", "react");
	}

	@Test
	void findAllOrderByUsage_mostUsedFirst_thenAlphabetical() {
		// java: 3, react: 1, spring: 1 -> eşitlikte isme göre
		assertThat(tagRepository.findAllOrderByUsage())
				.extracting(Tag::getName).containsExactly("java", "react", "spring");
	}

	@Test
	void deletingHoax_removesJoinRows_butKeepsTags() {
		Hoax b1 = hoaxRepository.findByUserId(bob.getId(), PageRequest.of(0, 1)).getContent().get(0);

		hoaxRepository.delete(b1);
		entityManager.flush();
		entityManager.clear();

		assertThat(tagRepository.findById(react.getId())).isPresent();
		assertThat(tagRepository.findById(react.getId()).orElseThrow().getHoaxes()).isEmpty();
	}
}
