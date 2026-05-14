# MintStack Finance Portal — Güvenlik Rehberi

Bu doküman, MintStack Finance Portal projesinin güvenlik yapılandırmasını, uygulanan kontrolleri ve operasyonel güvenlik kontrol listesini açıklar.

---

## 1. Kimlik Doğrulama ve Yetkilendirme

### OAuth2/OIDC (Keycloak 26)

- **Kimlik sağlayıcı**: Keycloak 26 ile OAuth2/OIDC
- **PKCE akışı**: S256 code challenge method ile yetkisiz kod değişimine karşı koruma
- **JWT doğrulama**: Spring Security OAuth2 Resource Server ile JWK Set URI üzerinden imza doğrulama
- **Realm**: `mintstack-finance`

### Rol Bazlı Erişim Kontrolü (RBAC)

| Rol | Açıklama | Erişim |
|-----|----------|--------|
| `user` | Standart kullanıcı | Piyasa verisi, portföy, watchlist, alertler |
| `admin` | Yönetici | Tüm kullanıcı özellikleri + simülasyon, admin paneli, rate limit yönetimi |
| `finance-backend` client rolü: `api-access` | Backend servis erişimi | M2M (machine-to-machine) API çağrıları |

### İki Faktörlü Kimlik Doğrulama (2FA)

- **TOTP**: Keycloak üzerinden Time-based One-Time Password desteği
- Kullanıcılar hesap güvenliği ayarlarından TOTP etkinleştirebilir

### LDAP Entegrasyonu

- **OpenLDAP**: Kurumsal kullanıcı dizini ile federasyon
- Keycloak LDAP User Federation ile kullanıcı senkronizasyonu
- LDAP admin/config şifreleri Docker Secrets ile yönetilir

---

## 2. API Güvenliği

### Rate Limiting (Bucket4j)

| Kullanıcı Tipi | İstek/Dakika |
|----------------|--------------|
| Anonim (IP bazlı) | 100 |
| Kimliği doğrulanmış kullanıcı | 200 |
| Admin | 500 |

- **Uygulama**: `RateLimitFilter` ile tüm API isteklerinde
- **Atlanan yollar**: `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`, `/api-docs/**`, `/swagger-ui/**`
- **Yanıt**: Limit aşıldığında `429 Too Many Requests`; header'larda `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`

### CORS Yapılandırması

- **Whitelist**: Sadece tanımlı origin'lere izin verilir
- **Geliştirme**: `localhost:3000/3001/3002`, `127.0.0.1:3000/3001/3002`, `localhost:8088`, `127.0.0.1:8088`
- **Üretim**: `application-prod.yml` veya ortam değişkenleri ile sadece gerçek domain'ler tanımlanmalı
- **Metodlar**: GET, POST, PUT, DELETE, OPTIONS
- **Credentials**: `allow-credentials: true` (çerez/token ile kimlik doğrulama için)

### Güvenlik Başlıkları

