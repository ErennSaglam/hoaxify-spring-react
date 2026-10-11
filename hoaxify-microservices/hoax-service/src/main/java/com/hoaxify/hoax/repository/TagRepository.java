package com.hoaxify.hoax.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hoaxify.hoax.entities.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {

	List<Tag> findByNameIn(Collection<String> names);

	@Query("select t from Tag t left join t.hoaxes h group by t order by count(h) desc, t.name asc")
	List<Tag> findAllOrderByUsage();
}
