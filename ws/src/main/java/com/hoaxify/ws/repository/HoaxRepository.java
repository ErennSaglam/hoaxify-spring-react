package com.hoaxify.ws.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hoaxify.ws.entities.Hoax;

public interface HoaxRepository extends JpaRepository<Hoax, Long> {

	/*
	 * Yazar ve profili JOIN ile gelir. tags koleksiyonunu buraya eklemiyoruz:
	 * sayfalama + koleksiyon fetch birlikte kullanılırsa Hibernate sayfalamayı bellekte yapar.
	 * tags, default_batch_fetch_size sayesinde tek bir IN sorgusuyla yüklenir.
	 */
	@Override
	@EntityGraph(attributePaths = { "user", "user.profile" })
	Page<Hoax> findAll(Pageable pageable);

	@EntityGraph(attributePaths = { "user", "user.profile" })
	Page<Hoax> findByUserId(Long userId, Pageable pageable);

	// HQL (JPQL): tablo/kolon adları yerine entity ve alan adları kullanılır
	@EntityGraph(attributePaths = { "user", "user.profile" })
	@Query(value = "select h from Hoax h join h.tags t where t.name = :tagName",
			countQuery = "select count(h) from Hoax h join h.tags t where t.name = :tagName")
	Page<Hoax> findByTagName(@Param("tagName") String tagName, Pageable pageable);
}
