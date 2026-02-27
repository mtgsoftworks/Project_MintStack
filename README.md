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
| Grafana | <http://localhost:3030> |

## Local Access Credentials

This section is for local development only. Rotate/change these values before any shared or production use.

### Application Login (Frontend)

- Username: `admin`
- Password: `Admin123!Mint!`
- Username: `test`
- Password: `TestUser123!M!`

### Keycloak Admin Console

- URL: <http://localhost:8180/admin/>
- Username: `admin`
- Password: `C1GVjujPw3WlrV5zNZOpknZSyz0ELaC9wK7YB6Uc0Y0=`

### Grafana

- URL: <http://localhost:3030>
- Username: `admin`
- Password: `aRIRHwf1jzfC8VMhQsLpVhIUtPhKcM+X`

Keycloak and Grafana credentials are sourced from `.env`:

- `KEYCLOAK_ADMIN`
- `KEYCLOAK_ADMIN_PASSWORD`
- `KEYCLOAK_ADMIN_USER_PASSWORD`
- `KEYCLOAK_TEST_USER_PASSWORD`
- `GRAFANA_ADMIN_PASSWORD`

## Tech Stack

- Backend: Java 17, Spring Boot 3.4.2, PostgreSQL 15, Redis 7, Kafka 3.5
- Frontend: React 18.2, Vite 5
- Infra: Docker, Keycloak, OpenLDAP, Nginx
- Observability: OpenSearch, Prometheus, Grafana

## Portfolio Simulation Updates

- Trading supports `MARKET`, `LIMIT`, and `STOP` orders.
- Order lifecycle is modeled as `PENDING`, `PARTIALLY_FILLED`, `FILLED`, `CANCELED`, `REJECTED`.
- Pending orders can be processed on demand from UI/API.
- Portfolio cash operations support simulated deposit/withdraw.
- Commission model includes:
  - base commission rate
  - minimum commission amount
  - commission tax rate
  - instrument-based multiplier
- Portfolio summary exposes:
  - cash balance
  - net asset value
  - realized PnL
  - unrealized PnL

Key portfolio endpoints:

- `POST /api/v1/portfolios/{id}/trades`
- `POST /api/v1/portfolios/{id}/orders/process`
- `POST /api/v1/portfolios/{id}/orders/{orderId}/cancel`
- `POST /api/v1/portfolios/{id}/cash`
- `GET /api/v1/portfolios/{id}/transactions?orderStatus=...`

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
