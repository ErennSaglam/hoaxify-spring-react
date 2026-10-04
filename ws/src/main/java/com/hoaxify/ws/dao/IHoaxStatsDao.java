package com.hoaxify.ws.dao;

import java.util.List;

import com.hoaxify.ws.dto.DtoTagStats;

public interface IHoaxStatsDao {

	/** Etiketler ve kaç hoax'ta kullanıldıkları; kullanılmayan etiketler 0 ile gelir */
	List<DtoTagStats> findTagUsage();
}
