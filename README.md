# MintStack Finance Portal

Turkiye odakli finans platformu: piyasa izleme, portfoy yonetimi, teknik analiz, alarm ve bildirim altyapisi.

## Hizli Baslangic

### Gereksinimler

- Docker
- Docker Compose v2+
- Git

### Calistirma

```bash
git clone https://github.com/YOUR_USERNAME/MintStack-Finance.git
cd MintStack-Finance
cp .env.example .env
docker compose up -d
```

> Not: `.env` dosyasini repoya commit etmeyin. Tum sifre ve anahtarlari guclu degerlerle degistirin.

## Servisler

| Servis | URL |
|---|---|
| API Gateway (Dev Nginx) | <http://localhost:8088> |
| Frontend | <http://localhost:3002> |
| Backend API (Gateway) | <http://localhost:8088/api/v1> |
| Swagger UI (Gateway) | <http://localhost:8088/swagger-ui.html> |
| Keycloak | <http://localhost:8180> |
| Grafana | <http://localhost:13030> |

## Giris Bilgileri (Dev)

### Uygulama (Frontend)

- `admin / .env icindeki KEYCLOAK_ADMIN_USER_PASSWORD`
- `test / .env icindeki KEYCLOAK_TEST_USER_PASSWORD`

### Keycloak Admin Konsolu

- URL: <http://localhost:8180/admin/>
- Kullanici: `.env` icindeki `KEYCLOAK_ADMIN`
- Sifre: `.env` icindeki `KEYCLOAK_ADMIN_PASSWORD`

### Grafana

- URL: <http://localhost:13030>
- Kullanici: `admin`
- Sifre: `.env` icindeki `GRAFANA_ADMIN_PASSWORD`

## Profiller

### Varsayilan Dev (KRaft + SASL)

```bash
docker compose up -d
```

### Secure Dev

```bash
docker compose -f docker-compose.yml -f docker-compose.secure-dev.yml up -d
```

### Lightweight Dev

```bash
docker compose -f docker-compose.light.yml up -d
```

## Teknoloji Yigini

- Backend: Java 17, Spring Boot 3.4, PostgreSQL 15, Redis 7, Kafka 3.5
- Frontend: React 18, TypeScript, Vite 5, Tailwind CSS, Redux Toolkit
- Kimlik: Keycloak 26, OpenLDAP
- Gozlemlenebilirlik: Prometheus, Grafana, OpenSearch, OTEL, Logstash

## Gelistirme

```bash
# Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
cd frontend
npm install
npm run dev
```

## Dizin Yapisi

| Dizin | Aciklama |
|---|---|
| `backend/` | Spring Boot API |
| `frontend/` | React SPA |
| `docker/` | Altyapi ve observability konfigurasyonlari |
| `keycloak/` | Realm ve kimlik konfigurasyonlari |
| `docs/` | Mimari, modelleme, sunum ve operasyon dokumanlari |

## Dokumanlar

- [Toplanti 2 Sunum Akisi](docs/TOPLANTI_2_SUNUM_AKISI.md)
- [Toplanti 2 Konusma Notlari](docs/TOPLANTI_2_KONUSMA_NOTLARI.md)
- [Mimari Dokumani](docs/ARCHITECTURE.md)
- [Tasarim ve Modelleme](docs/TASARIM_MIMARISI_VE_MODELLEME.md)
- [ADR Kayitlari](docs/ADR.md)
- [API Ozeti](docs/api-docs.md)
- [Deployment Rehberi](docs/DEPLOYMENT.md)
- [Guvenlik Rehberi](docs/SECURITY.md)
- [Keycloak 2FA Kurulumu](docs/KEYCLOAK_2FA_SETUP.md)
