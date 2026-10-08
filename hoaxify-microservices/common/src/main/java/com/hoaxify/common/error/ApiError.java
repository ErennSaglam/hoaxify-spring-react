package com.hoaxify.common.error;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Tüm servislerin (ve gateway'in) döndüğü ortak hata formatı. Monolith'tekiyle birebir aynı;
 * frontend status / message / validationErrors alanlarını okuyor.
 *
 * Record: sadece veri taşıyan, değişmez (immutable) sınıf. Getter/constructor/equals otomatik gelir.
 */
@JsonInclude(Include.NON_NULL)
public record ApiError<T>(
		String id,
		int status,
		String message,
		String path,
		Instant timestamp,
		T validationErrors) {
}
