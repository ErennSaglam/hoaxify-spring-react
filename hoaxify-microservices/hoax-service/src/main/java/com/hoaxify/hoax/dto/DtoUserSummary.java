package com.hoaxify.hoax.dto;

import java.time.LocalDateTime;

public record DtoUserSummary(Long userId, long hoaxCount, LocalDateTime lastHoaxAt) {
}
