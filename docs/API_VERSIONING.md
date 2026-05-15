# API Versiyonlama Dokümantasyonu

Bu doküman, MintStack Finance Portal REST API'sinin versiyonlama stratejisini ve kullanımını açıklar.

---

## 1. Versiyonlama Stratejisi

### 1.1 Yaklaşım: Hibrit Model

Proje **hibrit versiyonlama** kullanır:

| Bileşen | Yöntem | Açıklama |
|---------|--------|----------|
| **URL** | Path-based | Tüm endpoint'ler `/api/v1/*` prefix'i ile erişilir |
| **Response Headers** | Header-based | Yanıtlarda `X-API-Version`, `X-API-Deprecated`, `X-API-Sunset` ile versiyon bilgisi iletilir |

### 1.2 Neden Bu Yaklaşım?

- **URL tabanlı**: Net endpoint ayrımı, kolay routing, istemci tarafında basit base URL yönetimi
- **Header tabanlı**: Yanıtta kullanılan sürümün açık iletilmesi, deprecation uyarıları, sunset tarihi bildirimi
- **ADR-009** ile kararlaştırılmıştır (bkz. `docs/ADR.md`)

### 1.3 Teknik Uygulama

- **ApiVersioningConfig**: Spring Boot konfigürasyon sınıfı
- **ApiVersionFilter**: `OncePerRequestFilter` ile tüm `/api/*` isteklerine response header ekler
- **Url pattern**: `/api/*` (order: 1)

---

## 2. Kullanım

### 2.1 İstekte Versiyon Belirtme

**URL path** zorunludur; tüm API çağrıları `/api/v1/` ile başlamalıdır:

```
GET /api/v1/market/currencies
GET /api/v1/portfolios
POST /api/v1/alerts
```

### 2.2 İsteğe Opsiyonel Header (Gelecek Kullanım)

Gelecekte birden fazla sürüm desteklendiğinde, istemci `X-API-Version` header'ı ile tercih edilen sürümü belirtebilir:

```http
GET /api/v1/market/currencies
X-API-Version: 1.0.0
```

Şu an tek sürüm (v1) olduğu için bu header zorunlu değildir.

### 2.3 Yanıt Header'ları

Her API yanıtında aşağıdaki header'lar bulunur:

| Header | Örnek Değer | Açıklama |
|--------|--------------|----------|
| `X-API-Version` | 1.0.0 | Kullanılan API sürümü |
| `X-API-Min-Version` | 1.0.0 | Minimum desteklenen sürüm |
| `X-API-Deprecated` | true | Sürüm deprecated ise (şu an yok) |
| `X-API-Sunset` | Wed, 01 Sep 2026 00:00:00 GMT | Kaldırılma tarihi (RFC 7231) |

---

## 3. Mevcut Versiyonlar

| Versiyon | Durum | Base Path | Açıklama |
|----------|-------|-----------|----------|
| **v1** | Aktif (Stable) | `/api/v1` | Mevcut kararlı sürüm |

### Semantik Versiyonlama

- **Major (1.x.x)**: Breaking change'ler; yeni base path (`/api/v2`)
- **Minor (x.1.x)**: Geriye dönük uyumlu yeni özellikler
- **Patch (x.x.1)**: Sadece hata düzeltmeleri

---

## 4. Endpoint Listesi

Tüm endpoint'ler `/api/v1` prefix'i altındadır. Base URL: `http://localhost:8088/api/v1` (Gateway üzerinden).

### 4.1 Market (Piyasa Verileri) – Public

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/market/currencies` | Tüm döviz kurları |
| GET | `/market/currencies/{code}` | Belirli döviz kuru |
| GET | `/market/currencies/{code}/history` | Döviz kuru geçmişi |
| GET | `/market/stocks` | Hisse senetleri listesi |
| GET | `/market/stocks/{symbol}` | Hisse senedi detayı |
| GET | `/market/stocks/{symbol}/history` | Hisse fiyat geçmişi |
| GET | `/market/bonds` | Tahvil/bono listesi |
| GET | `/market/funds` | Yatırım fonları |
| GET | `/market/viop` | VİOP enstrümanları |
| GET | `/market/indices/{symbol}` | Piyasa endeksi |
| GET | `/market/search` | Enstrüman arama |

### 4.2 News (Haberler) – Public

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/news` | Haber listesi |
| GET | `/news/latest` | Son haberler |
| GET | `/news/featured` | Öne çıkan haberler |
| GET | `/news/{id}` | Haber detayı |
| GET | `/news/search` | Haber arama |
| GET | `/news/category/{slug}` | Kategoriye göre haberler |
| GET | `/news/categories` | Haber kategorileri |
| POST | `/news/{id}/view` | Görüntülenme sayacı artır |

