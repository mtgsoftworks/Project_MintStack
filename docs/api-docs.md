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

## Portfolio Trading & Simulation Endpoints

- `POST /portfolios/{id}/trades`
  - Creates a simulated trade order.
  - Request fields:
    - `instrumentId` or `instrumentSymbol` (one is required)
    - `transactionType`: `BUY | SELL`
    - `orderType`: `MARKET | LIMIT | STOP` (default: `MARKET`)
    - `quantity`
    - `price` (optional reference for market)
    - `limitPrice` (required for `LIMIT`)
    - `stopPrice` (required for `STOP`)
    - `transactionDate`, `notes` (optional)

- `POST /portfolios/{id}/orders/process`
  - Attempts to fill queued `PENDING` / `PARTIALLY_FILLED` orders.

- `POST /portfolios/{id}/orders/{orderId}/cancel?reason=...`
  - Cancels pending order.

- `POST /portfolios/{id}/cash`
  - Simulated cash adjustment.
  - Request:
    - `action`: `DEPOSIT | WITHDRAW`
    - `amount`
    - `notes` (optional)

- `GET /portfolios/{id}/transactions`
  - Query params:
    - `page`, `size`
    - `orderStatus` (optional): `PENDING | PARTIALLY_FILLED | FILLED | CANCELED | REJECTED`

- `GET /portfolios/{id}/summary`
  - Includes `cashBalance`, `netAssetValue`, `realizedProfitLoss`, `unrealizedProfitLoss`.

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
