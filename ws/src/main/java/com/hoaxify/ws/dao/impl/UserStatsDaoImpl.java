package com.hoaxify.ws.dao.impl;

import java.sql.ResultSet;
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

import com.hoaxify.ws.dao.IUserStatsDao;
import com.hoaxify.ws.dto.DtoUserStats;

/**
 * @Repository: bu bir Spring bean'i olur VE JDBC hataları (SQLException) Spring'in
 * DataAccessException hiyerarşisine çevrilir. JpaRepository arayüzlerinde gereksizdi,
 * elle yazılan DAO sınıflarında ise tam da bu yüzden kullanılır.
 */
@Repository
public class UserStatsDaoImpl implements IUserStatsDao {

	/*
	 * SQL'ler sabit olarak sınıfın başında: okuması, DBA'ya göstermesi ve test etmesi kolay.
	 * ":limit" gibi isimli parametreler SQL injection'a karşı güvenlidir (PreparedStatement'a dönüşür);
	 * SQL'i asla string birleştirerek kurmayız.
	 */
	private static final String FIND_TOP_POSTERS = """
			SELECT u.id          AS user_id,
			       u.username    AS username,
			       p.image       AS image,
			       COUNT(h.id)   AS hoax_count
			FROM users u
			JOIN hoax h              ON h.user_id = u.id
			LEFT JOIN user_profile p ON p.id = u.profile_id
			GROUP BY u.id, u.username, p.image
			ORDER BY hoax_count DESC, u.username ASC
			LIMIT :limit
			""";

	private static final String FIND_LAST_HOAX_AT = """
			SELECT MAX(created_at) FROM hoax WHERE user_id = :userId
			""";

	/** ResultSet'in bir satırını DTO'ya çeviren kural. Lambda yerine sabit: yeniden kullanılabilir ve test edilebilir. */
	private static final RowMapper<DtoUserStats> USER_STATS_ROW_MAPPER = (ResultSet rs, int rowNum) -> new DtoUserStats(
			rs.getLong("user_id"),
			rs.getString("username"),
			rs.getString("image"),
			rs.getLong("hoax_count"));

	private final NamedParameterJdbcTemplate jdbcTemplate;

	private final SimpleJdbcCall hoaxCountFunction;

	/*
	 * SimpleJdbcCall'ı constructor'da bir kez hazırlıyoruz (thread-safe, tekrar kullanılabilir).
	 * Ek iş yaptığımız için constructor'ı Lombok'a bırakmadık.
	 */
	public UserStatsDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.hoaxCountFunction = new SimpleJdbcCall(jdbcTemplate.getJdbcTemplate())
				.withFunctionName("hoaxify_user_hoax_count");
	}

	@Override
	public List<DtoUserStats> findTopPosters(int limit) {
		return jdbcTemplate.query(FIND_TOP_POSTERS, Map.of("limit", limit), USER_STATS_ROW_MAPPER);
	}

	@Override
	public long countHoaxesOfUser(Long userId) {
		Long count = hoaxCountFunction.executeFunction(Long.class,
				new MapSqlParameterSource("p_user_id", userId));
		return count == null ? 0 : count;
	}

	@Override
	public Optional<LocalDateTime> findLastHoaxAt(Long userId) {
		Timestamp lastHoaxAt = jdbcTemplate.queryForObject(FIND_LAST_HOAX_AT, Map.of("userId", userId),
				Timestamp.class);
		return Optional.ofNullable(lastHoaxAt).map(Timestamp::toLocalDateTime);
	}
}
