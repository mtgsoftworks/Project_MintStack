# MintStack Finance Portal - API Documentation

## Base URL
```
http://localhost:8080/api/v1
```

## Authentication
Bu API, OAuth2/OpenID Connect (Keycloak) kullanır. Korumalı endpoint'ler için JWT token gereklidir.

```
Authorization: Bearer <token>
```

## Endpoints

### Market Data

#### Döviz Kurları
```http
GET /market/currencies
```
Tüm güncel döviz kurlarını döndürür.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "currencyCode": "USD",
      "currencyName": "ABD Doları",
      "buyingRate": 34.1234,
      "sellingRate": 34.5678,
      "source": "TCMB",
      "fetchedAt": "2026-01-09T12:00:00Z"
    }
  ]
}
```

#### Tek Döviz Kuru
```http
GET /market/currencies/{code}
```

#### Döviz Kur Geçmişi
```http
GET /market/currencies/{code}/history?startDate=2025-01-01&endDate=2026-01-09
```

#### Hisse Senetleri
```http
GET /market/stocks?search=THYAO&page=0&size=20
```

#### Hisse Detayı
```http
GET /market/stocks/{symbol}
```

#### Hisse Fiyat Geçmişi
```http
GET /market/stocks/{symbol}/history?days=30
```

#### Tahvil/Bono
```http
GET /market/bonds?page=0&size=20
```

#### Fonlar
```http
GET /market/funds?search=AFT&page=0&size=20
```

#### VIOP
```http
GET /market/viop?page=0&size=20
```

#### Arama
```http
GET /market/search?query=turk
```

---

### Haberler

#### Haber Listesi
```http
GET /news?category=doviz&search=usd&page=0&size=10
```

#### Son Haberler
```http
GET /news/latest
```

#### Öne Çıkan Haberler
```http
GET /news/featured
```

#### Haber Detayı
```http
GET /news/{id}
```

#### Kategoriler
```http
GET /news/categories
```

---

### Portföy Yönetimi (Kimlik Doğrulama Gerekli)

#### Portföy Listesi
```http
GET /portfolios
```

#### Portföy Detayı
```http
GET /portfolios/{id}
```

#### Portföy Oluştur
```http
POST /portfolios
Content-Type: application/json

{
  "name": "Ana Portföy",
  "description": "Uzun vadeli yatırımlar",
  "isDefault": false
}
```

#### Portföy Güncelle
```http
PUT /portfolios/{id}
Content-Type: application/json

{
  "name": "Güncellenmiş İsim",
  "description": "Yeni açıklama"
}
```

#### Portföy Sil
```http
DELETE /portfolios/{id}
```

#### Enstrüman Ekle
```http
POST /portfolios/{id}/items
Content-Type: application/json

{
  "instrumentId": "uuid",
  "quantity": 100,
  "purchasePrice": 150.50,
  "purchaseDate": "2026-01-01",
  "notes": "İlk alım"
}
```

#### Enstrüman Çıkar
```http
DELETE /portfolios/{id}/items/{itemId}
```

#### Portföy Özeti
```http
GET /portfolios/{id}/summary
```

---

### Analiz (Kimlik Doğrulama Gerekli)

#### Hareketli Ortalama
```http
GET /analysis/ma/{symbol}?period=20&endDate=2026-01-09
```

#### Çoklu Hareketli Ortalama (MA7, MA25, MA99)
```http
GET /analysis/ma/multiple/{symbol}?endDate=2026-01-09
```

#### Trend Analizi
```http
GET /analysis/trend/{symbol}?days=30
```

**Response:**
```json
{
  "success": true,
  "data": {
    "symbol": "THYAO",
    "period": 30,
    "trend": "UPTREND",
    "trendStrength": "STRONG",
    "changePercent": 15.23,
    "volatility": 0.0234,
    "highPrice": 320.50,
    "lowPrice": 278.00
  }
}
```

#### Enstrüman Karşılaştırma
```http
POST /analysis/compare
Content-Type: application/json

{
  "symbols": ["THYAO", "PGSUS"],
  "startDate": "2025-12-01",
  "endDate": "2026-01-09"
}
```

---

### Kullanıcı (Kimlik Doğrulama Gerekli)

#### Profil Bilgisi
```http
GET /users/profile
```

#### Profil Güncelle
```http
PUT /users/profile
Content-Type: application/json

{
  "firstName": "Ad",
  "lastName": "Soyad"
}
```

---

## Response Format

### Başarılı Response
```json
{
  "timestamp": "2026-01-09T12:00:00Z",
  "success": true,
  "message": "İşlem başarılı",
  "data": { ... },
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false
  }
}
```

### Hata Response
```json
{
  "timestamp": "2026-01-09T12:00:00Z",
  "success": false,
  "error": {
    "status": 400,
    "error": "Bad Request",
    "message": "Geçersiz parametre",
    "path": "/api/v1/portfolios",
    "validationErrors": {
      "name": "Portföy adı zorunludur"
    }
  }
}
```

---

## Hata Kodları

| Kod | Açıklama |
|-----|----------|
| 400 | Bad Request - Geçersiz istek |
| 401 | Unauthorized - Kimlik doğrulama gerekli |
| 403 | Forbidden - Yetkisiz erişim |
| 404 | Not Found - Kaynak bulunamadı |
| 500 | Internal Server Error - Sunucu hatası |
| 502 | Bad Gateway - Harici API hatası |
| 503 | Service Unavailable - Servis kullanılamıyor |

---

## Swagger UI
API dokümantasyonuna tarayıcıdan erişebilirsiniz:
```
http://localhost:8080/swagger-ui.html
```
