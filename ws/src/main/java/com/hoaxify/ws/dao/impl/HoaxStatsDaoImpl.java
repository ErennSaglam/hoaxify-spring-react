package com.hoaxify.ws.dao.impl;

import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.hoaxify.ws.dao.IHoaxStatsDao;
import com.hoaxify.ws.dto.DtoTagStats;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HoaxStatsDaoImpl implements IHoaxStatsDao {

	private static final String FIND_TAG_USAGE = """
			SELECT t.name            AS name,
			       COUNT(ht.hoax_id) AS hoax_count
			FROM tag t
			LEFT JOIN hoax_tag ht ON ht.tag_id = t.id
			GROUP BY t.id, t.name
			ORDER BY hoax_count DESC, t.name ASC
			""";

	private static final RowMapper<DtoTagStats> TAG_STATS_ROW_MAPPER = (rs, rowNum) -> new DtoTagStats(
			rs.getString("name"),
			rs.getLong("hoax_count"));

	private final NamedParameterJdbcTemplate jdbcTemplate;

	@Override
	public List<DtoTagStats> findTagUsage() {
		return jdbcTemplate.query(FIND_TAG_USAGE, TAG_STATS_ROW_MAPPER);
	}
}
