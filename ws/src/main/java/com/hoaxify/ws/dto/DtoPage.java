package com.hoaxify.ws.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sayfalı response. Spring'in PageImpl'ini doğrudan JSON'a çevirmek yerine kendi
 * sınıfımızı dönüyoruz; böylece API sözleşmesi Spring sürümüne bağlı kalmaz.
 * Frontend content / first / last alanlarını kullanıyor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoPage<T> {

	private List<T> content;

	private int number;

	private int size;

	private long totalElements;

	private int totalPages;

	private boolean first;

	private boolean last;

	public static <E, T> DtoPage<T> of(Page<E> page, Function<E, T> mapper) {
		return new DtoPage<>(
				page.getContent().stream().map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}
}
