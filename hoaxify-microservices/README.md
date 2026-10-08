# Hoaxify - Mikroservis versiyonu

`ws/` klasöründeki monolith'in aynı özelliklerle mikroservislere bölünmüş hali. Frontend (`frontend/`) iki backend ile de
**hiç değişmeden** çalışır, çünkü gateway monolith'le aynı `/api/v1/...` sözleşmesini sunar.

## Mimari

```
Tarayıcı (React, :5173)
   │  REST + JSON, cookie: hoax-token
   ▼
api-gateway :8000 ──── POST /internal/auth/verify ───► auth-service :8081 ──► Redis (token, TTL)
   │  X-User-Id başlığını gateway ekler                    │   └──► PostgreSQL authdb
   │                                                        │ Feign (giriş cevabı için profil)
   ├──────────────► user-service :8082 ◄────────────────────┘
   │                 │  gRPC sunucusu :9095 ◄──────────┐        └──► PostgreSQL userdb, Redis (cache), disk (resimler)
   │                 │                                  │ gRPC (yazar bilgisi, toplu)
   └──────────────► hoax-service :8083 ─────────────────┘        └──► PostgreSQL hoaxdb (JPA + DAO)

                     RabbitMQ (hoaxify.events, topic exchange)
   auth-service ──user.registered──────────► user-service (profil oluştur), notification-service (aktivasyon maili)
   auth-service ──user.password-reset-requested──► notification-service
   user-service ──user.deleted─────────────► auth-service (hesap + oturumlar), hoax-service (hoax'lar)
   3 deneme de başarısız olan mesaj ─► hoaxify.events.dlx ─► hoaxify.dead-letter (DLQ)

config-server :8071 → tüm servislerin ayarları (config-server/src/main/resources/config/*.yml)
```

| Modül | Tür | Ne yapar |
|---|---|---|
| `common` | kütüphane | Olaylar (record), `ApiError`, başlık sabitleri. Framework'e bağımlı değil. |
| `common-web` | starter | Servlet servislerinin ortak hata yönetimi, i18n, `@CurrentUserId`, `DtoPage` (auto-configuration) |
| `common-messaging` | starter | RabbitMQ exchange, JSON dönüştürücü, dead letter queue (auto-configuration) |
| `grpc-contract` | kütüphane | `user_query_service.proto` + ondan üretilen Java kodu |
| `config-server` | altyapı | Spring Cloud Config (native) |
| `api-gateway` | altyapı | Spring Cloud Gateway (WebFlux): routing, kimlik doğrulama |
| `auth-service` | domain | Hesap, aktivasyon, şifre sıfırlama, oturum |
| `user-service` | domain | Profil, resim; REST + gRPC sunucusu |
| `hoax-service` | domain | Hoax, etiket, istatistik (DAO); gRPC istemcisi |
| `notification-service` | alt servis | Olay dinler, mail gönderir (DB'si yok) |

## Çalıştırma (lokal, debug için)

```bash
cd hoaxify-microservices
cp .env.example .env            # ilk seferde, şifreleri doldur
docker compose up -d            # PostgreSQL (3 DB), RabbitMQ, Redis
./gradlew bootJar

# Her biri ayrı terminalde, MODÜL KLASÖRÜNDEN (config-server önce):
(cd config-server && java -jar build/libs/config-server-0.0.1.jar)
(cd user-service && java -jar build/libs/user-service-0.0.1.jar)
(cd auth-service && java -jar build/libs/auth-service-0.0.1.jar)
(cd hoax-service && java -jar build/libs/hoax-service-0.0.1.jar)
(cd notification-service && java -jar build/libs/notification-service-0.0.1.jar)
(cd api-gateway && java -jar build/libs/api-gateway-0.0.1.jar)

cd ../frontend && npm run dev:ms   # frontend -> gateway (:8000)
```

VS Code: Run and Debug panelinde `MS: config-server`'ı başlat, sonra `MS: tüm servisler`. Breakpoint'ler çalışır.

- Giriş: `user1@mail.com` … `user10@mail.com` / `P4ssword`
- Kayıt / şifre sıfırlama linkleri notification-service logunda `[DEV MAIL]` satırında
- RabbitMQ arayüzü: http://localhost:15672 (kullanıcı ve şifre `.env` içinde)
- Swagger: http://localhost:8081/swagger-ui.html (auth), :8082 (user), :8083 (hoax)

## Çalıştırma (hepsi Docker'da)

```bash
./gradlew bootJar
docker compose --profile apps up -d --build   # sadece gateway (8000) dışarıya açık
```

## Testler

```bash
./gradlew test      # Docker gerekmez; unit testler + in-process gRPC testleri
```

## Önerilen okuma sırası (bir isteğin yolculuğu)

1. **Kayıt:** `auth-service/.../AccountServiceImpl.register` → `messaging/DomainEventRelay` (commit sonrası RabbitMQ)
   → `user-service/.../UserRegisteredListener` ve `notification-service/.../NotificationListener`
2. **Giriş:** `auth-service/.../AuthServiceImpl.login` → `client/UserServiceClient` (Feign) → `store/impl/RedisTokenStore`
3. **Her istek:** `api-gateway/.../filter/AuthenticationFilter` → `security/AuthServiceTokenVerifier` → `X-User-Id`
   → servisteki `@CurrentUserId` (`common-web/.../CurrentUserIdArgumentResolver`)
4. **Akış (gRPC):** `hoax-service/.../HoaxServiceImpl.getHoaxes` → `client/impl/GrpcUserQueryClient`
   → `grpc-contract/src/main/proto/user_query_service.proto` → `user-service/.../grpc/UserQueryGrpcService`
5. **Hesap silme (olaylarla):** `user-service/.../UserProfileServiceImpl.deleteUser` → `user.deleted`
   → `auth-service/.../UserDeletedListener` + `hoax-service/.../UserDeletedListener`

## Monolith'le karşılaştırma (mülakat için)

| Konu | Monolith (`ws/`) | Mikroservis |
|---|---|---|
| Veritabanı | Tek DB, JOIN ve FK | Servis başına DB; JOIN yerine gRPC ile "API composition" |
| Hoax silinirken kullanıcı | `cascade = REMOVE` (tek transaction) | `user.deleted` olayı; eventual consistency |
| Kayıt + mail | Tek transaction; mail patlarsa rollback | Olay; mail servisi kapalıyken mesaj kuyrukta bekler |
| Kimlik doğrulama | `TokenFilter` her istekte | Gateway'de bir kez; servisler `X-User-Id`'ye güvenir |
| `@PreAuthorize` | Spring Security | Serviste sahiplik kontrolü (`checkOwnership`) |
| Token | PostgreSQL tablosu | Redis (TTL ile otomatik silinir) |

## Bilinçli olarak eksik bırakılanlar (sonraki adımlar)

- **Transactional Outbox:** Commit olup RabbitMQ o an kapalıysa olay kaybolur (bkz. `DomainEventRelay` yorumu).
- **Circuit breaker (Resilience4j):** Feign ve gRPC çağrılarında timeout var, devre kesici yok.
- **Service discovery:** Adresler config'ten geliyor; Kubernetes'te servis adları DNS ile çözülür.
- **Dağıtık izleme (tracing):** Micrometer Tracing + Zipkin ile tek isteği tüm servislerde izlemek.
- **Resim depolama:** Birden çok user-service kopyası için yerel disk yerine S3 / MinIO gerekir.
