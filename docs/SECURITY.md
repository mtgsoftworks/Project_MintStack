# Güvenlik Rehberi

## 1. Güvenlik Mimarisi Genel Bakış

MintStack Finance Portal, çok katmanlı bir güvenlik mimarisi kullanır:

```
┌────────────────────────────────────────────────────────┐
│ Katman 1: Ağ Güvenliği                                │
│ - Nginx tek giriş noktası                              │
│ - İç servis portları 127.0.0.1'e bağlı                │
│ - Docker bridge network izolasyonu                     │
├────────────────────────────────────────────────────────┤
│ Katman 2: Kimlik Doğrulama                             │
│ - Keycloak OAuth2/OIDC (PKCE S256)                     │
│ - JWT access token doğrulama (JWK Set)                 │
│ - 2FA (TOTP) desteği                                   │
│ - LDAP federation                                      │
├────────────────────────────────────────────────────────┤
│ Katman 3: Yetkilendirme                                │
│ - Spring Security ile rol bazlı erişim (USER, ADMIN)   │
│ - WebSocket auth interceptor                           │
│ - Endpoint seviyesinde yetki kontrolü                  │
├────────────────────────────────────────────────────────┤
│ Katman 4: Uygulama Güvenliği                           │
│ - Rate limiting (Bucket4j)                             │
│ - Input validation (Bean Validation)                   │
│ - CORS yapılandırması                                  │
│ - Circuit breaker (Resilience4j)                       │
├────────────────────────────────────────────────────────┤
│ Katman 5: Veri Güvenliği                               │
│ - Secret yönetimi (.env, hiçbir zaman commit edilmez)  │
│ - Kafka SASL/PLAIN doğrulama                           │
│ - OpenSearch security plugin aktif                     │
│ - Redis şifre korumalı                                 │
└────────────────────────────────────────────────────────┘
```

## 2. Secret Yönetimi

### Kurallar

- Secret değerlerini **asla** repoya commit etmeyin
- `.env.example` dosyasını kopyalayarak `.env` oluşturun
- Tüm varsayılan şifreleri güçlü ve benzersiz değerlerle değiştirin
- Ortam bazlı ayrı `.env` dosyaları kullanın (dev, staging, prod)

### Güçlü Secret Üretimi

```bash
# 24 byte (32 karakter) base64
openssl rand -base64 24

# 32 byte (44 karakter) base64
openssl rand -base64 32

# 16 byte hex
openssl rand -hex 16
```

### Yönetilen Secret'lar

| Secret | Kullanım Yeri | Minimum Güç |
|---|---|---|
| `POSTGRES_PASSWORD` | PostgreSQL veritabanı | 24 byte base64 |
| `REDIS_PASSWORD` | Redis cache | 24 byte base64 |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin konsolu | 32 byte base64 |
| `KEYCLOAK_FINANCE_BACKEND_SECRET` | Backend OAuth2 client | 32 byte base64 |
| `KEYCLOAK_ADMIN_USER_PASSWORD` | Uygulama admin kullanıcı | Güçlü şifre |
| `KEYCLOAK_TEST_USER_PASSWORD` | Uygulama test kullanıcı | Güçlü şifre |
| `LDAP_ADMIN_PASSWORD` | OpenLDAP admin | 24 byte base64 |
| `KAFKA_SASL_PASSWORD` | Kafka SASL doğrulama | 24 byte base64 |
| `OPENSEARCH_INITIAL_ADMIN_PASSWORD` | OpenSearch admin | 8+ karakter, karmaşık |
| `GRAFANA_ADMIN_PASSWORD` | Grafana admin | 24 byte base64 |
| `ALPHA_VANTAGE_API_KEY` | Alpha Vantage API | API key |
| `FINNHUB_API_KEY` | Finnhub API | API key |

## 3. Kimlik Doğrulama Detayları

### OAuth2/OIDC Akışı

1. Frontend, Keycloak'a `login-required` ile yönlendirir (PKCE S256)
2. Kullanıcı kimlik bilgilerini girer
3. 2FA aktifse TOTP doğrulaması istenir
4. Keycloak JWT access token + refresh token döner
5. Frontend, API isteklerinde `Authorization: Bearer <token>` gönderir
6. Backend, JWK Set üzerinden JWT imzasını doğrular
7. Token içindeki roller (`realm_access.roles`) ile yetki kontrolü yapılır
8. Token otomatik olarak 60 saniyede bir yenilenir

### Rol Yapısı

| Rol | Yetki |
|---|---|
| `USER` | Portföy yönetimi, piyasa izleme, alarm, izleme listesi, ayarlar |
| `ADMIN` | Tüm USER yetkileri + admin dashboard, kullanıcı yönetimi, simülasyon kontrolü |

## 4. Üretim Güvenlik Kontrol Listesi

### Zorunlu

- [ ] Keycloak admin şifresi ve client secret'lar değiştirildi
- [ ] PostgreSQL, Redis, Kafka, OpenSearch şifreleri güçlü ve benzersiz
- [ ] TLS/HTTPS aktif (Nginx SSL termination)
- [ ] İç servis portları dışarıya kapalı (127.0.0.1 binding)
- [ ] `.env` dosyası `.gitignore`'da
- [ ] 2FA aktif (`docs/KEYCLOAK_2FA_SETUP.md`)
- [ ] Varsayılan admin şifreleri değiştirildi

### Önerilen

- [ ] Secret rotasyonu periyodik (her 90 gün)
- [ ] Güvenlik güncellemeleri düzenli uygulanıyor
- [ ] Alarm eşikleri aktif (CPU, memory, hata oranları)
- [ ] Audit log izleme açık
- [ ] Backup rutini otomatik ve test edilmiş
- [ ] Rate limiting yapılandırması gözden geçirildi
- [ ] CORS origin'leri yalnızca beklenen domain'lerle sınırlı

## 5. Sık Yapılan Güvenlik Hataları

| Hata | Risk | Çözüm |
|---|---|---|
| Varsayılan şifrelerle canlı ortama çıkmak | 🔴 Kritik | `.env.example` dışında asla varsayılan değer |
| Dışarıya açık DB/Redis portu bırakmak | 🔴 Kritik | `127.0.0.1:port` binding |
| SSL doğrulamasını kalıcı olarak kapatmak | 🟠 Yüksek | Dev ortamında geçici, prod'da kesinlikle açık |
| `.env` dosyasını repoya commit etmek | 🔴 Kritik | `.gitignore` kontrolü |
| Token'ı local storage'da saklamak | 🟠 Yüksek | Keycloak JS handle eder (memory-based) |
| Rate limiting'i devre dışı bırakmak | 🟡 Orta | Bucket4j her zaman aktif |
