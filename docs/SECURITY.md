# MintStack Finance Portal â€” GÃ¼venlik Rehberi

Bu dokÃ¼man, MintStack Finance Portal projesinin gÃ¼venlik yapÄ±landÄ±rmasÄ±nÄ±, uygulanan kontrolleri ve operasyonel gÃ¼venlik kontrol listesini aÃ§Ä±klar.

---

## 1. Kimlik DoÄŸrulama ve Yetkilendirme

### OAuth2/OIDC (Keycloak 26)

- **Kimlik saÄŸlayÄ±cÄ±**: Keycloak 26 ile OAuth2/OIDC
- **PKCE akÄ±ÅŸÄ±**: S256 code challenge method ile yetkisiz kod deÄŸiÅŸimine karÅŸÄ± koruma
- **JWT doÄŸrulama**: Spring Security OAuth2 Resource Server ile JWK Set URI Ã¼zerinden imza doÄŸrulama
- **Realm**: `mintstack-finance`

### Rol BazlÄ± EriÅŸim KontrolÃ¼ (RBAC)

| Rol | AÃ§Ä±klama | EriÅŸim |
|-----|----------|--------|
| `user` | Standart kullanÄ±cÄ± | Piyasa verisi, portfÃ¶y, watchlist, alertler |
| `admin` | YÃ¶netici | TÃ¼m kullanÄ±cÄ± Ã¶zellikleri + simÃ¼lasyon, admin paneli, rate limit yÃ¶netimi |
| `finance-backend` client rolÃ¼: `api-access` | Backend servis eriÅŸimi | M2M (machine-to-machine) API Ã§aÄŸrÄ±larÄ± |

### Ä°ki FaktÃ¶rlÃ¼ Kimlik DoÄŸrulama (2FA)

- **TOTP**: Keycloak Ã¼zerinden Time-based One-Time Password desteÄŸi
- KullanÄ±cÄ±lar hesap gÃ¼venliÄŸi ayarlarÄ±ndan TOTP etkinleÅŸtirebilir

### LDAP Entegrasyonu

- **OpenLDAP**: Kurumsal kullanÄ±cÄ± dizini ile federasyon
- Keycloak LDAP User Federation ile kullanÄ±cÄ± senkronizasyonu
- LDAP admin/config ÅŸifreleri Docker Secrets ile yÃ¶netilir

---

## 2. API GÃ¼venliÄŸi

### Rate Limiting (Bucket4j)

| KullanÄ±cÄ± Tipi | Ä°stek/Dakika |
|----------------|--------------|
| Anonim (IP bazlÄ±) | 100 |
| KimliÄŸi doÄŸrulanmÄ±ÅŸ kullanÄ±cÄ± | 200 |
| Admin | 500 |

- **Uygulama**: `RateLimitFilter` ile tÃ¼m API isteklerinde
- **Atlanan yollar**: `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`, `/api-docs/**`, `/swagger-ui/**`
- **YanÄ±t**: Limit aÅŸÄ±ldÄ±ÄŸÄ±nda `429 Too Many Requests`; header'larda `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`

### CORS YapÄ±landÄ±rmasÄ±

- **Whitelist**: Sadece tanÄ±mlÄ± origin'lere izin verilir
- **GeliÅŸtirme**: `localhost:3000/3001/3002`, `127.0.0.1:3000/3001/3002`, `localhost:8088`, `127.0.0.1:8088`
- **Ãœretim**: `application-prod.yml` veya ortam deÄŸiÅŸkenleri ile sadece gerÃ§ek domain'ler tanÄ±mlanmalÄ±
- **Metodlar**: GET, POST, PUT, DELETE, OPTIONS
- **Credentials**: `allow-credentials: true` (Ã§erez/token ile kimlik doÄŸrulama iÃ§in)

### GÃ¼venlik BaÅŸlÄ±klarÄ±

