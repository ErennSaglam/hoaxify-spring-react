package com.hoaxify.auth.dto;

/** Monolith'teki giriş cevabıyla aynı: { "user": {...}, "token": {...} } */
public record DtoAuthResponse(DtoUser user, DtoToken token) {
}
