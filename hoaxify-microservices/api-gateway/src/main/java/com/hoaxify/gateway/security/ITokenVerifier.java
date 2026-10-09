package com.hoaxify.gateway.security;

import java.util.Optional;

import reactor.core.publisher.Mono;

/**
 * Token -> kullanıcı id. Reaktif dünyada metotlar sonucu hemen değil, ileride gelecek bir değer
 * olarak (Mono) döner; thread beklemeye alınmaz.
 *   Mono.just(Optional.of(5)) : geçerli token, kullanıcı 5
 *   Mono.just(Optional.empty()): geçersiz / süresi dolmuş token
 *   Mono.error(...)           : auth-service'e ulaşılamadı
 */
public interface ITokenVerifier {

	Mono<Optional<Long>> verify(String token);
}
