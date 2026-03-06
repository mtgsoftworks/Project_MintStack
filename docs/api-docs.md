# API Referansı

## 1. Genel Bilgiler

| Parametre | Değer |
|---|---|
| **Base URL (Gateway)** | `http://localhost:8088/api/v1` |
| **Swagger UI** | `http://localhost:8088/swagger-ui.html` |
| **OpenAPI JSON** | `http://localhost:8088/api-docs` |
| **OpenAPI JSON (compat)** | `http://localhost:8088/v3/api-docs` |
| **Kimlik Sunucusu** | `http://localhost:8180` |
| **WebSocket** | `ws://localhost:8088/ws` |
| **API Versiyonlama** | URL bazlı: `/api/v1/...` |
| **Aktif Versiyon** | `v1` |
| **İçerik Tipi** | `application/json` |
| **Karakter Kodlaması** | `UTF-8` |

### 1.1 Platform Sürümleri

| Bileşen | Sürüm |
|---|---|
| Backend (Spring Boot) | `3.4.2` |
| Frontend (React) | `18.3.1` |
| Frontend (Vite) | `5.4.21` |
| Kimlik Sunucusu (Keycloak) | `26.5.4` |
| Veritabanı (PostgreSQL) | `15-alpine` |
| Mesajlaşma (Kafka KRaft) | `7.5.0` |

## 2. Kimlik Doğrulama

### Token Alma

```http
POST http://localhost:8180/realms/mintstack-finance/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password
&client_id=finance-frontend
&username=<kullanici_adi>
&password=<sifre>
```

### Token Kullanımı

Korumalı tüm endpoint'ler için `Authorization` header'ı gereklidir:

```http
Authorization: Bearer <access_token>
```

### Token Yenileme

```http
POST http://localhost:8180/realms/mintstack-finance/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&client_id=finance-frontend
&refresh_token=<refresh_token>
```

## 3. Public Endpoint'ler (Auth Gerektirmez)

### Piyasa Verileri

| Metot | Endpoint | Açıklama |
|---|---|---|
| `GET` | `/market/currencies` | Tüm döviz kurları |
| `GET` | `/market/currencies/{code}` | Belirli döviz kuru (örn: USD, EUR) |
| `GET` | `/market/stocks` | Hisse senetleri listesi |
| `GET` | `/market/stocks/{symbol}` | Belirli hisse detayı |
| `GET` | `/market/bonds` | Tahvil/bono verileri |
| `GET` | `/market/funds` | Yatırım fonu verileri |
| `GET` | `/market/viop` | VİOP verileri |
| `GET` | `/market/indices/{symbol}` | Piyasa endeksi (örn: XU100.IS) |

### Haber Akışı

| Metot | Endpoint | Açıklama |
|---|---|---|
| `GET` | `/news` | Haber listesi (sayfalama desteklenir) |
| `GET` | `/news/latest` | Son haberler |
| `GET` | `/news/{id}` | Haber detayı |
| `GET` | `/news/categories` | Haber kategorileri |

## 4. Auth Gerektiren Endpoint'ler

### Portföy Yönetimi

| Metot | Endpoint | Açıklama |
|---|---|---|
| `GET` | `/portfolios` | Kullanıcının portföyleri |
| `POST` | `/portfolios` | Yeni portföy oluştur |
| `GET` | `/portfolios/{id}` | Portföy detayı |
| `PUT` | `/portfolios/{id}` | Portföy güncelle |
| `DELETE` | `/portfolios/{id}` | Portföy sil |
| `GET` | `/portfolios/{id}/summary` | Portföy özeti (nakit, PnL, toplam değer) |
| `GET` | `/portfolios/{id}/items` | Portföy pozisyonları |
| `GET` | `/portfolios/{id}/transactions` | İşlem geçmişi (filtreleme + sayfalama) |

### Emir İşlemleri

| Metot | Endpoint | Açıklama |
|---|---|---|
| `POST` | `/portfolios/{id}/trades` | Emir oluştur (BUY/SELL) |
| `POST` | `/portfolios/{id}/orders/process` | Bekleyen emirleri işle |
| `POST` | `/portfolios/{id}/orders/{orderId}/cancel` | Emri iptal et |
| `POST` | `/portfolios/{id}/cash` | Nakit hareketi (DEPOSIT/WITHDRAW) |

#### Emir Oluşturma İstek Gövdesi

```json
{
  "instrumentId": 1,
  "instrumentSymbol": "THYAO.IS",
  "transactionType": "BUY",
  "orderType": "LIMIT",
  "quantity": 100,
  "price": 250.50,
  "limitPrice": 245.00,
  "stopPrice": null
}
```

| Alan | Tip | Zorunluluk | Açıklama |
|---|---|---|---|
| `instrumentId` | Long | `instrumentId` veya `instrumentSymbol` | Enstrüman ID |
| `instrumentSymbol` | String | zorunlu | Enstrüman sembolü |
| `transactionType` | Enum | ✅ | `BUY`, `SELL` |
| `orderType` | Enum | ✅ | `MARKET`, `LIMIT`, `STOP` |
| `quantity` | Integer | ✅ | Adet |
| `price` | Decimal | MARKET için | İşlem fiyatı |
| `limitPrice` | Decimal | LIMIT için ✅ | Limit fiyat |
| `stopPrice` | Decimal | STOP için ✅ | Stop fiyat |

