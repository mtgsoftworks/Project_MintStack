# MintStack Finance Portal

Turkey-focused finance platform for market tracking, portfolio management, and technical analysis.

## Quick Start

### Requirements

- Docker
- Docker Compose v2+
- Git

### Run

```bash
git clone https://github.com/YOUR_USERNAME/MintStack-Finance.git
cd MintStack-Finance
cp .env.example .env
docker compose up -d
```

Important:

- Never commit `.env` or real credentials.
- Set strong values for all secrets before first run.
- For default user passwords, use environment variables referenced in `keycloak/realm-export.json`.

## Services

| Service | URL |
|---|---|
| Frontend | <http://localhost:3002> |
| Backend API | <http://localhost:18080/api/v1> |
| Swagger UI | <http://localhost:18080/swagger-ui.html> |
| Keycloak | <http://localhost:8180> |

Keycloak admin console credentials come from your `.env` values:

- `KEYCLOAK_ADMIN`
- `KEYCLOAK_ADMIN_PASSWORD`

## Tech Stack

- Backend: Java 17, Spring Boot 3.4.2, PostgreSQL 15, Redis 7, Kafka 3.5
- Frontend: React 18.2, Vite 5
- Infra: Docker, Keycloak, OpenLDAP, Nginx
- Observability: OpenSearch, Prometheus, Grafana

## Development

### Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Project Layout

- `backend`: Spring Boot API
- `frontend`: React SPA
- `docker`: DB, cache, messaging, observability configs
- `keycloak`: realm and auth configuration
- `docs`: architecture, deployment, and security docs
