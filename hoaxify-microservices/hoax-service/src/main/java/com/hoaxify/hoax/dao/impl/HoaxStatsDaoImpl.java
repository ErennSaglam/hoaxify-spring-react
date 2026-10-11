package com.hoaxify.hoax.dao.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import com.hoaxify.hoax.dao.IHoaxStatsDao;
import com.hoaxify.hoax.dto.DtoTagStats;
import com.hoaxify.hoax.dto.UserHoaxCount;

@Repository
public class HoaxStatsDaoImpl implements IHoaxStatsDao {

	private static final String FIND_TOP_POSTERS = """
			SELECT user_id, COUNT(*) AS hoax_count
			FROM hoax
			GROUP BY user_id
			ORDER BY hoax_count DESC, user_id ASC
			LIMIT :limit
			""";

	private static final String FIND_TAG_USAGE = """
			SELECT t.name AS name, COUNT(ht.hoax_id) AS hoax_count
			FROM tag t
			LEFT JOIN hoax_tag ht ON ht.tag_id = t.id
			GROUP BY t.id, t.name
			ORDER BY hoax_count DESC, t.name ASC
			""";

	private static final String FIND_LAST_HOAX_AT = """
			SELECT MAX(created_at) FROM hoax WHERE user_id = :userId
			""";

	private static final RowMapper<UserHoaxCount> USER_HOAX_COUNT_ROW_MAPPER = (rs, rowNum) -> new UserHoaxCount(
			rs.getLong("user_id"),
			rs.getLong("hoax_count"));

	private static final RowMapper<DtoTagStats> TAG_STATS_ROW_MAPPER = (rs, rowNum) -> new DtoTagStats(
			rs.getString("name"),
			rs.getLong("hoax_count"));

	private final NamedParameterJdbcTemplate jdbcTemplate;

	private final SimpleJdbcCall hoaxCountFunction;

	public HoaxStatsDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.hoaxCountFunction = new SimpleJdbcCall(jdbcTemplate.getJdbcTemplate())
				.withFunctionName("hoaxify_user_hoax_count");
	}

	@Override
	public List<UserHoaxCount> findTopPosters(int limit) {
		return jdbcTemplate.query(FIND_TOP_POSTERS, Map.of("limit", limit), USER_HOAX_COUNT_ROW_MAPPER);
	}

	@Override
	public List<DtoTagStats> findTagUsage() {
		return jdbcTemplate.query(FIND_TAG_USAGE, TAG_STATS_ROW_MAPPER);
	}

	@Override
	public long countHoaxesOfUser(Long userId) {
		Long count = hoaxCountFunction.executeFunction(Long.class, new MapSqlParameterSource("p_user_id", userId));
		return count == null ? 0 : count;
	}

	@Override
	public Optional<LocalDateTime> findLastHoaxAt(Long userId) {
		Timestamp lastHoaxAt = jdbcTemplate.queryForObject(FIND_LAST_HOAX_AT, Map.of("userId", userId),
				Timestamp.class);
		return Optional.ofNullable(lastHoaxAt).map(Timestamp::toLocalDateTime);
	}
}
