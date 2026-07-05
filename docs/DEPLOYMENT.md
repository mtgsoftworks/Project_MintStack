# MintStack Finance Portal - Dağıtım Rehberi

> Son doğrulama: 5 Temmuz 2026 — `docker-compose*.yml`, `.env.example` ve GitHub Actions ile karşılaştırılmıştır.

Bu doküman, MintStack Finance Portal uygulamasının geliştirme ve üretim ortamlarında nasıl dağıtılacağını açıklar.

## İçindekiler

1. [Gereksinimler](#1-gereksinimler)
2. [Ortam Değişkenleri](#2-ortam-değişkenleri)
3. [Development Ortamı](#3-development-ortamı)
4. [Production Dağıtımı](#4-production-dağıtımı)
5. [CI/CD Pipeline](#5-cicd-pipeline)
6. [Yedekleme ve Geri Yükleme](#6-yedekleme-ve-geri-yükleme)
7. [Monitoring ve Alerting](#7-monitoring-ve-alerting)
8. [Sorun Giderme](#8-sorun-giderme)

---

## 1. Gereksinimler

### Donanım Gereksinimleri

| Ortam | CPU | RAM | Disk |
|-------|-----|-----|------|
| **Full Stack (docker-compose.yml)** | Min. 4 CPU | Min. 8 GB | 20 GB+ |
| **Lightweight (docker-compose.light.yml)** | Min. 2 CPU | Min. 4 GB | 10 GB+ |
| **Production (docker-compose.prod.yml)** | Min. 4 CPU | Min. 8 GB | 50 GB+ |

### Yazılım Gereksinimleri

- **Docker** 24+ (Docker Engine)
- **Docker Compose** v2+ (Compose V2 plugin)
- **Git** (kaynak kodu çekmek için)

### Kurulum Kontrolü

```bash
docker --version          # Docker 24+
docker compose version     # Docker Compose v2+
```

---

## 2. Ortam Değişkenleri

`.env.example` dosyasını `.env` olarak kopyalayın ve değerleri doldurun:

```bash
cp .env.example .env
```

### Ortam Değişkenleri Listesi

| Kategori | Değişken | Açıklama |
|----------|----------|----------|
| **PostgreSQL** | `POSTGRES_DB` | Veritabanı adı (varsayılan: mintstack_finance) |
| | `POSTGRES_USER` | Veritabanı kullanıcısı |
| | `POSTGRES_PASSWORD` | Şifre (üret: `openssl rand -base64 24`) |
| **Redis** | `REDIS_PASSWORD` | Redis şifresi (üret: `openssl rand -base64 24`) |
| **Keycloak** | `KEYCLOAK_ADMIN` | Admin kullanıcı adı |
| | `KEYCLOAK_ADMIN_PASSWORD` | Admin şifresi |
| | `KEYCLOAK_FINANCE_BACKEND_SECRET` | Backend client secret |
| **OpenLDAP** | `LDAP_ORGANISATION` | Organizasyon adı |
| | `LDAP_DOMAIN` | LDAP domain |
| | `LDAP_ADMIN_PASSWORD` | LDAP admin şifresi |
| **Kafka** | `KAFKA_CLUSTER_ID` | KRaft cluster ID |
| | `KAFKA_SASL_PASSWORD` | SASL şifresi |
| **OpenSearch** | `OPENSEARCH_INITIAL_ADMIN_PASSWORD` | Admin şifresi (min 8 karakter) |
| **Grafana** | `GRAFANA_ADMIN_PASSWORD` | Grafana admin şifresi |
| **Spring Boot** | `SPRING_PROFILES_ACTIVE` | `dev` veya `prod` |
| | `ALPHA_VANTAGE_API_KEY` | Alpha Vantage API anahtarı |
| | `FINNHUB_API_KEY` | Finnhub API anahtarı |
| | `APP_EXTERNAL_API_TEFAS_ENABLED` | TEFAS fon adapter anahtarı; API key gerekmez |
| | `APP_EXTERNAL_API_FINTABLES_ENABLED` | Fintables policy switch; güvenli varsayılan `false` |
| | `APP_NEWS_LLM_*` | RSS haber LLM enrichment endpoint/model/key ayarları |
| **Production** | `PUBLIC_APP_ORIGIN` | Tarayıcıdan erişilen tek HTTPS origin |
| | `APP_ALERT_WEBHOOK_REQUIRE_SIGNATURE` | Webhook açıksa production'da `true` olmalı |
| | `APP_RATE_LIMIT_TRUSTED_PROXIES` | Yalnız güvenilen reverse proxy CIDR'ları |
| **Frontend** | `VITE_API_URL` | API base URL |
| | `VITE_KEYCLOAK_URL` | Keycloak URL |
| | `VITE_KEYCLOAK_REALM` | Keycloak realm |
| | `VITE_KEYCLOAK_CLIENT_ID` | Frontend client ID |
| | `VITE_WS_URL` | WebSocket URL |

### Örnek .env (Geliştirme)

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

## 3. Development Ortamı

### 3.1 Full Stack (Tüm Servisler)

16 servis: postgres, redis, keycloak, openldap, kafka, kafka-exporter, opensearch, opensearch-dashboards, logstash, otel-collector, prometheus, grafana, alertmanager, backend, frontend, nginx.

```bash
# .env dosyasını hazırlayın
cp .env.example .env
# .env içindeki değerleri düzenleyin

# Tüm servisleri başlat
docker compose up -d

# Logları izle
docker compose logs -f backend
```

**Erişim Noktaları:**

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

~4 GB RAM yeterli. Kafka, OpenSearch, Logstash, Prometheus, Grafana, Alertmanager, OTEL hariç.

```bash
docker compose -f docker-compose.light.yml up -d
```

**Light modda devre dışı özellikler:**
- Kafka mesajlaşma
- OpenSearch log aggregation
- OpenTelemetry tracing
- Prometheus/Grafana monitoring

**Erişim:** http://localhost:8088

### 3.3 Servisleri Durdurma

```bash
# Full stack
docker compose down

# Light stack
docker compose -f docker-compose.light.yml down

# Volume'ları da silmek için
docker compose down -v
```

---

## 4. Production Dağıtımı

### 4.1 Ön Hazırlık

#### Docker Secrets Oluşturma

Production ortamında hassas veriler Docker Secrets ile yönetilir. `secrets/` klasöründe aşağıdaki dosyaları oluşturun:

```bash
mkdir -p secrets

# Her secret için ayrı dosya (içerik sadece değer, satır sonu yok)
echo -n "your_postgres_password" > secrets/postgres_password.txt
echo -n "your_redis_password" > secrets/redis_password.txt
echo -n "your_keycloak_admin_password" > secrets/keycloak_admin_password.txt
echo -n "your_alpha_vantage_api_key" > secrets/alpha_vantage_key.txt
openssl rand -base64 32 > secrets/app_field_encryption_key.txt
echo -n "your_grafana_password" > secrets/grafana_admin_password.txt
echo -n "your_ldap_password" > secrets/ldap_admin_password.txt

# Bu dosyalar mevcut .gitignore kurallarıyla ignore edilir; commit etmeyin.
```

#### SSL Sertifikaları

Nginx production konfigürasyonu HTTPS kullanır. `nginx/ssl/` klasörüne sertifikaları yerleştirin:

```
nginx/ssl/
├── fullchain.pem   # Let's Encrypt veya CA sertifikası
└── privkey.pem    # Özel anahtar
```

Let's Encrypt ile örnek:

```bash
# Certbot ile sertifika al
certbot certonly --standalone -d yourdomain.com
# Sertifikalar: /etc/letsencrypt/live/yourdomain.com/
cp /etc/letsencrypt/live/yourdomain.com/fullchain.pem nginx/ssl/
cp /etc/letsencrypt/live/yourdomain.com/privkey.pem nginx/ssl/
```

OpenLDAP için ayrıca `secrets/ldap/ca.crt`, `secrets/ldap/ldap.crt` ve `secrets/ldap/ldap.key` hazırlanmalıdır.

### 4.2 Ağ Segmentasyonu

Production compose dosyası dört ayrı ağ kullanır:

| Ağ | Servisler | Açıklama |
|----|-----------|----------|
| **data-network** | postgres, redis, opensearch | Veri katmanı (internal) |
| **app-network** | backend, frontend, nginx | Uygulama katmanı |
| **auth-network** | keycloak, openldap | Kimlik doğrulama (internal) |
| **obs-network** | prometheus, grafana, alertmanager, kafka, logstash, otel-collector | Observability (internal) |

### 4.3 Production Ortam Değişkenleri

`.env` dosyasında production değerlerini ayarlayın:

```env
SPRING_PROFILES_ACTIVE=prod
PUBLIC_APP_ORIGIN=https://yourdomain.com

VITE_API_URL=/api/v1
VITE_WS_URL=wss://yourdomain.com/ws
VITE_KEYCLOAK_URL=https://yourdomain.com/auth

APP_EXTERNAL_API_FINTABLES_ENABLED=false
APP_ALERT_WEBHOOK_REQUIRE_SIGNATURE=true
APP_ALERT_WEBHOOK_SECRET=<strong-random-secret>
```

> **Production blocker:** `docker-compose.prod.yml` içinde Fintables varsayılanı halen `true`, webhook signature varsayılanı `false` durumundadır. Ayrıca backend `SecurityConfig`, `APP_CORS_*` değerleri yerine wildcard origin pattern kullanmaktadır. Bu üç konu kod/config seviyesinde düzeltilmeden production güvenli kabul edilmez.

### 4.4 Dağıtım

```bash
# Önce zorunlu değişken ve secret referanslarını doğrula
docker compose -f docker-compose.prod.yml config

# Production compose ile başlat
docker compose -f docker-compose.prod.yml up -d

# Sadece uygulama katmanını güncellemek için
docker compose -f docker-compose.prod.yml up -d --build backend frontend nginx
```

### 4.5 Production Özellikleri

- **Backend replicas:** 2 instance (yük dengeleme)
- **HTTPS:** Nginx 80→443 yönlendirmesi, TLS 1.2/1.3
- **Güvenlik başlıkları:** HSTS, X-Frame-Options, CSP, vb.
- **Rate limiting:** API 30 req/s, login 5 req/s
- **Swagger/API docs:** Production'da devre dışı (404)

---

## 5. CI/CD Pipeline

### 5.1 CI Pipeline (`.github/workflows/ci.yml`)

**Tetikleyici:** `main`, `develop` push/PR ve manuel `workflow_dispatch`.

| Job | Açıklama |
|-----|----------|
| **secrets** | Gitleaks ile tam Git history secret taraması |
| **backend** | `./mvnw -B -ntp clean verify -DskipITs=false`, JaCoCo, OWASP raporu, Flyway `migrate -> validate` |
| **frontend** | `npm ci`, lint, typecheck, Vitest coverage, production build |
| **e2e** | Playwright Chromium smoke senaryoları |
| **images** | Backend/frontend image build ve Trivy taraması |
| **compose** | Dev/light/prod Compose config doğrulaması |

Güncel CI notları:

- Backend adımı unit/integration test, paketleme ve JaCoCo kontrolünü birlikte çalıştırır.
- JaCoCo kalite kapısı: line `0.35`, branch `0.12`.
- Flyway boş CI PostgreSQL üzerinde önce `migrate`, sonra `validate` çalıştırır.
- OWASP Dependency Check ve iki Trivy adımı `continue-on-error` çalışır; rapor üretir fakat pipeline'ı bloklamaz.
- Gitleaks, backend/frontend kalite işleri, Playwright E2E, image build ve Compose validation blocking kalite kapılarıdır.

### 5.2 Manuel Docker Build (`.github/workflows/deploy.yml`)

**Tetikleyici:** yalnızca manuel `workflow_dispatch`.

Bu workflow deploy yapmaz ve registry'ye image push etmez. Amacı yalnızca backend ve frontend Docker image'larının GitHub runner üzerinde build edilebildiğini doğrulamaktır.

| Job | Açıklama |
|-----|----------|
| **docker-build** | Backend ve frontend Docker image build + image inspect |

Bu manuel workflow ana CI'daki image build/scan işine ek bir build doğrulamasıdır. Production release için registry publish, SBOM/provenance, image signing, deployment ve rollback otomasyonu ayrıca tanımlanmalıdır.

---

## 6. Yedekleme ve Geri Yükleme

### 6.1 PostgreSQL Yedekleme

**Bash (Linux/macOS):**

```bash
# Geliştirme ortamı, 7 gün saklama
./docker/backup/pg_backup.sh dev ./backups 7

# Production ortamı, 14 gün saklama
./docker/backup/pg_backup.sh prod ./backups 14
```

**PowerShell (Windows):**

```powershell
.\docker\backup\pg_backup.ps1 -Mode dev -BackupDir .\backups -RetentionDays 7
.\docker\backup\pg_backup.ps1 -Mode prod -BackupDir .\backups -RetentionDays 14
```

> **Not:** Bash script `.sql.gz` (sıkıştırılmış), PowerShell script `.sql` (sıkıştırılmamış) üretir.

**Parametreler:**
- `MODE`: `dev` veya `prod` (container adına göre)
- `BACKUP_DIR`: Yedek dosyalarının dizini
- `RETENTION_DAYS`: Bu süreden eski yedekler silinir

**Çıktı:** `./backups/dev_mintstack_finance_20260307_120000.sql.gz`

### 6.2 PostgreSQL Geri Yükleme

**Bash:**

```bash
./docker/backup/pg_restore.sh dev ./backups/dev_mintstack_finance_20260307_120000.sql.gz
./docker/backup/pg_restore.sh prod ./backups/prod_mintstack_finance_20260307_120000.sql.gz
```

**PowerShell:**

```powershell
# Bash ile oluşturulmuş .sql.gz dosyası için (gunzip pipe ile)
# PowerShell restore script sadece .sql dosyalarını destekler; .gz için bash kullanın
.\docker\backup\pg_restore.ps1 -Mode dev -BackupFile .\backups\dev_mintstack_finance_20260307_120000.sql
```

**Uyarı:** Geri yükleme mevcut veriyi üzerine yazar. Production'da önce test ortamında deneyin.

### 6.3 Zamanlanmış Yedekleme (Cron)

```cron
# Her gün 02:00'da production yedeği
0 2 * * * cd /opt/mintstack && ./docker/backup/pg_backup.sh prod /opt/backups 14
```

---

## 7. Monitoring ve Alerting

### 7.1 Prometheus

**URL:** http://localhost:9090 (dev) / internal (prod)

**Scrape hedefleri:**
- `backend:8080/actuator/prometheus` — Spring Boot metrikleri
- `otel-collector:8889` — OpenTelemetry metrikleri
- `localhost:9090` — Prometheus self-monitoring

**Konfigürasyon:** `docker/prometheus/prometheus.yml`

### 7.2 Grafana

**URL:** http://localhost:13030 (dev)

**Varsayılan giriş:** admin / `GRAFANA_ADMIN_PASSWORD`

**Datasource'lar (otomatik provisioning):**
- Prometheus: `http://prometheus:9090`
- Alertmanager: `http://alertmanager:9093`

**Dashboard'lar:** `docker/grafana/dashboards/` (api-dashboard, circuit-breaker-dashboard, jvm-dashboard)

### 7.3 Alertmanager

**URL:** http://localhost:9093 (dev)

**Konfigürasyon:** `docker/alertmanager/alertmanager.yml`

**Alert kuralları** (`docker/prometheus/alerts.yml`):

| Alert | Severity | Açıklama |
|-------|----------|----------|
| HighErrorRate | critical | 5xx hata oranı > %10 |
| ApplicationDown | critical | Backend yanıt vermiyor |
| DBConnectionPoolExhausted | critical | Connection pool > %90 |
| CircuitBreakerOpen | warning | Circuit breaker açık |
| HighLatency | warning | P95 latency > 2 sn |
| HighHeapMemory | warning | JVM heap > %85 |
| ExternalAPISlowResponse | warning | Harici API yavaş |
| ExternalAPIHighFailureRate | warning | Harici API hata oranı > %30 |
| KafkaConsumerLag | warning | Consumer lag > 1000 |

**Bildirim kanalları:**
- Webhook: `http://backend:8080/api/v1/admin/webhooks/alerts`
- E-posta: `ALERTMANAGER_CRITICAL_EMAIL`, `ALERTMANAGER_WARNING_EMAIL` (ops-team, dev-team)

**Ortam değişkenleri (Alertmanager için):**
- `ALERTMANAGER_SMTP_HOST`, `ALERTMANAGER_SMTP_FROM`
- `ALERTMANAGER_SMTP_USERNAME`, `ALERTMANAGER_SMTP_PASSWORD`
- `ALERTMANAGER_WEBHOOK_URL`
- `ALERTMANAGER_CRITICAL_EMAIL`, `ALERTMANAGER_WARNING_EMAIL`

---

## 8. Sorun Giderme

### Backend başlamıyor

| Sorun | Çözüm |
|-------|-------|
| PostgreSQL bağlantı hatası | `docker compose ps postgres` — container çalışıyor mu? Healthcheck geçti mi? |
| Redis bağlantı hatası | `REDIS_PASSWORD` .env'de doğru mu? |
| Keycloak hazır değil | Keycloak ilk açılışta 60+ sn sürebilir. `docker compose logs keycloak` ile izleyin |
| Kafka SASL hatası | `KAFKA_SASL_PASSWORD` tüm servislerde aynı olmalı |
| Flyway migration hatası | Veritabanı şeması güncel mi? `db/migration/` script'leri sıralı mı? |

### Frontend API hatası

| Sorun | Çözüm |
|-------|-------|
| CORS hatası | `application.yml` → `cors.allowed-origins` doğru origin içeriyor mu? |
| 401 Unauthorized | Keycloak token süresi dolmuş olabilir. Yeniden giriş yapın |
| API 404 | Nginx routing: `/api/` → backend. `nginx/nginx.dev.conf` kontrol edin |
| WebSocket bağlantı hatası | Backend WebSocket endpoint aktif mi? `http://localhost:8088/ws` |

### Keycloak sorunları

| Sorun | Çözüm |
|-------|-------|
| Realm import hatası | `keycloak/realm-export.json` geçerli mi? |
| JWT issuer mismatch | `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` Keycloak URL ile eşleşmeli |
| Client secret hatalı | `KEYCLOAK_FINANCE_BACKEND_SECRET` realm-export.json'daki client secret ile aynı olmalı |

### Docker Compose sorunları

| Sorun | Çözüm |
|-------|-------|
| Config validation hatası | `docker compose -f docker-compose.yml config` ile doğrulayın |
| Port çakışması | Başka uygulama aynı portu kullanıyor olabilir. Portları değiştirin veya diğer uygulamayı durdurun |
| Out of memory | Light compose kullanın veya RAM artırın |
| Volume permission | Linux'ta `sudo` gerekebilir veya user namespace ayarlayın |

### Production özel sorunlar

| Sorun | Çözüm |
|-------|-------|
| Docker Secrets bulunamıyor | `secrets/*.txt` dosyaları mevcut mu? `secrets/` .gitignore'da mı? |
| SSL sertifika hatası | `nginx/ssl/fullchain.pem` ve `privkey.pem` doğru mu? |
| 502 Bad Gateway | Backend container'lar healthy mi? `docker compose ps` |
| Grafana şifre dosyası | `GF_SECURITY_ADMIN_PASSWORD__FILE` (çift alt çizgi) kullanılmalı |

### Faydalı Komutlar

```bash
# Tüm container durumları
docker compose ps

# Belirli servis logları
docker compose logs -f backend

# Backend health check
curl http://localhost:8088/actuator/health

# PostgreSQL bağlantı testi
docker exec mintstack-postgres pg_isready -U mintstack -d mintstack_finance

# Redis ping
docker exec mintstack-redis redis-cli -a $REDIS_PASSWORD ping
```

---

## İlgili Dokümantasyon

- [README.md](../README.md) — Proje genel bakış
- [ARCHITECTURE.md](ARCHITECTURE.md) — Sistem mimarisi
- [API_VERSIONING.md](API_VERSIONING.md) — API versiyonlama
- [SECURITY.md](SECURITY.md) — Güvenlik checklist
- [KEYCLOAK_2FA_SETUP.md](KEYCLOAK_2FA_SETUP.md) — 2FA kurulumu