### 4.3 Portfolios (Portföy) – Auth

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/portfolios` | Portföy listesi |
| GET | `/portfolios/summary` | Genel portföy özeti |
| GET | `/portfolios/{id}` | Portföy detayı |
| POST | `/portfolios` | Yeni portföy oluştur |
| PUT | `/portfolios/{id}` | Portföy güncelle |
| DELETE | `/portfolios/{id}` | Portföy sil |
| POST | `/portfolios/{id}/items` | Enstrüman ekle |
| DELETE | `/portfolios/{id}/items/{itemId}` | Enstrüman çıkar |
| POST | `/portfolios/{id}/trades` | Al/sat işlemi |
| POST | `/portfolios/{id}/orders/process` | Bekleyen emirleri işle |
| POST | `/portfolios/{id}/orders/{orderId}/cancel` | Emir iptal |
| POST | `/portfolios/{id}/cash` | Nakit hareketi |
| GET | `/portfolios/{id}/transactions` | İşlem geçmişi |
| GET | `/portfolios/{id}/summary` | Portföy özeti |
| GET | `/portfolios/{id}/export/excel` | Excel export |
| GET | `/portfolios/{id}/export/pdf` | PDF export |

### 4.4 Watchlist (İzleme Listesi) – Auth

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/watchlist` | İzleme listeleri |
| GET | `/watchlist/{id}` | İzleme listesi detayı |
| POST | `/watchlist` | Yeni liste oluştur |
| PUT | `/watchlist/{id}` | Liste güncelle |
| DELETE | `/watchlist/{id}` | Liste sil |
| POST | `/watchlist/{id}/items/{symbol}` | Enstrüman ekle |
| DELETE | `/watchlist/{id}/items/{symbol}` | Enstrüman çıkar |

### 4.5 Alerts (Fiyat Alarmları) – Auth

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/alerts` | Kullanıcı alarmları |
| GET | `/alerts/active` | Aktif alarmlar |
| POST | `/alerts` | Yeni alarm oluştur |
| DELETE | `/alerts/{id}` | Alarm sil |
| PUT | `/alerts/{id}/deactivate` | Alarmı devre dışı bırak |

### 4.6 Users (Kullanıcı) – Auth

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/users/me` | Profil |
| PUT | `/users/me` | Profil güncelle |
| GET | `/users/me/preferences` | Tercihler |
| PUT | `/users/me/preferences` | Tercihleri güncelle |
| GET | `/users/me/notifications` | Bildirimler |
| POST | `/users/me/notifications/{id}/read` | Bildirimi okundu işaretle |
| POST | `/users/me/notifications/read-all` | Tümünü okundu işaretle |

### 4.7 Data Sources (Veri Kaynakları) – Auth

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/data-sources/capabilities` | Sağlayıcı yetenekleri |
| GET | `/data-sources/preferences` | Tercihler |
| POST | `/data-sources/preferences` | Tercih güncelle |
| POST | `/data-sources/trigger/{apiConfigId}` | Anlık veri çekimi tetikle |

### 4.8 Settings (Ayarlar) – Auth

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/settings/api-keys` | API anahtarları |
| GET | `/settings/api-keys/providers` | Sağlayıcı listesi |
| POST | `/settings/api-keys/test` | Anahtar test |
| POST | `/settings/api-keys` | Yeni anahtar ekle |
| DELETE | `/settings/api-keys/{id}` | Anahtar sil |
| DELETE | `/settings/cache` | Önbellek temizle |
| DELETE | `/settings/market-data` | Piyasa verisi temizle |

### 4.9 Analysis (Analiz) – Auth

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/analysis/ma/{symbol}` | Hareketli ortalama |
| GET | `/analysis/ma/multiple/{symbol}` | Çoklu MA |
| GET | `/analysis/trend/{symbol}` | Trend analizi |
| POST | `/analysis/compare` | Enstrüman karşılaştırma |

### 4.10 Technical Indicators (Teknik Göstergeler) – Auth

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/indicators/rsi/{symbol}` | RSI |
| GET | `/indicators/macd/{symbol}` | MACD |
| GET | `/indicators/bollinger/{symbol}` | Bollinger Bands |
| GET | `/indicators/sma/{symbol}` | SMA |
| GET | `/indicators/ema/{symbol}` | EMA |
| GET | `/indicators/stochastic/{symbol}` | Stochastic |
| GET | `/indicators/all/{symbol}` | Tüm göstergeler |

### 4.11 Backtesting – Auth

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| POST | `/backtest/run` | Backtest çalıştır |
| GET | `/backtest/strategies` | Strateji listesi |
| GET | `/backtest/quick/{symbol}` | Hızlı backtest |
| GET | `/backtest/compare/{symbol}` | Strateji karşılaştırma |

### 4.12 Monte Carlo – Auth

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| POST | `/montecarlo/simulate` | Monte Carlo simülasyonu |
| GET | `/montecarlo/var/{symbol}` | VaR hesapla |
| POST | `/montecarlo/portfolio-risk` | Portföy risk analizi |

