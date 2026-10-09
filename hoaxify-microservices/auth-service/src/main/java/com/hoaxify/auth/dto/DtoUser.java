package com.hoaxify.auth.dto;

/**
 * user-service'in /internal/users/{id} cevabı. Servisler birbirinin DTO sınıfını paylaşmaz
 * (bağımlılık olmasın diye); her servis ihtiyaç duyduğu alanlarla kendi kopyasını tanımlar.
 * JSON alan adları aynı olduğu sürece uyumludur.
 */
public record DtoUser(Long id, String username, String email, String image, String bio) {
}
