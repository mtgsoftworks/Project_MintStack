# Deployment Rehberi

## 1. Gereksinimler

- Docker 24+
- Docker Compose 2.20+
- Git
- Onerilen kaynak: en az 4 CPU, 8 GB RAM

## 2. Gelistirme Ortami

```bash
git clone https://github.com/YOUR_USERNAME/MintStack-Finance.git
cd MintStack-Finance
cp .env.example .env
docker compose up -d
```

Erisim noktalar:

- Frontend: `http://localhost:3002`
- Gateway: `http://localhost:8088`
- API: `http://localhost:8088/api/v1`
- Swagger: `http://localhost:8088/swagger-ui.html`
- Keycloak: `http://localhost:8180`

## 3. Profil Bazli Calisma

### Varsayilan

```bash
docker compose up -d
```

### Secure Dev

```bash
docker compose -f docker-compose.yml -f docker-compose.secure-dev.yml up -d
```

### Lightweight

```bash
docker compose -f docker-compose.light.yml up -d
```

## 4. Uretim Ortami

1. `.env` icerigini guclu degerlerle doldurun.
2. TLS sertifikalarini ve secret dosyalarini hazirlayin.
3. Prod compose ile stack'i kaldirin:

```bash
docker compose -f docker-compose.prod.yml up -d
```

## 5. Isletim Komutlari

```bash
docker compose ps
docker compose logs -f

docker compose restart <service>
docker compose down
```

## 6. Backup/Restore

### Bash scriptleri

```bash
./docker/backup/pg_backup.sh dev ./backups 7
./docker/backup/pg_backup.sh prod ./backups 14

./docker/backup/pg_restore.sh dev ./backups/<dosya>.sql.gz
./docker/backup/pg_restore.sh prod ./backups/<dosya>.sql.gz
```

### PowerShell scriptleri

```powershell
.\docker\backup\pg_backup.ps1 -Mode dev -BackupDir .\backups -RetentionDays 7
.\docker\backup\pg_restore.ps1 -Mode dev -BackupFile .\backups\<dosya>.sql
```

## 7. Toplanti Oncesi Hizli Kontrol

- `docker compose ps` tum kritik servisler up/healthy mi?
- `http://localhost:8088/actuator/health` cevap donuyor mu?
- Frontend login ve temel sayfalar aciliyor mu?
