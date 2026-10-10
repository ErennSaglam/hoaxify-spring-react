package com.hoaxify.user.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hoaxify.user.entities.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

	Page<UserProfile> findByIdNot(Long id, Pageable pageable);

	/** gRPC toplu sorgusu: select ... where id in (?, ?, ?) */
	List<UserProfile> findAllByIdIn(Collection<Long> ids);
}