| BaÅŸlÄ±k | DeÄŸer |
|--------|-------|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `X-XSS-Protection` | `1; mode=block` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Strict-Transport-Security` (HSTS) | `max-age=31536000; includeSubDomains; preload` |
| `Permissions-Policy` | Kamera, mikrofon, konum vb. devre dÄ±ÅŸÄ± |
| `Content-Security-Policy` | `default-src 'self'`; script/style/font/img/connect kÄ±sÄ±tlamalarÄ± |

---

## 3. Veri GÃ¼venliÄŸi

### AktarÄ±mda Åžifreleme (Encryption in Transit)

- **TLS**: Nginx Ã¼zerinden TLS 1.2 ve TLS 1.3
- **Cipher suite**: ECDHE-ECDSA-AES128-GCM-SHA256, ECDHE-RSA-AES128-GCM-SHA256, ECDHE-ECDSA-AES256-GCM-SHA384, ECDHE-RSA-AES256-GCM-SHA384
- **OCSP Stapling**: Sertifika doÄŸrulama iÃ§in etkin

### Redis Kimlik DoÄŸrulama

- **Parola**: `SPRING_DATA_REDIS_PASSWORD` ile zorunlu kimlik doÄŸrulama
- **Ãœretim**: `redis_password` Docker Secret Ã¼zerinden saÄŸlanÄ±r
- **Komut**: `redis-server --requirepass $(cat /run/secrets/redis_password)`

### Kafka SASL/PLAIN

- **Protokol**: `security.protocol=SASL_PLAINTEXT` veya `SASL_SSL` (Ã¼retimde tercih edilir)
- **Mekanizma**: `sasl.mechanism=PLAIN`
- **JAAS**: `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG` ile kimlik bilgileri

### OpenSearch GÃ¼venliÄŸi

- **Security Plugin**: Etkin
- **Kimlik doÄŸrulama**: `OPENSEARCH_USERNAME`, `OPENSEARCH_PASSWORD` ortam deÄŸiÅŸkenleri
- **HTTPS**: Ãœretimde `OPENSEARCH_SCHEME=https` kullanÄ±lmalÄ±

### VeritabanÄ±

- **PostgreSQL**: Åžifre zorunlu; Ã¼retimde Docker Secrets (`postgres_password`)
- **BaÄŸlantÄ±**: JDBC URL Ã¼zerinden SSL/TLS (Ã¼retimde `sslmode=require` Ã¶nerilir)

---

## 4. AltyapÄ± GÃ¼venliÄŸi

### Docker Secrets (Ãœretim)

Hassas veriler dosya veya Swarm secret olarak yÃ¶netilir:

| Secret | KullanÄ±m |
|--------|----------|
| `postgres_password` | PostgreSQL, Keycloak DB |
| `redis_password` | Redis kimlik doÄŸrulama |
| `keycloak_admin_password` | Keycloak admin |
| `ldap_admin_password` | OpenLDAP admin/config |
| `alpha_vantage_key` | Alpha Vantage API |
| `grafana_admin_password` | Grafana admin |

**Entrypoint**: `backend/docker-entrypoint.sh` ile `*_FILE` pattern'Ä± kullanÄ±larak `/run/secrets/` okunur.

### AÄŸ Segmentasyonu (Ãœretim)

| AÄŸ | Servisler | Ã–zellik |
|----|-----------|---------|
| `data-network` | postgres, redis, opensearch | `internal: true` â€” dÄ±ÅŸ eriÅŸim yok |
| `app-network` | backend, frontend, nginx | Uygulama katmanÄ± |
| `auth-network` | keycloak, openldap | `internal: true` |
| `obs-network` | prometheus, grafana, alertmanager, otel, logstash, kafka | `internal: true` |

### Port Binding (GeliÅŸtirme)

- **PostgreSQL**: `127.0.0.1:5432`
- **Redis**: `127.0.0.1:${REDIS_HOST_PORT:-16379}` (container iÃ§i port: `6379`)
- **LDAP**: `127.0.0.1:389`, `127.0.0.1:636`
- **OpenSearch**: `127.0.0.1:19200`
- **Prometheus**: `127.0.0.1:9090`
- **Alertmanager**: `127.0.0.1:9093`

DÄ±ÅŸ eriÅŸime yalnÄ±zca Nginx (80/443), Frontend (3002) ve Keycloak (8180) portlarÄ± aÃ§Ä±lÄ±r.

### Nginx (Ãœretim)

- **TLS**: 1.2 ve 1.3
- **Rate limiting**: API iÃ§in `30r/s`, login iÃ§in `5r/s` (burst: 50)
- **Actuator**: `/actuator/health` dÄ±ÅŸÄ±ndaki actuator endpoint'leri `403` ile engellenir
- **Swagger/API Docs**: Ãœretimde `404` dÃ¶ner (dokÃ¼mantasyon gizlenir)

### Prometheus Basic Auth

- **web.yml**: bcrypt ile hash'lenmiÅŸ parola
- Ãœretimde `admin` kullanÄ±cÄ±sÄ± iÃ§in gÃ¼Ã§lÃ¼ parola zorunludur
- Hash Ã¼retimi: `htpasswd -nBC 10 admin` veya Python `bcrypt`

---

## 5. CI/CD Guvenligi

Guncel otomatik CI/CD akisi sade tutulur ve harici kirilgan security action'lara bagli degildir.

Otomatik CI kontrolleri:

- Backend `clean verify` ve JaCoCo kalite kapisi.
- Flyway `migrate -> validate`.
- Frontend lint, typecheck, Vitest ve build.
- Docker Compose config validation.

Manuel Docker build workflow'u:

- Backend ve frontend Docker image'larini GitHub runner uzerinde build eder.
- Image'lari registry'ye push etmez.
- Trivy/Cosign gibi harici action'lar bu sade pipeline'dan cikarilmistir.

Production'a cikis planlanirsa image tarama, SBOM/provenance ve imzalama tekrar ayri bir hardening fazinda ele alinmalidir.

---
## 6. GÃ¼venlik Kontrol Listesi

### Kimlik DoÄŸrulama ve Yetkilendirme

- [ ] Keycloak realm ayarlarÄ± gÃ¼ncel (`mintstack-finance`)
- [ ] PKCE S256 frontend'de etkin
- [ ] JWT issuer-uri ve jwk-set-uri doÄŸru yapÄ±landÄ±rÄ±lmÄ±ÅŸ
- [ ] Rol eÅŸlemesi (`realm_access.roles`, `resource_access`) doÄŸru
- [ ] 2FA (TOTP) kritik kullanÄ±cÄ±lar iÃ§in Ã¶nerilir/ zorunlu
- [ ] LDAP federation test edilmiÅŸ (kullanÄ±lÄ±yorsa)

### API GÃ¼venliÄŸi

- [ ] Rate limiting etkin ve limitler uygun
- [ ] CORS origin'leri Ã¼retimde sadece gerÃ§ek domain'ler
- [ ] Security header'lar tÃ¼m yanÄ±tlarda mevcut
- [ ] Swagger/API docs Ã¼retimde kapalÄ± veya IP kÄ±sÄ±tlÄ±

### Veri GÃ¼venliÄŸi

- [ ] Redis parolasÄ± gÃ¼Ã§lÃ¼ ve secret olarak yÃ¶netiliyor
- [ ] Kafka SASL/PLAIN veya SASL_SSL Ã¼retimde etkin
- [ ] OpenSearch gÃ¼venlik eklentisi ve kimlik doÄŸrulama etkin
- [ ] PostgreSQL baÄŸlantÄ±sÄ± Ã¼retimde SSL ile

### AltyapÄ±

- [ ] Docker Secrets oluÅŸturulmuÅŸ ve `secrets/` git'e commit edilmemiÅŸ
- [ ] AÄŸ segmentasyonu Ã¼retim compose'da doÄŸru
- [ ] Port binding geliÅŸtirmede `127.0.0.1` ile sÄ±nÄ±rlÄ±
- [ ] Nginx TLS 1.2/1.3, gÃ¼venli cipher'lar
- [ ] Prometheus basic auth parolasÄ± Ã¼retimde deÄŸiÅŸtirilmiÅŸ

### CI/CD

- [ ] CI backend/frontend/compose kalite kapilari basarili
- [ ] Manuel Docker image build workflow'u basarili
- [ ] Production oncesi image tarama ve imzalama karari ayrica alinmis

### Operasyonel

- [ ] `.env` dosyasÄ± git'e eklenmemiÅŸ (`.env.example` ÅŸablon olarak kullanÄ±lÄ±r)
- [ ] API anahtarlarÄ± (Alpha Vantage, Finnhub) secret olarak yÃ¶netiliyor
- [ ] Loglarda hassas veri (parola, token) yok
- [ ] DÃ¼zenli gÃ¼venlik gÃ¼ncellemeleri ve baÄŸÄ±mlÄ±lÄ±k taramasÄ± yapÄ±lÄ±yor

---

## Ä°lgili DokÃ¼mantasyon

- [ARCHITECTURE.md](./ARCHITECTURE.md) â€” Sistem mimarisi
- [OPERATIONS.md](./OPERATIONS.md) â€” Operasyonel rehber
- [ADR.md](./ADR.md) â€” Mimari kararlar
- [DEPLOYMENT.md](./DEPLOYMENT.md) â€” DaÄŸÄ±tÄ±m rehberi
- [secrets/README.md](../secrets/README.md) â€” Secret yÃ¶netimi
