# MintStack Finance Portal - API Quick Reference

Base URL: `http://localhost:18080/api/v1`  
Swagger UI: `http://localhost:18080/swagger-ui.html`

## Authentication

- Auth server: Keycloak
- Protected endpoints require `Authorization: Bearer <token>`
- Token endpoint example:
  `POST http://localhost:8180/realms/mintstack-finance/protocol/openid-connect/token`

## Public Endpoints

- `GET /market/currencies`
- `GET /market/currencies/{code}`
- `GET /market/stocks`
- `GET /market/bonds`
- `GET /market/funds`
- `GET /market/viop`
- `GET /news`
- `GET /news/latest`
- `GET /news/categories`

## Authenticated Endpoints

- Portfolio: `/portfolios/**`
- Watchlist: `/watchlist/**`
- Alerts: `/alerts/**`
- User profile/preferences/notifications: `/users/**`
- Data source preferences: `/data-sources/**`

## Admin Endpoints

- Admin dashboard/users: `/admin/**`
- Simulation controls: `/simulation/**`

## WebSocket

- Endpoint: `ws://localhost:18080/ws` (SockJS/STOMP)
- JWT is required on STOMP CONNECT headers:
  - `Authorization: Bearer <token>`
- Example topics:
  - `/topic/prices/currency`
  - `/topic/prices/stocks/{symbol}`
