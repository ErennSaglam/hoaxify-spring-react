package com.hoaxify.user.dto;

/** Profil response'u (monolith'teki DtoUser ile aynı JSON) */
public record DtoUser(Long id, String username, String email, String image, String bio) {
}
