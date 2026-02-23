# MintStack Finance Portal - Deployment Rehberi

Bu dokümantasyon, MintStack Finance Portal'ın development ve production ortamlarına deployment sürecini açıklar.

---

## İçindekiler

1. [Gereksinimler](#gereksinimler)
2. [Development Deployment](#development-deployment)
3. [Production Deployment](#production-deployment)
4. [Ortam Konfigürasyonu](#ortam-konfigürasyonu)
5. [SSL/TLS Kurulumu](#ssltls-kurulumu)
6. [Backup ve Recovery](#backup-ve-recovery)
7. [Monitoring ve Alerting](#monitoring-ve-alerting)
8. [Troubleshooting](#troubleshooting)

---

## Gereksinimler

### Sistem Gereksinimleri

| Bileşen | Minimum | Önerilen (Production) |
|---------|---------|----------------------|
| CPU | 4 cores | 8+ cores |
| RAM | 8 GB | 16+ GB |
| Disk | 50 GB SSD | 100+ GB SSD |
| OS | Linux (Ubuntu 22.04+) | Linux (Ubuntu 22.04+) |

### Yazılım Gereksinimleri

| Yazılım | Versiyon |
|---------|----------|
| Docker | 24.0+ |
| Docker Compose | 2.20+ |
| Git | 2.40+ |
| OpenSSL | 3.0+ |

### Kontrol

```bash
# Versiyon kontrolü
docker --version
docker compose version
git --version
openssl version
```

---

## Development Deployment

### Hızlı Başlangıç

```bash
# 1. Repository'yi klonlayın
git clone https://github.com/YOUR_USERNAME/MintStack-Finance.git
cd MintStack-Finance

# 2. Environment dosyasını oluşturun
cp .env.example .env

# 3. .env dosyasını düzenleyin (en azından secret'ları değiştirin)
nano .env

# 4. Tüm servisleri başlatın
docker-compose up -d

# 5. Servislerin hazır olmasını bekleyin (~2-3 dakika)
docker-compose ps
```

### Servis URL'leri (Development)

| Servis | URL | Açıklama |
|--------|-----|----------|
| Frontend | http://localhost:3001 | React Web Uygulaması |
| Backend API | http://localhost:18080/api/v1 | REST API |
| Swagger UI | http://localhost:18080/swagger-ui.html | API Dokümantasyonu |
| Keycloak | http://localhost:8180 | Identity Management |
| OpenSearch Dashboards | http://localhost:15601 | Log & Metrics Dashboard |
| Prometheus | http://localhost:8889 | Metrics Endpoint |
| Grafana | http://localhost:3030 | Monitoring Dashboard |
| Alertmanager | http://localhost:9093 | Alert Management |

### Varsayılan Kullanıcılar

| Kullanıcı | Şifre | Rol |
|-----------|-------|-----|
| `admin` | `Admin123!` | Admin |
| `testuser` | `Test123!` | User |
| Keycloak Admin | `admin` | `KeycloakAdmin2026!` |

### Log İzleme

```bash
# Tüm servislerin logları
docker-compose logs -f

# Belirli bir servis
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f keycloak

# Son 100 satır
docker-compose logs --tail=100 backend
```

### Servis Yönetimi

```bash
# Servisleri durdur
docker-compose down

# Servisleri yeniden başlat
docker-compose restart

# Yeniden build ederek başlat
docker-compose up -d --build

# Verileri silerek tam temizlik
docker-compose down -v
```

---

## Production Deployment

### 1. Ön Hazırlık

```bash
# Production environment dosyasını oluşturun
cp .env.example .env

# Tüm secret'ları production değerleriyle değiştirin
# Önemli: Her secret için benzersiz, güçlü değerler kullanın!
```

### 2. SSL Sertifikaları

```bash
# Let's Encrypt ile sertifika alın (önerilen)
sudo apt install certbot
sudo certbot certonly --standalone -d your-domain.com

# Veya self-signed sertifika oluşturun (test için)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout docker/nginx/ssl/server.key \
  -out docker/nginx/ssl/server.crt
```

### 3. Production Docker Compose

Production için optimize edilmiş `docker-compose.prod.yml` kullanın:

```bash
# Production deployment
docker-compose -f docker-compose.prod.yml up -d
```

### 4. Health Check

```bash
# Backend health
curl http://localhost:18080/actuator/health

# Keycloak health
curl http://localhost:8180/health/ready

# OpenSearch health
curl -k -u admin:${OPENSEARCH_INITIAL_ADMIN_PASSWORD} \
  https://localhost:19200/_cluster/health
```

### 5. Production Checklist

- [ ] Tüm default şifreler değiştirildi
- [ ] SSL sertifikaları kuruldu
- [ ] Firewall kuralları ayarlandı
- [ ] Backup job'lar schedule edildi
- [ ] Monitoring alerts yapılandırıldı
- [ ] Log rotation ayarlandı
- [ ] Rate limiting aktif
- [ ] CORS origins kısıtlandı

---

## Ortam Konfigürasyonu

### Frontend Environment Variables

```bash
# frontend/.env.production
VITE_API_URL=/api/v1
VITE_KEYCLOAK_URL=https://your-domain.com/auth
VITE_KEYCLOAK_REALM=mintstack-finance
VITE_KEYCLOAK_CLIENT_ID=finance-frontend
VITE_WS_URL=wss://your-domain.com/ws
VITE_SENTRY_DSN=https://your-sentry-dsn
```

### Backend Environment Variables

```bash
# backend/.env.production (veya Docker secrets)
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mintstack_finance
SPRING_DATASOURCE_USERNAME=mintstack
SPRING_DATASOURCE_PASSWORD=<secure-password>

SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=<secure-password>

SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

OPENSEARCH_HOST=opensearch
OPENSEARCH_PORT=9200
OPENSEARCH_SCHEME=https

OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_SERVICE_NAME=finance-portal

ALPHA_VANTAGE_API_KEY=<your-key>
FINNHUB_API_KEY=<your-key>

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=<your-email>
SMTP_PASSWORD=<app-password>
EMAIL_FROM=noreply@your-domain.com
```

### Application Profiles

```yaml
# Development
SPRING_PROFILES_ACTIVE=dev

# Production
SPRING_PROFILES_ACTIVE=prod
```

---

## SSL/TLS Kurulumu

### Nginx Reverse Proxy

```nginx
# docker/nginx/nginx.conf
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers off;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Content-Security-Policy "default-src 'self';" always;

    location / {
        proxy_pass http://frontend:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws {
        proxy_pass http://backend:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
```

### Sertifika Yenileme (Let's Encrypt)

```bash
# Otomatik yenileme testi
sudo certbot renew --dry-run

# Cron job ekle
sudo crontab -e
# Ekle: 0 0 1 * * /usr/bin/certbot renew --quiet --post-hook "docker-compose restart nginx"
```

---

## Backup ve Recovery

### PostgreSQL Backup

```bash
# Manuel backup
docker exec mintstack-postgres pg_dump -U mintstack mintstack_finance > backup_$(date +%Y%m%d).sql

# Otomatik backup script
cat > backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/backups/postgres"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
docker exec mintstack-postgres pg_dump -U mintstack mintstack_finance | gzip > $BACKUP_DIR/backup_$TIMESTAMP.sql.gz

# Eski backup'ları temizle (30 günden eski)
find $BACKUP_DIR -name "backup_*.sql.gz" -mtime +30 -delete
EOF

chmod +x backup.sh

# Cron job
# 0 2 * * * /path/to/backup.sh
```

### PostgreSQL Restore

```bash
# Veritabanını restore et
gunzip -c backup_20260201_020000.sql.gz | docker exec -i mintstack-postgres psql -U mintstack mintstack_finance
```

### Redis Backup

```bash
# Redis RDB backup
docker exec mintstack-redis redis-cli -a ${REDIS_PASSWORD} BGSAVE
docker cp mintstack-redis:/data/dump.rdb redis_backup_$(date +%Y%m%d).rdb
```

### Keycloak Backup

```bash
# Keycloak realm export
docker exec mintstack-keycloak \
  /opt/keycloak/bin/kc.sh export \
  --realm mintstack-finance \
  --file /tmp/realm-export.json

docker cp mintstack-keycloak:/tmp/realm-export.json keycloak_backup_$(date +%Y%m%d).json
```

### OpenSearch Backup

```bash
# Snapshot repository kayıt
curl -k -u admin:${OPENSEARCH_INITIAL_ADMIN_PASSWORD} -X PUT \
  "https://localhost:19200/_snapshot/backup_repo" \
  -H 'Content-Type: application/json' \
  -d'{
    "type": "fs",
    "settings": {
      "location": "/opt/opensearch/backups"
    }
  }'

# Snapshot oluştur
curl -k -u admin:${OPENSEARCH_INITIAL_ADMIN_PASSWORD} -X PUT \
  "https://localhost:19200/_snapshot/backup_repo/snapshot_$(date +%Y%m%d)"
```

---

## Monitoring ve Alerting

### Prometheus Metrics

Backend, Prometheus formatında metrikler sunar:

```bash
# Metrics endpoint
curl http://localhost:18080/actuator/prometheus

# Önemli metrikler
# - http_server_requests_seconds
# - jvm_memory_used_bytes
# - process_cpu_usage
# - hikaricp_connections_active
```

### Grafana Dashboards

Grafana'da hazır dashboard'lar mevcuttur:

1. **JVM Dashboard** - Backend JVM metrikleri
2. **Spring Boot Dashboard** - Spring Boot actuator metrikleri
3. **PostgreSQL Dashboard** - Veritabanı metrikleri
4. **Kafka Dashboard** - Kafka metrikleri

```bash
# Grafana erişimi
http://localhost:3030
# Kullanıcı: admin / ${GRAFANA_ADMIN_PASSWORD}
```

### Alert Rules

Prometheus alert kuralları `docker/prometheus/alerts.yml` içinde:

```yaml
groups:
  - name: mintstack-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"

      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"

      - alert: HighMemoryUsage
        expr: process_resident_memory_bytes / 1024 / 1024 > 800
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
```

### Alertmanager Konfigürasyonu

```yaml
# docker/alertmanager/alertmanager.yml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'alerts@your-domain.com'
  smtp_auth_username: 'your-email@gmail.com'
  smtp_auth_password: 'app-password'

route:
  receiver: 'team-email'
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 1h

receivers:
  - name: 'team-email'
    email_configs:
      - to: 'team@your-domain.com'
```

---

## Troubleshooting

### Backend Başlamıyor

```bash
# PostgreSQL kontrolü
docker-compose ps postgres
docker-compose logs postgres

# Keycloak kontrolü
docker-compose logs keycloak

# Environment variables kontrolü
docker-compose config
```

**Yaygın Nedenler:**
- PostgreSQL hazır değil → `depends_on` ve health check kontrolü
- Keycloak hazır değil → Keycloak loglarını kontrol et
- Yanlış environment variables → `.env` dosyasını kontrol et

### Frontend API Hatası

```bash
# Backend health check
curl http://localhost:18080/actuator/health

# CORS kontrolü
# Backend application.yml'da cors.allowed-origins kontrol et

# Network kontrolü
docker network ls
docker network inspect mintstack-network
```

### WebSocket Bağlantı Hatası

```bash
# WebSocket endpoint kontrolü
curl http://localhost:18080/ws/info

# Nginx konfigürasyonu (production)
# /ws location'ının doğru proxy_pass ve upgrade header'ları içerdiğini kontrol et
```

### Database Bağlantı Hatası

```bash
# PostgreSQL logları
docker-compose logs postgres

# Bağlantı testi
docker exec -it mintstack-postgres psql -U mintstack -d mintstack_finance

# Connection pool kontrolü (backend)
curl http://localhost:18080/actuator/health | jq '.components.db'
```

### Kafka Bağlantı Hatası

```bash
# Kafka broker kontrolü
docker exec mintstack-kafka kafka-topics --bootstrap-server localhost:9092 --list

# SASL konfigürasyonu kontrolü
docker exec mintstack-kafka cat /etc/kafka/kafka_server_jaas.conf
```

### OpenSearch Bağlantı Hatası

```bash
# OpenSearch health
curl -k -u admin:${OPENSEARCH_INITIAL_ADMIN_PASSWORD} https://localhost:19200/_cluster/health

# Index kontrolü
curl -k -u admin:${OPENSEARCH_INITIAL_ADMIN_PASSWORD} https://localhost:19200/_cat/indices
```

### Memory Sorunları

```bash
# Container kaynak kullanımı
docker stats

# JVM heap dump (gerekirse)
docker exec mintstack-backend jcmd 1 GC.heap_info

# Container memory limit artırma
# docker-compose.yml'da deploy.resources.limits.memory değerini artırın
```

### Log Analizi

```bash
# OpenSearch Dashboards'ta log arama
# http://localhost:15601

# Veya doğrudan CLI ile
docker-compose logs backend | grep ERROR
docker-compose logs backend | grep -i "exception"
```

---

## Faydalı Komutlar

```bash
# Servis health durumları
docker-compose ps

# Container kaynak kullanımı
docker stats --no-stream

# Network bağlantıları
docker network inspect mintstack-network

# Volume kullanımı
docker volume ls
docker system df -v

# Temizlik (dikkatli kullanın)
docker system prune -a --volumes

# Container içine giriş
docker exec -it mintstack-backend /bin/sh
docker exec -it mintstack-postgres /bin/bash

# Dosya kopyalama
docker cp local_file.txt mintstack-backend:/app/
docker cp mintstack-backend:/app/logs/ ./logs/
```

---

## İletişim ve Destek

- **Dokümantasyon:** `docs/` dizini
- **Güvenlik:** `docs/SECURITY.md`
- **Mimari:** `docs/ARCHITECTURE.md`
- **Issue Tracker:** GitHub Issues

---

*Son Güncelleme: Şubat 2026*
