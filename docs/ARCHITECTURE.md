# MintStack Finance Portal - Architecture

## 1. Runtime Topology

- Frontend: React + Vite (Nginx container in Docker)
- Backend: Spring Boot monolith (REST + WebSocket + schedulers)
- Data stores: PostgreSQL + Redis
- Messaging: Kafka
- Auth: Keycloak (with OpenLDAP integration)
- Observability: Prometheus, Grafana, OpenSearch, Logstash, OTEL Collector

## 2. Core Data Flow

1. Scheduled jobs fetch market/news data from external providers.
2. Backend validates, transforms, and persists data in PostgreSQL.
3. Hot/read paths are cached in Redis.
4. Important updates are published to Kafka topics.
5. Clients receive near-real-time updates over STOMP WebSocket topics.

## 3. Security Model

- Authentication: OAuth2 Resource Server with Keycloak JWT.
- Authorization:
  - Public read-only: `GET /api/v1/market/**`, `GET /api/v1/news/**`
  - Authenticated: user-scoped endpoints (`/users`, `/portfolios`, `/watchlist`, `/alerts`, `/data-sources`)
  - Admin-only: `/api/v1/admin/**`, `/api/v1/simulation/**`
- Transport hardening: security headers + CORS allowlist.
- Rate limiting: Bucket4j (anonymous/auth/admin buckets).

## 4. Scheduling

Configured in `backend/src/main/resources/application.yml`:

- TCMB rates: `0 30 10,16 * * MON-FRI`
- Stock prices: `0 * * * * *` (every minute)
- Non-TCMB forex: `0 */5 * * * MON-FRI`
- Crypto prices: `0 * * * * *` (every minute)
- News fetch: `0 */15 * * * *` (every 15 minutes)
- Cleanup: `0 0 2 * * *`

## 5. Ports (Docker Compose Dev)

- Frontend: `3002`
- Backend: `18080`
- Keycloak: `8180`
- PostgreSQL: `5432` (localhost-bound)
- Redis: `6379` (localhost-bound)
- Kafka: `29092`
- OpenSearch: `19200` (localhost-bound)

## 6. Architectural Notes

- Current style is a modular monolith with clear package boundaries.
- Simulation domain is the heaviest module and isolated under `service/simulation`.
- Frontend data layer is standardized around RTK Query endpoints.