### 4.13 Admin – ADMIN Rolü

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/admin/dashboard` | Admin dashboard |
| GET | `/admin/users` | Kullanıcı listesi |
| GET | `/admin/users/{id}` | Kullanıcı detayı |
| GET | `/admin/users/search` | Kullanıcı arama |
| PUT | `/admin/users/{id}/activate` | Kullanıcı aktifleştir |
| PUT | `/admin/users/{id}/deactivate` | Kullanıcı devre dışı bırak |
| GET | `/admin/rate-limit` | Rate limit ayarları |
| PUT | `/admin/rate-limit` | Rate limit güncelle |
| GET | `/admin/observability/logs` | Log arama |
| GET | `/admin/observability/logs/trace/{traceId}` | Trace logları |
| GET | `/admin/observability/logs/recent` | Son loglar |
| GET | `/admin/observability/logs/stats` | Log istatistikleri |
| GET | `/admin/observability/metrics` | Sistem metrikleri |
| GET | `/admin/observability/health/detailed` | Detaylı sağlık |

### 4.14 Simulation – ADMIN Rolü

| Metot | Endpoint | Açıklama |
|-------|----------|----------|
| GET | `/simulation/config` | Simülasyon ayarları |
| POST | `/simulation/config` | Ayarları güncelle |
| POST | `/simulation/toggle` | Simülasyon aç/kapat |
| POST | `/simulation/reset` | Simülasyonu sıfırla |
| GET | `/simulation/status` | Durum |
| GET | `/simulation/metrics` | Metrikler |
| GET | `/simulation/health` | Sağlık kontrolü |
| GET | `/simulation/stocks` | Simüle hisseler |
| GET | `/simulation/currencies` | Simüle dövizler |
| GET | `/simulation/indices` | Simüle endeksler |
| GET | `/simulation/cryptos` | Deprecated: kripto modülü aktif kullanıcı kapsamından çıkarıldı |
| GET | `/simulation/volatility` | Volatilite |
| POST | `/simulation/volatility/burst` | Volatilite patlaması |
| GET | `/simulation/events` | Piyasa olayları |
| GET | `/simulation/events/types` | Olay tipleri |
| POST | `/simulation/events/trigger` | Olay tetikle |
| GET | `/simulation/events/symbol/{symbol}` | Sembole göre olaylar |
| DELETE | `/simulation/events` | Olayları temizle |

---

## 5. Deprecation Politikası

### 5.1 Kurallar

- **Deprecated sürümler** en az **6 ay** desteklenir
- **Breaking change** sadece major versiyon artışında yapılır
- **Minor** sürümler geriye dönük uyumludur
- **Patch** sürümler sadece hata düzeltmesi içerir

### 5.2 Deprecation Bildirimi

Deprecated bir sürüm kullanıldığında yanıtta:

```
X-API-Deprecated: true
X-API-Sunset: Wed, 01 Sep 2026 00:00:00 GMT
```

header'ları gönderilir. İstemciler bu tarihten önce yeni sürüme geçmelidir.

### 5.3 Geçiş Süreci

1. Yeni sürüm yayınlanır (örn. v2)
2. Eski sürüm (v1) deprecated olarak işaretlenir
3. 6 ay boyunca her iki sürüm desteklenir
4. Sunset tarihinde v1 kaldırılır

---

## 6. Örnekler

### 6.1 curl ile Public Endpoint

```bash
curl -X GET "http://localhost:8088/api/v1/market/currencies" \
  -H "Accept: application/json"
```

### 6.2 curl ile Auth Gerektiren Endpoint

```bash
curl -X GET "http://localhost:8088/api/v1/portfolios" \
  -H "Authorization: Bearer <access_token>" \
  -H "Accept: application/json" \
  -H "X-API-Version: 1.0.0"
```

### 6.3 Yanıt Örneği (Header'lar)

```http
HTTP/1.1 200 OK
Content-Type: application/json
X-API-Version: 1.0.0
X-API-Min-Version: 1.0.0

{
  "data": [...],
  "success": true,
  "message": null
}
```

### 6.4 JavaScript (Fetch)

```javascript
const response = await fetch('http://localhost:8088/api/v1/market/currencies', {
  headers: {
    'Accept': 'application/json',
    'X-API-Version': '1.0.0'
  }
});
const version = response.headers.get('X-API-Version');
const data = await response.json();
```

---

## API Dokümantasyonu

| Kaynak | URL |
|--------|-----|
| Swagger UI | http://localhost:8088/swagger-ui.html |
| OpenAPI JSON | http://localhost:8088/api-docs |
| OpenAPI v3 | http://localhost:8088/v3/api-docs |

---

## İlgili Dokümantasyon

- [docs/ADR.md](./ADR.md) - ADR-009: API Versioning
- [docs/api-docs.md](./api-docs.md) - API referansı
- [AGENTS.md](../AGENTS.md) - Proje genel bilgileri
