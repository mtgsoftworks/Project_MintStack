# API Ozeti

## 1. Erisim Bilgisi

- Base URL (Gateway): `http://localhost:8088/api/v1`
- Swagger UI (Gateway): `http://localhost:8088/swagger-ui.html`
- Kimlik sunucusu: `http://localhost:8180`

## 2. Kimlik Dogrulama

- Korumali endpointler icin `Authorization: Bearer <token>` gerekir.
- Token alma ornegi:
  - `POST /realms/mintstack-finance/protocol/openid-connect/token`

## 3. API Versiyonlama

- URL bazli versiyonlama kullanilir: `/api/v1/...`
- Aktif versiyon: `v1`

## 4. Public Endpointler

- `GET /market/currencies`
- `GET /market/currencies/{code}`
- `GET /market/stocks`
- `GET /market/bonds`
- `GET /market/funds`
- `GET /market/viop`
- `GET /news`
- `GET /news/latest`
- `GET /news/categories`

## 5. Auth Gerektiren Endpointler

- Portfoy: `/portfolios/**`
- Izleme listesi: `/watchlist/**`
- Alarm: `/alerts/**`
- Kullanici profil/tercih/bildirim: `/users/**`
- Veri kaynagi tercihleri: `/data-sources/**`

## 6. Portfoy ve Simulasyon Endpointleri

- `POST /portfolios/{id}/trades`
  - Emir olusturur.
  - Alanlar: `instrumentId|instrumentSymbol`, `transactionType`, `orderType`, `quantity`, `price/limitPrice/stopPrice`.

- `POST /portfolios/{id}/orders/process`
  - Bekleyen emirleri kosullara gore islemeye calisir.

- `POST /portfolios/{id}/orders/{orderId}/cancel`
  - Bekleyen emri iptal eder.

- `POST /portfolios/{id}/cash`
  - Simulasyon nakit hareketi (`DEPOSIT`/`WITHDRAW`).

- `GET /portfolios/{id}/transactions`
  - Filtreleme ve sayfalama destekler.

- `GET /portfolios/{id}/summary`
  - Nakit, net varlik, gerceklesen/gerceklesmemis PnL alanlarini doner.

## 7. Admin Endpointleri

- Dashboard ve kullanici yonetimi: `/admin/**`
- Simulasyon kontrolu: `/simulation/**`

## 8. WebSocket

- Endpoint: `ws://localhost:8088/ws`
- STOMP connect header: `Authorization: Bearer <token>`
- Ornek topicler:
  - `/topic/prices/currency`
  - `/topic/prices/stocks/{symbol}`