### İzleme Listesi

| Metot | Endpoint | Açıklama |
|---|---|---|
| `GET` | `/watchlist` | İzleme listesi |
| `POST` | `/watchlist` | Yeni izleme listesi oluştur |
| `POST` | `/watchlist/{id}/items` | Enstrüman ekle |
| `DELETE` | `/watchlist/{id}/items/{itemId}` | Enstrüman çıkar |

### Fiyat Alarmları

| Metot | Endpoint | Açıklama |
|---|---|---|
| `GET` | `/alerts` | Kullanıcının alarmları |
| `POST` | `/alerts` | Yeni alarm oluştur |
| `PUT` | `/alerts/{id}` | Alarm güncelle |
| `DELETE` | `/alerts/{id}` | Alarm sil |

### Kullanıcı

| Metot | Endpoint | Açıklama |
|---|---|---|
| `GET` | `/users/profile` | Kullanıcı profili |
| `PUT` | `/users/profile` | Profil güncelle |
| `GET` | `/users/notifications` | Bildirimler |
| `PUT` | `/users/notifications/{id}/read` | Bildirimi okundu işaretle |

### Veri Kaynağı Tercihleri

| Metot | Endpoint | Açıklama |
|---|---|---|
| `GET` | `/data-sources/preferences` | Mevcut tercihler |
| `POST` | `/data-sources/preferences` | Tercihleri güncelle |
| `GET` | `/data-sources/capabilities` | Mevcut sağlayıcılar ve yetenekleri |
| `POST` | `/data-sources/trigger/{apiConfigId}` | Seçili API anahtarı için anlık veri çekimini tetikle |

Not:
- `Finnhub` anahtarı hisse/kripto için geçerli olsa bile `forex/rates` endpoint erişimi plan bazlı olabilir.
- Uygulama bu durumda `0` değerli kuru geçerli veri olarak kabul etmez ve alternatif sağlayıcı fallback'i dener.

### Teknik Analiz

| Metot | Endpoint | Açıklama |
|---|---|---|
| `POST` | `/analysis/correlation` | Korelasyon matrisi |
| `GET` | `/analysis/indicators/{symbol}` | Teknik indikatörler |
| `POST` | `/backtesting/run` | Backtesting çalıştır |
| `POST` | `/montecarlo/simulate` | Monte Carlo simülasyonu |

### Dışa Aktarım

| Metot | Endpoint | Açıklama |
|---|---|---|
| `GET` | `/portfolios/{id}/export/excel` | Portföyü Excel olarak indir |
| `GET` | `/portfolios/{id}/export/pdf` | Portföyü PDF olarak indir |

## 5. Admin Endpoint'leri (ADMIN rolü gerektirir)

| Metot | Endpoint | Açıklama |
|---|---|---|
| `GET` | `/admin/dashboard` | Admin dashboard verileri |
| `GET` | `/admin/users` | Tüm kullanıcılar |
| `GET` | `/admin/stats` | Sistem istatistikleri |
| `GET` | `/admin/rate-limit` | Aktif rate limit ayarları |
| `PUT` | `/admin/rate-limit` | Runtime rate limit güncelle |
| `POST` | `/simulation/start` | Simülasyonu başlat |
| `POST` | `/simulation/stop` | Simülasyonu durdur |
| `GET` | `/simulation/config` | Simülasyon konfigürasyonu |
| `PUT` | `/simulation/config` | Simülasyon ayarlarını güncelle |

## 6. WebSocket (STOMP)

### Bağlantı

```javascript
const socket = new SockJS('http://localhost:8088/ws')
const stompClient = Stomp.over(socket)
stompClient.connect(
  { Authorization: 'Bearer <token>' },
  () => { /* bağlantı başarılı */ }
)
```

### Topic'ler

| Topic | Açıklama |
|---|---|
| `/topic/prices/currency` | Döviz kuru güncellemeleri |
| `/topic/prices/stocks/{symbol}` | Hisse fiyat güncellemeleri |
| `/topic/prices/all` | Tüm fiyat güncellemeleri |
| `/topic/notifications/{userId}` | Kullanıcı bildirimleri |

### Fiyat Güncelleme İsteği

```javascript
stompClient.send('/app/prices/refresh', {}, JSON.stringify({}))
```

## 7. Hata Yanıtları

API, hata durumlarında standart HTTP durum kodları ve tutarlı JSON yanıt formatı kullanır:

```json
{
  "timestamp": "2026-03-04T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "instrumentId veya instrumentSymbol zorunludur",
  "path": "/api/v1/portfolios/1/trades"
}
```

| Durum Kodu | Açıklama |
|---|---|
| `200` | Başarılı |
| `201` | Oluşturuldu |
| `400` | Hatalı istek (validasyon hatası) |
| `401` | Yetkisiz (token eksik/geçersiz) |
| `403` | Erişim reddedildi (yetersiz rol) |
| `404` | Bulunamadı |
| `429` | Rate limit aşıldı |
| `500` | Sunucu hatası |

## 8. Rate Limiting

API endpoint'lerinde Bucket4j ile rate limiting uygulanır:

- Rate limit aşıldığında `429 Too Many Requests` yanıtı döner
- Rate limit bilgileri response header'larında iletilir
