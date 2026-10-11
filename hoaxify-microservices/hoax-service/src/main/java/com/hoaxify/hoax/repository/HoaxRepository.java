package com.hoaxify.hoax.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hoaxify.hoax.entities.Hoax;

public interface HoaxRepository extends JpaRepository<Hoax, Long> {

	Page<Hoax> findByUserId(Long userId, Pageable pageable);

	List<Hoax> findAllByUserId(Long userId);

	@Query(value = "select h from Hoax h join h.tags t where t.name = :tagName",
			countQuery = "select count(h) from Hoax h join h.tags t where t.name = :tagName")
	Page<Hoax> findByTagName(@Param("tagName") String tagName, Pageable pageable);
}
