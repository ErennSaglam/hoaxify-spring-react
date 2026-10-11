package com.hoaxify.hoax.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Monolith'teki DtoHoax ile aynı JSON; "user" alanı artık iki farklı kaynaktan birleştiriliyor */
public record DtoHoax(Long id, String content, LocalDateTime createdAt, DtoUser user, List<DtoTag> tags) {
}
