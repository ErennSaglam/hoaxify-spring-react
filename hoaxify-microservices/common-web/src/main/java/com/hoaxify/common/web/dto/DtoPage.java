package com.hoaxify.common.web.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Sayfalı response; monolith'teki DtoPage ile aynı JSON (frontend content / first / last okuyor) */
public record DtoPage<T>(
		List<T> content,
		int number,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {

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