| Başlık | Değer |
|--------|-------|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `X-XSS-Protection` | `1; mode=block` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Strict-Transport-Security` (HSTS) | `max-age=31536000; includeSubDomains; preload` |
| `Permissions-Policy` | Kamera, mikrofon, konum vb. devre dışı |
| `Content-Security-Policy` | `default-src 'self'`; script/style/font/img/connect kısıtlamaları |

---

## 3. Veri Güvenliği

### Aktarımda Şifreleme (Encryption in Transit)

- **TLS**: Nginx üzerinden TLS 1.2 ve TLS 1.3
- **Cipher suite**: ECDHE-ECDSA-AES128-GCM-SHA256, ECDHE-RSA-AES128-GCM-SHA256, ECDHE-ECDSA-AES256-GCM-SHA384, ECDHE-RSA-AES256-GCM-SHA384
- **OCSP Stapling**: Sertifika doğrulama için etkin

### Redis Kimlik Doğrulama

- **Parola**: `SPRING_DATA_REDIS_PASSWORD` ile zorunlu kimlik doğrulama
- **Üretim**: `redis_password` Docker Secret üzerinden sağlanır
- **Komut**: `redis-server --requirepass $(cat /run/secrets/redis_password)`

### Kafka SASL/PLAIN

- **Protokol**: `security.protocol=SASL_PLAINTEXT` veya `SASL_SSL` (üretimde tercih edilir)
- **Mekanizma**: `sasl.mechanism=PLAIN`
- **JAAS**: `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG` ile kimlik bilgileri

### OpenSearch Güvenliği

- **Security Plugin**: Etkin
- **Kimlik doğrulama**: `OPENSEARCH_USERNAME`, `OPENSEARCH_PASSWORD` ortam değişkenleri
- **HTTPS**: Üretimde `OPENSEARCH_SCHEME=https` kullanılmalı

### Veritabanı

- **PostgreSQL**: Şifre zorunlu; üretimde Docker Secrets (`postgres_password`)
- **Bağlantı**: JDBC URL üzerinden SSL/TLS (üretimde `sslmode=require` önerilir)

---

## 4. Altyapı Güvenliği

### Docker Secrets (Üretim)

Hassas veriler dosya veya Swarm secret olarak yönetilir:

| Secret | Kullanım |
|--------|----------|
| `postgres_password` | PostgreSQL, Keycloak DB |
| `redis_password` | Redis kimlik doğrulama |
| `keycloak_admin_password` | Keycloak admin |
| `ldap_admin_password` | OpenLDAP admin/config |
| `alpha_vantage_key` | Alpha Vantage API |
| `grafana_admin_password` | Grafana admin |

**Entrypoint**: `backend/docker-entrypoint.sh` ile `*_FILE` pattern'ı kullanılarak `/run/secrets/` okunur.

### Ağ Segmentasyonu (Üretim)

| Ağ | Servisler | Özellik |
|----|-----------|---------|
| `data-network` | postgres, redis, opensearch | `internal: true` — dış erişim yok |
| `app-network` | backend, frontend, nginx | Uygulama katmanı |
| `auth-network` | keycloak, openldap | `internal: true` |
| `obs-network` | prometheus, grafana, alertmanager, otel, logstash, kafka | `internal: true` |

### Port Binding (Geliştirme)

- **PostgreSQL**: `127.0.0.1:5432`
- **Redis**: `127.0.0.1:6379`
- **LDAP**: `127.0.0.1:389`, `127.0.0.1:636`
- **OpenSearch**: `127.0.0.1:19200`
- **Prometheus**: `127.0.0.1:9090`
- **Alertmanager**: `127.0.0.1:9093`

Dış erişime yalnızca Nginx (80/443), Frontend (3002) ve Keycloak (8180) portları açılır.

### Nginx (Üretim)

- **TLS**: 1.2 ve 1.3
- **Rate limiting**: API için `30r/s`, login için `5r/s` (burst: 50)
- **Actuator**: `/actuator/health` dışındaki actuator endpoint'leri `403` ile engellenir
- **Swagger/API Docs**: Üretimde `404` döner (dokümantasyon gizlenir)

### Prometheus Basic Auth

- **web.yml**: bcrypt ile hash'lenmiş parola
- Üretimde `admin` kullanıcısı için güçlü parola zorunludur
- Hash üretimi: `htpasswd -nBC 10 admin` veya Python `bcrypt`

---

## 5. CI/CD Güvenliği

### Görüntü Tarama (Trivy)

- **Araç**: `aquasecurity/trivy-action@0.24.0`
- **Hedef**: Backend ve frontend Docker görüntüleri
- **Seviye**: `CRITICAL` ve `HIGH` zafiyetler
- **Çıktı**: SARIF formatında GitHub Security tab'ına yüklenir
- **Exit code**: Zafiyet bulunursa pipeline başarısız olur (`exit-code: "1"`)

### Görüntü İmzalama (Cosign)

- **Araç**: `sigstore/cosign-installer@v3` (v2.2.4)
- **İmzalama**: GHCR OIDC ile keyless imzalama (`COSIGN_EXPERIMENTAL=true`)
- **Doğrulama**: İmza doğrulaması pipeline içinde yapılır
- **Kapsam**: Backend ve frontend görüntüleri

### SBOM ve Provenance

- Docker Buildx ile `sbom: true`, `provenance: true` etkin
- Yazılım malzeme listesi ve build provenance metadata üretilir

---

## 6. Güvenlik Kontrol Listesi

### Kimlik Doğrulama ve Yetkilendirme

- [ ] Keycloak realm ayarları güncel (`mintstack-finance`)
- [ ] PKCE S256 frontend'de etkin
- [ ] JWT issuer-uri ve jwk-set-uri doğru yapılandırılmış
- [ ] Rol eşlemesi (`realm_access.roles`, `resource_access`) doğru
- [ ] 2FA (TOTP) kritik kullanıcılar için önerilir/ zorunlu
- [ ] LDAP federation test edilmiş (kullanılıyorsa)

### API Güvenliği

- [ ] Rate limiting etkin ve limitler uygun
- [ ] CORS origin'leri üretimde sadece gerçek domain'ler
- [ ] Security header'lar tüm yanıtlarda mevcut
- [ ] Swagger/API docs üretimde kapalı veya IP kısıtlı

### Veri Güvenliği

- [ ] Redis parolası güçlü ve secret olarak yönetiliyor
- [ ] Kafka SASL/PLAIN veya SASL_SSL üretimde etkin
- [ ] OpenSearch güvenlik eklentisi ve kimlik doğrulama etkin
- [ ] PostgreSQL bağlantısı üretimde SSL ile

### Altyapı

- [ ] Docker Secrets oluşturulmuş ve `secrets/` git'e commit edilmemiş
- [ ] Ağ segmentasyonu üretim compose'da doğru
- [ ] Port binding geliştirmede `127.0.0.1` ile sınırlı
- [ ] Nginx TLS 1.2/1.3, güvenli cipher'lar
- [ ] Prometheus basic auth parolası üretimde değiştirilmiş

### CI/CD

- [ ] Trivy CRITICAL/HIGH zafiyetleri pipeline'ı kırmalı
- [ ] Cosign imzalama başarılı
- [ ] SARIF sonuçları Security tab'da inceleniyor

### Operasyonel

- [ ] `.env` dosyası git'e eklenmemiş (`.env.example` şablon olarak kullanılır)
- [ ] API anahtarları (Alpha Vantage, Finnhub) secret olarak yönetiliyor
- [ ] Loglarda hassas veri (parola, token) yok
- [ ] Düzenli güvenlik güncellemeleri ve bağımlılık taraması yapılıyor

---

## İlgili Dokümantasyon

- [ARCHITECTURE.md](./ARCHITECTURE.md) — Sistem mimarisi
- [OPERATIONS.md](./OPERATIONS.md) — Operasyonel rehber
- [ADR.md](./ADR.md) — Mimari kararlar
- [DEPLOYMENT.md](./DEPLOYMENT.md) — Dağıtım rehberi
- [secrets/README.md](../secrets/README.md) — Secret yönetimi
