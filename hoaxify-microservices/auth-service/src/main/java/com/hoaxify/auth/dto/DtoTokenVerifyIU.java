package com.hoaxify.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Gateway'in token doğrulama isteği. Token URL'de değil gövdede: URL'ler loglara yazılır. */
public record DtoTokenVerifyIU(@NotBlank String token) {
}
