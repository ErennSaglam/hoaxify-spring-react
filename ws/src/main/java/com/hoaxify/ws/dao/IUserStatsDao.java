package com.hoaxify.ws.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.hoaxify.ws.dto.DtoUserStats;

/**
 * DAO (Data Access Object): SQL'i elle yazdığımız veri erişim katmanı.
 *
 * Repository (Spring Data JPA) ile farkı: JPA entity'leri yönetir ve SQL'i kendisi üretir;
 * DAO ise SQL'i tam kontrol eder ve sonucu doğrudan istediğimiz DTO'ya çevirir.
 * Raporlama, toplu işlemler ve stored procedure çağrıları gibi JPA'nın zorlandığı yerlerde kullanılır.
 * Bu projede ikisi yan yana yaşar: CRUD için repository, rapor için DAO.
 */
public interface IUserStatsDao {

	/** En çok hoax paylaşan kullanıcılar (hiç paylaşmayanlar dahil değil) */
	List<DtoUserStats> findTopPosters(int limit);

	/** Veritabanındaki hoaxify_user_hoax_count fonksiyonunu çağırır (stored procedure örneği) */
	long countHoaxesOfUser(Long userId);

	/** Kullanıcının son hoax zamanı; hiç hoax yoksa boş */
	Optional<LocalDateTime> findLastHoaxAt(Long userId);
}
