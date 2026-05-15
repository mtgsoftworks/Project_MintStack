# MintStack Finance Portal - DaÄŸÄ±tÄ±m Rehberi

Bu dokÃ¼man, MintStack Finance Portal uygulamasÄ±nÄ±n geliÅŸtirme ve Ã¼retim ortamlarÄ±nda nasÄ±l daÄŸÄ±tÄ±lacaÄŸÄ±nÄ± aÃ§Ä±klar.

## Ä°Ã§indekiler

1. [Gereksinimler](#1-gereksinimler)
2. [Ortam DeÄŸiÅŸkenleri](#2-ortam-deÄŸiÅŸkenleri)
3. [Development OrtamÄ±](#3-development-ortamÄ±)
4. [Production DaÄŸÄ±tÄ±mÄ±](#4-production-daÄŸÄ±tÄ±mÄ±)
5. [CI/CD Pipeline](#5-cicd-pipeline)
6. [Yedekleme ve Geri YÃ¼kleme](#6-yedekleme-ve-geri-yÃ¼kleme)
7. [Monitoring ve Alerting](#7-monitoring-ve-alerting)
8. [Sorun Giderme](#8-sorun-giderme)

---

## 1. Gereksinimler

### DonanÄ±m Gereksinimleri

| Ortam | CPU | RAM | Disk |
|-------|-----|-----|------|
| **Full Stack (docker-compose.yml)** | Min. 4 CPU | Min. 8 GB | 20 GB+ |
| **Lightweight (docker-compose.light.yml)** | Min. 2 CPU | Min. 4 GB | 10 GB+ |
| **Production (docker-compose.prod.yml)** | Min. 4 CPU | Min. 8 GB | 50 GB+ |

### YazÄ±lÄ±m Gereksinimleri

- **Docker** 24+ (Docker Engine)
- **Docker Compose** v2+ (Compose V2 plugin)
- **Git** (kaynak kodu Ã§ekmek iÃ§in)

### Kurulum KontrolÃ¼

```bash
docker --version          # Docker 24+
docker compose version     # Docker Compose v2+
```

---

## 2. Ortam DeÄŸiÅŸkenleri

`.env.example` dosyasÄ±nÄ± `.env` olarak kopyalayÄ±n ve deÄŸerleri doldurun:

```bash
cp .env.example .env
```

### Ortam DeÄŸiÅŸkenleri Listesi

| Kategori | DeÄŸiÅŸken | AÃ§Ä±klama |
|----------|----------|----------|
| **PostgreSQL** | `POSTGRES_DB` | VeritabanÄ± adÄ± (varsayÄ±lan: mintstack_finance) |
| | `POSTGRES_USER` | VeritabanÄ± kullanÄ±cÄ±sÄ± |
| | `POSTGRES_PASSWORD` | Åžifre (Ã¼ret: `openssl rand -base64 24`) |
| **Redis** | `REDIS_PASSWORD` | Redis ÅŸifresi (Ã¼ret: `openssl rand -base64 24`) |
| **Keycloak** | `KEYCLOAK_ADMIN` | Admin kullanÄ±cÄ± adÄ± |
| | `KEYCLOAK_ADMIN_PASSWORD` | Admin ÅŸifresi |
| | `KEYCLOAK_FINANCE_BACKEND_SECRET` | Backend client secret |
| **OpenLDAP** | `LDAP_ORGANISATION` | Organizasyon adÄ± |
| | `LDAP_DOMAIN` | LDAP domain |
| | `LDAP_ADMIN_PASSWORD` | LDAP admin ÅŸifresi |
| **Kafka** | `KAFKA_CLUSTER_ID` | KRaft cluster ID |
| | `KAFKA_SASL_PASSWORD` | SASL ÅŸifresi |
| **OpenSearch** | `OPENSEARCH_INITIAL_ADMIN_PASSWORD` | Admin ÅŸifresi (min 8 karakter) |
| **Grafana** | `GRAFANA_ADMIN_PASSWORD` | Grafana admin ÅŸifresi |
| **Spring Boot** | `SPRING_PROFILES_ACTIVE` | `dev` veya `prod` |
| | `ALPHA_VANTAGE_API_KEY` | Alpha Vantage API anahtarÄ± |
| | `FINNHUB_API_KEY` | Finnhub API anahtarÄ± |
| | `APP_EXTERNAL_API_TEFAS_ENABLED` | TEFAS fon adapter anahtarÄ±; API key gerekmez |
| | `APP_EXTERNAL_API_FINTABLES_ENABLED` | Fintables policy switch; gÃ¼venli varsayÄ±lan `false` |
| | `APP_NEWS_LLM_*` | RSS haber LLM enrichment endpoint/model/key ayarlarÄ± |
| **Frontend** | `VITE_API_URL` | API base URL |
| | `VITE_KEYCLOAK_URL` | Keycloak URL |
| | `VITE_KEYCLOAK_REALM` | Keycloak realm |
| | `VITE_KEYCLOAK_CLIENT_ID` | Frontend client ID |
| | `VITE_WS_URL` | WebSocket URL |

### Ã–rnek .env (GeliÅŸtirme)

```env
POSTGRES_DB=mintstack_finance
POSTGRES_USER=mintstack
POSTGRES_PASSWORD=your_secure_password

REDIS_PASSWORD=your_redis_password
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=your_keycloak_password
KEYCLOAK_FINANCE_BACKEND_SECRET=your_backend_secret

LDAP_ORGANISATION=MintStack
LDAP_DOMAIN=mintstack.local
LDAP_ADMIN_PASSWORD=your_ldap_password

KAFKA_SASL_PASSWORD=your_kafka_password
OPENSEARCH_INITIAL_ADMIN_PASSWORD=YourSecurePass123!
GRAFANA_ADMIN_PASSWORD=admin

SPRING_PROFILES_ACTIVE=dev
ALPHA_VANTAGE_API_KEY=your_alpha_vantage_key
FINNHUB_API_KEY=your_finnhub_key

VITE_API_URL=http://localhost:8088/api/v1
VITE_WS_URL=http://localhost:8088/ws
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=mintstack-finance
VITE_KEYCLOAK_CLIENT_ID=finance-frontend
```

---

## 3. Development OrtamÄ±

### 3.1 Full Stack (TÃ¼m Servisler)

16 servis: postgres, redis, keycloak, openldap, kafka, opensearch, opensearch-dashboards, logstash, otel-collector, prometheus, grafana, alertmanager, backend, frontend, nginx.

```bash
# .env dosyasÄ±nÄ± hazÄ±rlayÄ±n
cp .env.example .env
# .env iÃ§indeki deÄŸerleri dÃ¼zenleyin

# TÃ¼m servisleri baÅŸlat
docker compose up -d

# LoglarÄ± izle
docker compose logs -f backend
```

**EriÅŸim NoktalarÄ±:**

| Servis | URL | Port |
|--------|-----|------|
| Uygulama (Nginx) | http://localhost:8088 | 8088 |
| Keycloak | http://localhost:8180 | 8180 |
| Grafana | http://localhost:13030 | 13030 |
| Prometheus | http://localhost:9090 | 9090 |
| Alertmanager | http://localhost:9093 | 9093 |
| OpenSearch Dashboards | http://localhost:15601 | 15601 |
| PostgreSQL | localhost:5432 | 5432 |
| Redis | localhost:16379 | 6379 |

### 3.2 Lightweight (Minimal Ortam)

~4 GB RAM yeterli. Kafka, OpenSearch, Logstash, Prometheus, Grafana, Alertmanager, OTEL hariÃ§.

```bash
docker compose -f docker-compose.light.yml up -d
```

**Light modda devre dÄ±ÅŸÄ± Ã¶zellikler:**
- Kafka mesajlaÅŸma
- OpenSearch log aggregation
- OpenTelemetry tracing
- Prometheus/Grafana monitoring

**EriÅŸim:** http://localhost:8088

### 3.3 Servisleri Durdurma

```bash
# Full stack
docker compose down

# Light stack
docker compose -f docker-compose.light.yml down

# Volume'larÄ± da silmek iÃ§in
docker compose down -v
```

---

## 4. Production DaÄŸÄ±tÄ±mÄ±

### 4.1 Ã–n HazÄ±rlÄ±k

#### Docker Secrets OluÅŸturma

Production ortamÄ±nda hassas veriler Docker Secrets ile yÃ¶netilir. `secrets/` klasÃ¶rÃ¼nde aÅŸaÄŸÄ±daki dosyalarÄ± oluÅŸturun:

```bash
mkdir -p secrets

# Her secret iÃ§in ayrÄ± dosya (iÃ§erik sadece deÄŸer, satÄ±r sonu yok)
echo -n "your_postgres_password" > secrets/postgres_password.txt
echo -n "your_redis_password" > secrets/redis_password.txt
echo -n "your_keycloak_admin_password" > secrets/keycloak_admin_password.txt
echo -n "your_alpha_vantage_api_key" > secrets/alpha_vantage_key.txt
echo -n "your_grafana_password" > secrets/grafana_admin_password.txt
echo -n "your_ldap_password" > secrets/ldap_admin_password.txt

# .gitignore'a ekleyin (zaten ekli olmalÄ±)
echo "secrets/" >> .gitignore
```

#### SSL SertifikalarÄ±

Nginx production konfigÃ¼rasyonu HTTPS kullanÄ±r. `nginx/ssl/` klasÃ¶rÃ¼ne sertifikalarÄ± yerleÅŸtirin:

```
nginx/ssl/
â”œâ”€â”€ fullchain.pem   # Let's Encrypt veya CA sertifikasÄ±
â””â”€â”€ privkey.pem    # Ã–zel anahtar
```

Let's Encrypt ile Ã¶rnek:

```bash
# Certbot ile sertifika al
certbot certonly --standalone -d yourdomain.com
# Sertifikalar: /etc/letsencrypt/live/yourdomain.com/
cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem nginx/ssl/
cp /etc/letsencrypt/live/yourdomain.com/privkey.pem nginx/ssl/
```

### 4.2 AÄŸ Segmentasyonu

Production compose dosyasÄ± dÃ¶rt ayrÄ± aÄŸ kullanÄ±r:

| AÄŸ | Servisler | AÃ§Ä±klama |
|----|-----------|----------|
| **data-network** | postgres, redis, opensearch | Veri katmanÄ± (internal) |
| **app-network** | backend, frontend, nginx | Uygulama katmanÄ± |
| **auth-network** | keycloak, openldap | Kimlik doÄŸrulama (internal) |
| **obs-network** | prometheus, grafana, alertmanager, kafka, logstash, otel-collector | Observability (internal) |

### 4.3 Production Ortam DeÄŸiÅŸkenleri

`.env` dosyasÄ±nda production deÄŸerlerini ayarlayÄ±n:

```env
SPRING_PROFILES_ACTIVE=prod
KC_HOSTNAME=yourdomain.com

VITE_API_URL=https://yourdomain.com/api/v1
VITE_WS_URL=wss://yourdomain.com/ws
VITE_KEYCLOAK_URL=https://yourdomain.com/auth
```

### 4.4 DaÄŸÄ±tÄ±m

```bash
# Production compose ile baÅŸlat
docker compose -f docker-compose.prod.yml up -d

# Sadece uygulama katmanÄ±nÄ± gÃ¼ncellemek iÃ§in
docker compose -f docker-compose.prod.yml up -d --build backend frontend nginx
```

### 4.5 Production Ã–zellikleri

- **Backend replicas:** 2 instance (yÃ¼k dengeleme)
- **HTTPS:** Nginx 80â†’443 yÃ¶nlendirmesi, TLS 1.2/1.3
- **GÃ¼venlik baÅŸlÄ±klarÄ±:** HSTS, X-Frame-Options, CSP, vb.
- **Rate limiting:** API 30 req/s, login 5 req/s
- **Swagger/API docs:** Production'da devre dÄ±ÅŸÄ± (404)

---

## 5. CI/CD Pipeline

### 5.1 CI Pipeline (`.github/workflows/ci.yml`)

**Tetikleyici:** `main`, `develop` push/PR ve manuel `workflow_dispatch`.

| Job | Aciklama |
|-----|----------|
| **backend** | `./mvnw -B -ntp clean verify`, JaCoCo kontrolu, Flyway `migrate -> validate` |
| **frontend** | `npm ci`, lint, typecheck, Vitest, production build |
| **compose** | `docker-compose.yml`, `docker-compose.light.yml`, `docker-compose.prod.yml` config dogrulama |

Guncel CI notlari:

- Otomatik CI yalnizca ana kalite kapilarini calistirir.
- Backend adimi unit test, paketleme ve JaCoCo kontrolunu birlikte calistirir.
- JaCoCo kalite kapisi: line `0.50`, branch `0.35`.
- Flyway bos CI PostgreSQL uzerinde once `migrate`, sonra `validate` calistirir. Bos DB'de tek basina `validate` pending migration hatasi verir.
- Frontend adimi lint, typecheck, Vitest ve production build kontrollerini kapsar.
- Compose validation dev/light/prod compose dosyalarinin config olarak parse edilebildigini dogrular.

### 5.2 Manuel Docker Build (`.github/workflows/deploy.yml`)

**Tetikleyici:** yalnizca manuel `workflow_dispatch`.

Bu workflow deploy yapmaz, registry'ye image push etmez ve harici security action kullanmaz. Amaci sadece backend ve frontend Docker image'larinin GitHub runner uzerinde build edilebildigini dogrulamaktir.

| Job | Aciklama |
|-----|----------|
| **docker-build** | Backend ve frontend Docker image build + image inspect |

Kaldirilan gereksiz/kirilgan adimlar:

- GHCR image publish.
- Trivy scan action.
- Cosign image signing.
- SSH staging/production deploy.
- Release asset paketleme.
- Otomatik E2E ve Docker smoke job'lari.

Bu sade yapi proje kapanisi ve juri sunumu icin daha stabil bir CI/CD zemini saglar. Production deployment gerekiyorsa ayrica secrets, registry policy, image scan ve imzalama kararlarinin tekrar netlestirilmesi gerekir.

---

## 6. Yedekleme ve Geri YÃ¼kleme

### 6.1 PostgreSQL Yedekleme

**Bash (Linux/macOS):**

```bash
# GeliÅŸtirme ortamÄ±, 7 gÃ¼n saklama
./docker/backup/pg_backup.sh dev ./backups 7

# Production ortamÄ±, 14 gÃ¼n saklama
./docker/backup/pg_backup.sh prod ./backups 14
```

**PowerShell (Windows):**

```powershell
.\docker\backup\pg_backup.ps1 -Mode dev -BackupDir .\backups -RetentionDays 7
.\docker\backup\pg_backup.ps1 -Mode prod -BackupDir .\backups -RetentionDays 14
```

> **Not:** Bash script `.sql.gz` (sÄ±kÄ±ÅŸtÄ±rÄ±lmÄ±ÅŸ), PowerShell script `.sql` (sÄ±kÄ±ÅŸtÄ±rÄ±lmamÄ±ÅŸ) Ã¼retir.

**Parametreler:**
- `MODE`: `dev` veya `prod` (container adÄ±na gÃ¶re)
- `BACKUP_DIR`: Yedek dosyalarÄ±nÄ±n dizini
- `RETENTION_DAYS`: Bu sÃ¼reden eski yedekler silinir

**Ã‡Ä±ktÄ±:** `./backups/dev_mintstack_finance_20260307_120000.sql.gz`

### 6.2 PostgreSQL Geri YÃ¼kleme

**Bash:**

```bash
./docker/backup/pg_restore.sh dev ./backups/dev_mintstack_finance_20260307_120000.sql.gz
./docker/backup/pg_restore.sh prod ./backups/prod_mintstack_finance_20260307_120000.sql.gz
```

**PowerShell:**

```powershell
# Bash ile oluÅŸturulmuÅŸ .sql.gz dosyasÄ± iÃ§in (gunzip pipe ile)
# PowerShell restore script sadece .sql dosyalarÄ±nÄ± destekler; .gz iÃ§in bash kullanÄ±n
.\docker\backup\pg_restore.ps1 -Mode dev -BackupFile .\backups\dev_mintstack_finance_20260307_120000.sql
```

**UyarÄ±:** Geri yÃ¼kleme mevcut veriyi Ã¼zerine yazar. Production'da Ã¶nce test ortamÄ±nda deneyin.

### 6.3 ZamanlanmÄ±ÅŸ Yedekleme (Cron)

```cron
# Her gÃ¼n 02:00'da production yedeÄŸi
0 2 * * * cd /opt/mintstack && ./docker/backup/pg_backup.sh prod /opt/backups 14
```

---

## 7. Monitoring ve Alerting

### 7.1 Prometheus

**URL:** http://localhost:9090 (dev) / internal (prod)

**Scrape hedefleri:**
- `backend:8080/actuator/prometheus` â€” Spring Boot metrikleri
- `otel-collector:8889` â€” OpenTelemetry metrikleri
- `localhost:9090` â€” Prometheus self-monitoring

**KonfigÃ¼rasyon:** `docker/prometheus/prometheus.yml`

### 7.2 Grafana

**URL:** http://localhost:13030 (dev)

**VarsayÄ±lan giriÅŸ:** admin / `GRAFANA_ADMIN_PASSWORD`

**Datasource'lar (otomatik provisioning):**
- Prometheus: `http://prometheus:9090`
- Alertmanager: `http://alertmanager:9093`

**Dashboard'lar:** `docker/grafana/dashboards/` (api-dashboard, circuit-breaker-dashboard, jvm-dashboard)

### 7.3 Alertmanager

**URL:** http://localhost:9093 (dev)

**KonfigÃ¼rasyon:** `docker/alertmanager/alertmanager.yml`

**Alert kurallarÄ±** (`docker/prometheus/alerts.yml`):

| Alert | Severity | AÃ§Ä±klama |
|-------|----------|----------|
| HighErrorRate | critical | 5xx hata oranÄ± > %10 |
| ApplicationDown | critical | Backend yanÄ±t vermiyor |
| DBConnectionPoolExhausted | critical | Connection pool > %90 |
| CircuitBreakerOpen | warning | Circuit breaker aÃ§Ä±k |
| HighLatency | warning | P95 latency > 2 sn |
| HighHeapMemory | warning | JVM heap > %85 |
| ExternalAPISlowResponse | warning | Harici API yavaÅŸ |
| ExternalAPIHighFailureRate | warning | Harici API hata oranÄ± > %30 |
| KafkaConsumerLag | warning | Consumer lag > 1000 |

**Bildirim kanallarÄ±:**
- Webhook: `http://backend:8080/api/v1/admin/webhooks/alerts`
- E-posta: `ALERTMANAGER_CRITICAL_EMAIL`, `ALERTMANAGER_WARNING_EMAIL` (ops-team, dev-team)

**Ortam deÄŸiÅŸkenleri (Alertmanager iÃ§in):**
- `ALERTMANAGER_SMTP_HOST`, `ALERTMANAGER_SMTP_FROM`
- `ALERTMANAGER_SMTP_USERNAME`, `ALERTMANAGER_SMTP_PASSWORD`
- `ALERTMANAGER_WEBHOOK_URL`
- `ALERTMANAGER_CRITICAL_EMAIL`, `ALERTMANAGER_WARNING_EMAIL`

---

## 8. Sorun Giderme

### Backend baÅŸlamÄ±yor

| Sorun | Ã‡Ã¶zÃ¼m |
|-------|-------|
| PostgreSQL baÄŸlantÄ± hatasÄ± | `docker compose ps postgres` â€” container Ã§alÄ±ÅŸÄ±yor mu? Healthcheck geÃ§ti mi? |
| Redis baÄŸlantÄ± hatasÄ± | `REDIS_PASSWORD` .env'de doÄŸru mu? |
| Keycloak hazÄ±r deÄŸil | Keycloak ilk aÃ§Ä±lÄ±ÅŸta 60+ sn sÃ¼rebilir. `docker compose logs keycloak` ile izleyin |
| Kafka SASL hatasÄ± | `KAFKA_SASL_PASSWORD` tÃ¼m servislerde aynÄ± olmalÄ± |
| Flyway migration hatasÄ± | VeritabanÄ± ÅŸemasÄ± gÃ¼ncel mi? `db/migration/` script'leri sÄ±ralÄ± mÄ±? |

### Frontend API hatasÄ±

| Sorun | Ã‡Ã¶zÃ¼m |
|-------|-------|
| CORS hatasÄ± | `application.yml` â†’ `cors.allowed-origins` doÄŸru origin iÃ§eriyor mu? |
| 401 Unauthorized | Keycloak token sÃ¼resi dolmuÅŸ olabilir. Yeniden giriÅŸ yapÄ±n |
| API 404 | Nginx routing: `/api/` â†’ backend. `nginx/nginx.dev.conf` kontrol edin |
| WebSocket baÄŸlantÄ± hatasÄ± | Backend WebSocket endpoint aktif mi? `http://localhost:8088/ws` |

### Keycloak sorunlarÄ±

| Sorun | Ã‡Ã¶zÃ¼m |
|-------|-------|
| Realm import hatasÄ± | `keycloak/realm-export.json` geÃ§erli mi? |
| JWT issuer mismatch | `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` Keycloak URL ile eÅŸleÅŸmeli |
| Client secret hatalÄ± | `KEYCLOAK_FINANCE_BACKEND_SECRET` realm-export.json'daki client secret ile aynÄ± olmalÄ± |

### Docker Compose sorunlarÄ±

| Sorun | Ã‡Ã¶zÃ¼m |
|-------|-------|
| Config validation hatasÄ± | `docker compose -f docker-compose.yml config` ile doÄŸrulayÄ±n |
| Port Ã§akÄ±ÅŸmasÄ± | BaÅŸka uygulama aynÄ± portu kullanÄ±yor olabilir. PortlarÄ± deÄŸiÅŸtirin veya diÄŸer uygulamayÄ± durdurun |
| Out of memory | Light compose kullanÄ±n veya RAM artÄ±rÄ±n |
| Volume permission | Linux'ta `sudo` gerekebilir veya user namespace ayarlayÄ±n |

### Production Ã¶zel sorunlar

| Sorun | Ã‡Ã¶zÃ¼m |
|-------|-------|
| Docker Secrets bulunamÄ±yor | `secrets/*.txt` dosyalarÄ± mevcut mu? `secrets/` .gitignore'da mÄ±? |
| SSL sertifika hatasÄ± | `nginx/ssl/fullchain.pem` ve `privkey.pem` doÄŸru mu? |
| 502 Bad Gateway | Backend container'lar healthy mi? `docker compose ps` |
| Grafana ÅŸifre dosyasÄ± | `GF_SECURITY_ADMIN_PASSWORD__FILE` (Ã§ift alt Ã§izgi) kullanÄ±lmalÄ± |

### FaydalÄ± Komutlar

```bash
# TÃ¼m container durumlarÄ±
docker compose ps

# Belirli servis loglarÄ±
docker compose logs -f backend

# Backend health check
curl http://localhost:8088/actuator/health

# PostgreSQL baÄŸlantÄ± testi
docker exec mintstack-postgres pg_isready -U mintstack -d mintstack_finance

# Redis ping
docker exec mintstack-redis redis-cli -a $REDIS_PASSWORD ping
```

---

## Ä°lgili DokÃ¼mantasyon

- [README.md](../README.md) â€” Proje genel bakÄ±ÅŸ
- [ARCHITECTURE.md](ARCHITECTURE.md) â€” Sistem mimarisi
- [API_VERSIONING.md](API_VERSIONING.md) â€” API versiyonlama
- [SECURITY.md](SECURITY.md) â€” GÃ¼venlik checklist
- [KEYCLOAK_2FA_SETUP.md](KEYCLOAK_2FA_SETUP.md) â€” 2FA kurulumu
