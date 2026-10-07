package com.hoaxify.ws.dao;

import static com.hoaxify.ws.support.TestData.newUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.hoaxify.ws.dao.impl.HoaxStatsDaoImpl;
import com.hoaxify.ws.dao.impl.UserStatsDaoImpl;
import com.hoaxify.ws.dto.DtoTagStats;
import com.hoaxify.ws.dto.DtoUserStats;
import com.hoaxify.ws.entities.Hoax;
import com.hoaxify.ws.entities.Tag;
import com.hoaxify.ws.entities.User;
import com.hoaxify.ws.support.PostgresTestContainer;

/**
 * DAO testleri MOCK'LA YAZILMAZ: SQL'in doğru olduğunu ancak gerçek veritabanı gösterebilir.
 * Veri JPA ile (TestEntityManager) hazırlanır, DAO ile (düz SQL) okunur. Her ikisi aynı
 * transaction'da çalıştığı için flush() sonrası DAO veriyi görür; test sonunda her şey rollback olur.
 *
 * @DataJpaTest DAO sınıflarını otomatik yüklemez (@Repository olsalar da JPA repository değiller),
 * bu yüzden @Import ile ekliyoruz. functions.sql de bu test context'inde çalışır.
 *
 * Tespit edilen senaryolar:
 *  findTopPosters    : sıralama (sayı desc, isim asc) | limit | hoax'ı olmayan kullanıcı listede yok | profil resmi gelir
 *  countHoaxesOfUser : stored function çağrısı | hoax'ı olmayan kullanıcı -> 0
 *  findLastHoaxAt    : en son tarih | hoax yok -> Optional.empty
 *  findTagUsage      : sayılar doğru | kullanılmayan etiket 0 ile gelir | sıralama
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ UserStatsDaoImpl.class, HoaxStatsDaoImpl.class })
class StatsDaoTest implements PostgresTestContainer {

	@Autowired
	private IUserStatsDao userStatsDao;

	@Autowired
	private IHoaxStatsDao hoaxStatsDao;

	@Autowired
	private TestEntityManager entityManager;

	private User alice;
	private User bobby;
	private User carol;
	private User silent;

	@BeforeEach
	void setUp() {
		alice = entityManager.persist(newUser("alice"));
		bobby = entityManager.persist(newUser("bobby"));
		carol = entityManager.persist(newUser("carol"));
		silent = entityManager.persist(newUser("silent"));
		alice.getProfile().setImage("alice.png");

		Tag java = entityManager.persist(new Tag("java"));
		Tag spring = entityManager.persist(new Tag("spring"));
		entityManager.persist(new Tag("unused"));

		hoax(alice, "a1", java);
		hoax(alice, "a2", java, spring);
		hoax(bobby, "b1", java);
		hoax(bobby, "b2");
		hoax(carol, "c1");
		entityManager.flush();
	}

	private void hoax(User author, String content, Tag... tags) {
		Hoax hoax = new Hoax();
		hoax.setUser(author);
		hoax.setContent(content);
		hoax.getTags().addAll(List.of(tags));
		entityManager.persist(hoax);
	}

	@Test
	void findTopPosters_orderedByCountThenUsername() {
		// Act
		List<DtoUserStats> result = userStatsDao.findTopPosters(10);

		// Assert: alice(2) = bobby(2) -> isim sırası, sonra carol(1); silent (0) listede yok
		assertEquals(3, result.size());
		assertEquals("alice", result.get(0).getUsername());
		assertEquals(2, result.get(0).getHoaxCount());
		assertEquals("alice.png", result.get(0).getImage());
		assertEquals("bobby", result.get(1).getUsername());
		assertEquals("carol", result.get(2).getUsername());
		assertEquals(1, result.get(2).getHoaxCount());
		assertTrue(result.stream().noneMatch(stats -> stats.getUserId().equals(silent.getId())));
	}

	@Test
	void findTopPosters_respectsLimit() {
		// Act
		List<DtoUserStats> result = userStatsDao.findTopPosters(1);

		// Assert
		assertEquals(1, result.size());
		assertEquals(alice.getId(), result.get(0).getUserId());
	}

	@Test
	void countHoaxesOfUser_callsDatabaseFunction() {
		assertEquals(2, userStatsDao.countHoaxesOfUser(alice.getId()));
		assertEquals(1, userStatsDao.countHoaxesOfUser(carol.getId()));
	}

	@Test
	void countHoaxesOfUser_userWithoutHoaxes_shouldReturnZero() {
		assertEquals(0, userStatsDao.countHoaxesOfUser(silent.getId()));
	}

	@Test
	void findLastHoaxAt_success() {
		// Act
		Optional<LocalDateTime> result = userStatsDao.findLastHoaxAt(alice.getId());

		// Assert
		assertTrue(result.isPresent());
		assertFalse(result.get().isAfter(LocalDateTime.now().plusMinutes(1)));
	}

	@Test
	void findLastHoaxAt_noHoaxes_shouldReturnEmpty() {
		assertTrue(userStatsDao.findLastHoaxAt(silent.getId()).isEmpty());
	}

	@Test
	void findTagUsage_includesUnusedTagsWithZero() {
		// Act
		List<DtoTagStats> result = hoaxStatsDao.findTagUsage();

		// Assert
		assertEquals(3, result.size());
		assertEquals(new DtoTagStats("java", 3), result.get(0));
		assertEquals(new DtoTagStats("spring", 1), result.get(1));
		assertEquals(new DtoTagStats("unused", 0), result.get(2));
	}

	@Test
	void findTagUsage_noTags_shouldReturnEmptyList() {
		// Arrange
		entityManager.getEntityManager().createNativeQuery("DELETE FROM hoax_tag").executeUpdate();
		entityManager.getEntityManager().createNativeQuery("DELETE FROM tag").executeUpdate();

		// Act
		List<DtoTagStats> result = hoaxStatsDao.findTagUsage();

		// Assert
		assertTrue(result.isEmpty());
	}
}
