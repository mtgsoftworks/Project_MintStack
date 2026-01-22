# MintStack Finance API Examples

Bu dokümantasyon, MintStack Finance API'sinin kullanım örneklerini içerir.

## Base URL

```
Development: http://localhost:18080/api/v1
Production: https://api.mintstack.finance/api/v1
```

## Authentication

Tüm korumalı endpoint'ler için Keycloak JWT token gereklidir.

```bash
# Token alma
curl -X POST "http://localhost:8180/realms/mintstack-finance/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=finance-portal" \
  -d "username=your_username" \
  -d "password=your_password"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer"
}
```

---

## Market Data Endpoints

### Get Currency Rates

```bash
curl -X GET "http://localhost:18080/api/v1/market/currencies" \
  -H "Accept: application/json"
```

Response:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "currencyCode": "USD",
      "currencyName": "ABD DOLARI",
      "buyingRate": 43.1234,
      "sellingRate": 43.2945,
      "changePercent": 0.45,
      "source": "TCMB",
      "fetchedAt": "2026-01-22T10:30:00"
    }
  ]
}
```

### Get Stock Price

```bash
curl -X GET "http://localhost:18080/api/v1/market/stocks/THYAO" \
  -H "Accept: application/json"
```

Response:
```json
{
  "success": true,
  "data": {
    "symbol": "THYAO",
    "name": "TURK HAVA YOLLARI",
    "currentPrice": 312.50,
    "previousClose": 308.25,
    "change": 4.25,
    "changePercent": 1.38,
    "currency": "TRY"
  }
}
```

### Get Market Index (XU100, S&P 500, etc.)

```bash
curl -X GET "http://localhost:18080/api/v1/market/index/XU100" \
  -H "Accept: application/json"
```

---

## Portfolio Endpoints

### Get All Portfolios

```bash
curl -X GET "http://localhost:18080/api/v1/portfolios" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Create Portfolio

```bash
curl -X POST "http://localhost:18080/api/v1/portfolios" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ana Portföy",
    "description": "Uzun vadeli yatırımlar",
    "currency": "TRY"
  }'
```

### Add Transaction

```bash
curl -X POST "http://localhost:18080/api/v1/portfolios/{portfolioId}/transactions" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "THYAO",
    "type": "BUY",
    "quantity": 100,
    "price": 310.50,
    "commission": 15.00,
    "transactionDate": "2026-01-22"
  }'
```

---

## Watchlist Endpoints

### Get All Watchlists

```bash
curl -X GET "http://localhost:18080/api/v1/watchlists" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Create Watchlist

```bash
curl -X POST "http://localhost:18080/api/v1/watchlists" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Teknoloji Hisseleri",
    "symbols": ["THYAO", "ASELS", "TCELL"]
  }'
```

---

## Alert Endpoints

### Create Price Alert

```bash
curl -X POST "http://localhost:18080/api/v1/alerts" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "THYAO",
    "targetPrice": 350.00,
    "condition": "ABOVE",
    "notificationType": "EMAIL"
  }'
```

### Get All Alerts

```bash
curl -X GET "http://localhost:18080/api/v1/alerts" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Settings Endpoints

### Get API Configurations

```bash
curl -X GET "http://localhost:18080/api/v1/settings/api-keys" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Add API Key

```bash
curl -X POST "http://localhost:18080/api/v1/settings/api-keys" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "ALPHA_VANTAGE",
    "apiKey": "YOUR_API_KEY",
    "isActive": true
  }'
```

### Trigger Data Fetch

```bash
curl -X POST "http://localhost:18080/api/v1/data-sources/trigger/{apiConfigId}" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## News Endpoints

### Get Latest News

```bash
curl -X GET "http://localhost:18080/api/v1/news?page=0&size=20" \
  -H "Accept: application/json"
```

### Get News by Symbol

```bash
curl -X GET "http://localhost:18080/api/v1/news/symbol/THYAO" \
  -H "Accept: application/json"
```

---

## WebSocket Connection

Real-time price updates via STOMP over WebSocket:

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const socket = new SockJS('http://localhost:18080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Subscribe to currency updates
  stompClient.subscribe('/topic/currencies', (message) => {
    const data = JSON.parse(message.body);
    console.log('Currency update:', data);
  });

  // Subscribe to stock updates
  stompClient.subscribe('/topic/stocks', (message) => {
    const data = JSON.parse(message.body);
    console.log('Stock update:', data);
  });
});
```

---

## Error Responses

All errors follow a consistent format:

```json
{
  "success": false,
  "error": {
    "timestamp": "2026-01-22T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation error message",
    "path": "/api/v1/portfolios",
    "validationErrors": {
      "name": "Name is required"
    }
  }
}
```

### HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request - Validation error |
| 401 | Unauthorized - Invalid/missing token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error |
| 502 | Bad Gateway - External API error |
| 503 | Service Unavailable |

---

## Rate Limits

| User Type | Requests/Minute |
|-----------|-----------------|
| Anonymous | 100 |
| Authenticated | 200 |
| Admin | 500 |

Rate limit headers:
```
X-RateLimit-Limit: 200
X-RateLimit-Remaining: 195
X-RateLimit-Reset: 1706000000
```
