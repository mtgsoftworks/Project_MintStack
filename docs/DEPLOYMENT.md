# Deployment Rehberi

## 1. Gereksinimler

| Bileşen | Minimum | Önerilen |
|---|---|---|
| **Docker** | 24+ | En güncel kararlı sürüm |
| **Docker Compose** | v2.20+ | En güncel |
| **Git** | 2.x | — |
| **CPU** | 4 çekirdek | 8 çekirdek |
| **RAM** | 8 GB | 16 GB |
| **Disk** | 20 GB | 50 GB (loglar ve veritabanı için) |
| **Ağ** | — | Dış API'ler için internet erişimi |

## 2. Hızlı Kurulum (Geliştirme Ortamı)

```bash
# 1. Repoyu klonla
git clone https://github.com/YOUR_USERNAME/MintStack-Finance.git
cd MintStack-Finance

# 2. Ortam değişkenlerini yapılandır
cp .env.example .env
# .env dosyasındaki tüm placeholder değerlerini gerçek değerlerle değiştir

# 3. Tüm servisleri başlat (15 konteyner)
docker compose up -d

# 4. Servislerin hazır olduğunu doğrula
docker compose ps

# 5. Backend health check
curl http://localhost:8088/actuator/health
```

### Erişim Noktaları

| Servis | URL |
|---|---|
| Frontend | `http://localhost:3002` |
| API Gateway | `http://localhost:8088` |
| REST API | `http://localhost:8088/api/v1` |
| Swagger UI | `http://localhost:8088/swagger-ui.html` |
| WebSocket | `ws://localhost:8088/ws` |
| Keycloak | `http://localhost:8180` |
| Grafana | `http://localhost:13030` |
| Prometheus | `http://localhost:9090` |
| OpenSearch Dashboards | `http://localhost:15601` |

## 3. Profil Bazlı Çalışma

### 3.1 Varsayılan Dev (Tam Stack — 15 servis)

```bash
docker compose up -d
```

İçerik: PostgreSQL, Redis, Keycloak, OpenLDAP, Kafka (KRaft + SASL), OpenSearch, OpenSearch Dashboards, Logstash, OTEL Collector, Prometheus, Grafana, AlertManager, Backend, Frontend, Nginx.

**Gerekli RAM:** ~8 GB

### 3.2 Lightweight Dev (Minimum — 6 servis)

```bash
docker compose -f docker-compose.light.yml up -d
```

İçerik: PostgreSQL, Redis, Keycloak, Backend, Frontend, Nginx. Observability servisleri hariç.

**Gerekli RAM:** ~4 GB

### 3.3 Secure Dev (TLS Aktif)

```bash
docker compose -f docker-compose.yml -f docker-compose.secure-dev.yml up -d
```

Varsayılan composition üzerine TLS/HTTPS yapılandırması ekler.

### 3.4 Production

```bash
# 1. .env dosyasını güçlü değerlerle doldur
# 2. TLS sertifikalarını hazırla
# 3. Stack'i kaldır
docker compose -f docker-compose.prod.yml up -d
```

## 4. Servis Başlatma Sırası ve Bağımlılıklar

```
PostgreSQL (healthcheck)
    ├── Keycloak (depends_on: postgres)
    │       └── Backend (depends_on: postgres, redis, kafka, keycloak)
    │               ├── Frontend (depends_on: backend)
    │               └── Nginx (depends_on: backend, frontend)
    ├── Redis (healthcheck)
    └── Kafka (independent, KRaft)
            └── Logstash (depends_on: kafka, opensearch)
OpenSearch (independent)
    └── OpenSearch Dashboards (depends_on: opensearch)
    └── OTEL Collector (depends_on: opensearch)
Prometheus (independent)
    ├── Grafana (depends_on: prometheus)
    └── AlertManager (independent)
OpenLDAP (independent)
```

## 5. İşletim Komutları

```bash
# Tüm servislerin durumunu kontrol et
docker compose ps

# Tüm log'ları izle
docker compose logs -f

# Belirli bir servisin loglarını izle
docker compose logs -f backend
docker compose logs -f frontend

# Belirli bir servisi yeniden başlat
docker compose restart backend

# Stack'i durdur (verileri koru)
docker compose down

# Stack'i durdur ve tüm verileri sil
docker compose down -v

# Belirli bir servisi yeniden build et
docker compose build --no-cache backend
docker compose up -d backend
```

## 6. Backup ve Restore

### 6.1 Bash Scriptleri (Linux/macOS)

```bash
# Backup al
./docker/backup/pg_backup.sh dev ./backups 7

# Production backup
./docker/backup/pg_backup.sh prod ./backups 14

# Restore
./docker/backup/pg_restore.sh dev ./backups/<dosya>.sql.gz
./docker/backup/pg_restore.sh prod ./backups/<dosya>.sql.gz
```

### 6.2 PowerShell Scriptleri (Windows)

```powershell
# Backup al
.\docker\backup\pg_backup.ps1 -Mode dev -BackupDir .\backups -RetentionDays 7

# Restore
.\docker\backup\pg_restore.ps1 -Mode dev -BackupFile .\backups\<dosya>.sql
```

### 6.3 Manuel Backup

```bash
# PostgreSQL dump
docker exec mintstack-postgres pg_dump -U mintstack -d mintstack_finance > backup.sql

# Redis dump
docker exec mintstack-redis redis-cli -a <password> BGSAVE
docker cp mintstack-redis:/data/dump.rdb ./backup/
```

## 7. Sorun Giderme

| Sorun | Çözüm |
|---|---|
| Keycloak başlamıyor | PostgreSQL healthcheck'i kontrol et: `docker logs mintstack-postgres` |
| Backend başlamıyor | Keycloak, Redis, Kafka healthcheck'lerini kontrol et |
| Frontend auth hatası | Keycloak realm ve client ayarlarını kontrol et |
| Kafka bağlantı hatası | SASL password eşleşmesini kontrol et |
| OpenSearch başlamıyor | `vm.max_map_count` ayarını kontrol et (Linux) |
| Yavaş başlangıç | İlk kurulumda Keycloak realm import süresi 60-90 saniye |

## 8. Toplantı Öncesi Hızlı Kontrol

```bash
# 1. Tüm servisler çalışıyor mu?
docker compose ps

# 2. Backend sağlıklı mı?
curl http://localhost:8088/actuator/health

# 3. Frontend erişilebilir mi?
curl -s -o /dev/null -w "%{http_code}" http://localhost:3002

# 4. Keycloak çalışıyor mu?
curl -s -o /dev/null -w "%{http_code}" http://localhost:8180

# 5. Demo verisi hazır mı?
# Login yaparak dashboard ve piyasa verilerini kontrol et
```
