package com.hoaxify.common.event;

/**
 * user-service yayınlar -> auth-service hesabı ve oturumları, hoax-service kullanıcının hoax'larını siler.
 * Monolith'teki cascade = REMOVE'un mikroservis karşılığı: veritabanları ayrı olduğu için
 * silme işlemi olaylarla yayılır ("eventual consistency": birkaç milisaniye içinde tutarlı hale gelir).
 */
public record UserDeletedEvent(Long userId) {
}
