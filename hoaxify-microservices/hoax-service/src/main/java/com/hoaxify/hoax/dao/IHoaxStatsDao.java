package com.hoaxify.hoax.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.hoaxify.hoax.dto.DtoTagStats;
import com.hoaxify.hoax.dto.UserHoaxCount;

/**
 * Monolith'teki IUserStatsDao + IHoaxStatsDao'nun birleşimi. Fark: users tablosu bu veritabanında
 * olmadığı için SQL'de JOIN users yok; kullanıcı adı ve resmi servis katmanında gRPC ile eklenir.
 */
public interface IHoaxStatsDao {

	List<UserHoaxCount> findTopPosters(int limit);

	List<DtoTagStats> findTagUsage();

	/** Veritabanı fonksiyonu (SimpleJdbcCall) */
	long countHoaxesOfUser(Long userId);

	Optional<LocalDateTime> findLastHoaxAt(Long userId);
}
