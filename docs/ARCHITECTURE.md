# MintStack Finance Portal - Proje Mimarisi

## 📋 Genel Bakış

MintStack Finance Portal, **gerçek zamanlı finansal veri takibi**, **portföy yönetimi** ve **teknik analiz** özellikleri sunan kurumsal düzeyde bir finans uygulamasıdır.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND (React)                               │
│                         http://localhost:3001                               │
├─────────────────────────────────────────────────────────────────────────────┤
│  WebSocket (STOMP/SockJS)  │  REST API  │  Keycloak OAuth2                 │
└─────────────┬──────────────┴─────┬──────┴────────────┬─────────────────────┘
              │                    │                   │
              ▼                    ▼                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Spring Boot 3.2)                           │
│                         http://localhost:18080                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ REST API │ │ WebSocket│ │ Scheduler│ │  Kafka   │ │  Cache   │          │
│  │Controller│ │  STOMP   │ │  Jobs    │ │ Producer │ │  Redis   │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
└──────┬────────────┬────────────┬────────────┬────────────┬─────────────────┘
       │            │            │            │            │
       ▼            ▼            ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│PostgreSQL│ │  Redis   │ │  Kafka   │ │ Keycloak │ │OpenSearch│
│  :5432   │ │  :6379   │ │  :29092  │ │  :8180   │ │  :19200  │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

---

## 🔄 Sistem Dinamik mi Çalışıyor?

### ✅ EVET - Canlı Veri Akışı

Sistem, API konfigürasyonları eklendikten sonra **tamamen dinamik** çalışır:

| Özellik | Durum | Açıklama |
|---------|-------|----------|
| **Döviz Kurları** | ✅ Dinamik | TCMB API'den otomatik çekilir (09:00, 12:00, 15:00) |
| **Hisse Fiyatları** | ✅ Dinamik | Yahoo/Alpha Vantage'dan 15 dk'da bir güncellenir |
| **WebSocket** | ✅ Canlı | Anlık fiyat güncellemeleri broadcast edilir |
| **Bildirimler** | ✅ Gerçek Zamanlı | Kafka → WebSocket ile anında iletilir |

### Veri Akış Döngüsü

```
┌─────────────────────────────────────────────────────────────────────┐
│                        VERİ AKIŞ DÖNGÜSÜ                            │
└─────────────────────────────────────────────────────────────────────┘

1. SCHEDULER TETIKLEME
   ┌─────────────┐
   │  Cron Job   │──────► fetchTcmbRates() / fetchStockPrices()
   └─────────────┘
         │
         ▼
2. EXTERNAL API ÇAĞRISI
   ┌─────────────┐     ┌─────────────┐
   │   TCMB API  │     │Yahoo Finance│
   │(Public XML) │     │Alpha Vantage│
   └──────┬──────┘     └──────┬──────┘
          │                   │
          └─────────┬─────────┘
                    ▼
3. VERİ İŞLEME & KAYDETME
   ┌─────────────────────────────────────┐
   │         MarketDataService           │
   │  - Parse XML/JSON                   │
   │  - Save to PostgreSQL               │
   │  - Update Redis Cache               │
   └─────────────────┬───────────────────┘
                     │
          ┌─────────┴─────────┐
          ▼                   ▼
4. BROADCAST (Paralel)
   ┌─────────────┐     ┌─────────────┐
   │  WebSocket  │     │    Kafka    │
   │ /topic/...  │     │mintstack-*  │
   └──────┬──────┘     └──────┬──────┘
          │                   │
          ▼                   ▼
5. TÜKETİCİLER
   ┌─────────────┐     ┌─────────────┐
   │  Frontend   │     │  Logstash   │
   │(SockJS/STOMP)│    │ OpenSearch  │
   └─────────────┘     └─────────────┘
```

---

## 🛠️ Servis Detayları

### 1. Apache Kafka (Port: 29092)

**İşlevi:** Event-driven mesajlaşma sistemi

```java
// Topic'ler
public static final String TOPIC_LOGS = "mintstack-logs";           // Log aggregation
public static final String TOPIC_MARKET_DATA = "mintstack-market-data"; // Fiyat güncellemeleri
public static final String TOPIC_NOTIFICATIONS = "mintstack-notifications"; // Bildirimler
```

**Kullanım Alanları:**
- 📊 **Market Data Events**: Fiyat değişiklikleri Kafka'ya publish edilir
- 🔔 **Notification Events**: Kullanıcı bildirimleri asenkron işlenir
- 📝 **Log Events**: Uygulama logları merkezi sisteme gönderilir

**Örnek Akış:**
```
Scheduler → EventPublisher.publishMarketDataEvent() 
         → Kafka Topic: mintstack-market-data
         → Logstash Consumer → OpenSearch Index
```

---

### 2. Redis Cache (Port: 6379)

**İşlevi:** Yüksek performanslı önbellekleme

**Cache Bölgeleri ve TTL:**
| Cache Adı | TTL | Açıklama |
|-----------|-----|----------|
| `currencyRates` | 5 dakika | Döviz kurları |
| `stockPrices` | 1 dakika | Hisse fiyatları |
| `news` | 10 dakika | Haberler |
| `portfolios` | 2 dakika | Portföy verileri |
| `instruments` | 1 saat | Enstrüman listesi |
| `historicalData` | 6 saat | Tarihsel veriler |

**Faydaları:**
- ⚡ API yanıt süresini ~10x hızlandırır
- 🛡️ External API rate limit'lerini korur
- 💾 Veritabanı yükünü azaltır

---

### 3. WebSocket / STOMP (Port: 8080/ws)

**İşlevi:** Gerçek zamanlı çift yönlü iletişim

**Endpoint'ler:**
```
/ws          → SockJS fallback ile (tarayıcı uyumluluğu)
/ws-native   → Pure WebSocket
```

**Topic'ler:**
```javascript
/topic/prices/currency      → Tüm döviz güncellemeleri
/topic/prices/currency/USD  → Belirli döviz (USD/TRY)
/topic/prices/stocks        → Tüm hisse güncellemeleri
/topic/prices/stocks/THYAO  → Belirli hisse
/user/queue/notifications   → Kullanıcıya özel bildirimler
```

**Frontend Bağlantısı:**
```javascript
// websocketService.js
const client = new Client({
    webSocketFactory: () => new SockJS('http://localhost:18080/ws'),
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
})

// Fiyat güncellemelerini dinle
client.subscribe('/topic/prices/currency', (message) => {
    const data = JSON.parse(message.body)
    // UI güncelle
})
```

---

### 4. OpenSearch (Port: 19200)

**İşlevi:** Log aggregation & full-text search

**Index'ler:**
- `mintstack-logs-YYYY.MM.dd` → Uygulama logları
- `mintstack-traces` → Distributed tracing verileri

**Veri Akışı:**
```
Backend → Kafka (mintstack-logs) → Logstash → OpenSearch
                                              ↓
                        OpenSearch Dashboards (Port: 15601)
```

---

### 5. OpenTelemetry Collector (Port: 4317/4318)

**İşlevi:** Observability (Traces, Metrics, Logs)

**Pipeline:**
```yaml
receivers:
  otlp (gRPC: 4317, HTTP: 4318)
       ↓
processors:
  batch, memory_limiter, resource
       ↓
exporters:
  - elasticsearch → OpenSearch (logs, traces)
  - prometheus → Metrics (:8889)
```

**Backend Entegrasyonu:**
```yaml
# application.yml
otel:
  exporter:
    otlp:
      endpoint: http://otel-collector:4317
  service:
    name: finance-portal
```

---

### 6. Logstash (Kafka → OpenSearch)

**İşlevi:** Log transformation & routing

**Pipeline:**
```
Input:  Kafka (mintstack-logs topic)
Filter: JSON parsing, trace context extraction
Output: OpenSearch (mintstack-logs-* index)
```

---

### 7. Keycloak (Port: 8180)

**İşlevi:** Identity & Access Management

**Özellikler:**
- 🔐 OAuth2 / OpenID Connect
- 👥 LDAP Federation (OpenLDAP)
- 🔑 2FA (TOTP) desteği
- 🎭 Role-based access (user, admin)

**Realm:** `mintstack-finance`
**Clients:**
- `finance-backend` (bearer-only)
- `finance-frontend` (public, PKCE)

---

### 8. PostgreSQL (Port: 5432)

**İşlevi:** Ana veri deposu

**Veritabanları:**
- `mintstack_finance` → Ana uygulama verileri
- `keycloak` → Keycloak verileri

**Tablolar:**
```
users, portfolios, portfolio_items, portfolio_transactions,
instruments, price_history, currency_rates, news, news_categories,
watchlists, watchlist_items, price_alerts, user_notifications,
user_api_configs
```

---

## 📊 Scheduler (Zamanlanmış Görevler)

| Görev | Cron | Açıklama |
|-------|------|----------|
| **TCMB Kurları** | `0 0 9,12,15 * * MON-FRI` | Hafta içi 09:00, 12:00, 15:00 |
| **Hisse Fiyatları** | `0 */15 9-18 * * MON-FRI` | Piyasa saatlerinde her 15 dk |
| **Haber Çekme** | `0 */30 * * * *` | Her 30 dakikada |
| **Temizlik** | `0 0 2 * * *` | Her gece 02:00 |
| **Bootstrap** | `fixedDelay=60000` | DB boşsa veri yükle |

**Koşullu Çalışma:**
```java
@Scheduled(cron = "${app.scheduler.tcmb-rates-cron}")
public void fetchTcmbRates() {
    UserApiConfig tcmbConfig = getActiveConfig(ApiProvider.TCMB);
    if (tcmbConfig == null) {
        return; // API yapılandırılmamışsa çalışma
    }
    // ...
}
```

---

## 🔌 External API Entegrasyonları

### TCMB (Türkiye Cumhuriyet Merkez Bankası)
- **URL:** `https://www.tcmb.gov.tr/kurlar/YYYYMM/DDMMYYYY.xml`
- **Format:** XML
- **Auth:** Public (API key gerektirmez, sadece config kaydı gerekli)
- **Veri:** Döviz kurları (alış/satış, efektif alış/satış)

### Yahoo Finance
- **URL:** `https://query1.finance.yahoo.com/v8/finance`
- **Format:** JSON
- **Auth:** API Key (opsiyonel)
- **Veri:** Hisse fiyatları, tarihsel veriler

### Alpha Vantage
- **URL:** `https://www.alphavantage.co/query`
- **Format:** JSON
- **Auth:** API Key (zorunlu)
- **Veri:** Global hisse verileri, teknik göstergeler

---

## 🖥️ Frontend Mimarisi

```
frontend/src/
├── components/
│   ├── ui/           → shadcn/ui bileşenleri
│   ├── common/       → Ortak bileşenler (LanguageSwitcher, etc.)
│   ├── layout/       → Header, Sidebar, Footer
│   └── charts/       → Grafik bileşenleri
├── pages/            → Sayfa bileşenleri
├── store/
│   ├── slices/       → Redux slices (uiSlice, authSlice)
│   └── api/          → RTK Query API'ları
├── services/
│   ├── websocketService.js  → WebSocket yönetimi
│   └── api.js               → Axios instance
├── hooks/
│   └── usePriceUpdates.js   → WebSocket hook
└── context/          → React Context'ler
```

**State Management:**
- **Redux Toolkit** → Global state
- **RTK Query** → API caching & data fetching
- **localStorage** → Persisted settings (theme, language, etc.)

---

## 🔒 Güvenlik Katmanları

1. **Authentication:** Keycloak OAuth2 + JWT
2. **Authorization:** Spring Security + Role-based access
3. **Rate Limiting:** Bucket4j (100/200/500 req/min)
4. **CORS:** Whitelist-based origin control
5. **HTTPS:** Production'da zorunlu

---

## 📦 Docker Compose Servisleri

| Servis | Port | Sağlık Kontrolü |
|--------|------|-----------------|
| PostgreSQL | 5432 | ✅ `pg_isready` |
| Redis | 6379 | ✅ `redis-cli ping` |
| Kafka | 29092 | ✅ `kafka-topics --list` |
| Keycloak | 8180 | ✅ `/health/ready` |
| OpenSearch | 19200 | ✅ `/_cluster/health` |
| Backend | 18080 | ✅ `/actuator/health` |
| Frontend | 3001 | ✅ HTTP check |

---

## 🚀 Sistem Başlatma Sırası

```
1. PostgreSQL → 2. Redis → 3. OpenLDAP → 4. Keycloak
         ↓                        ↓
    5. Zookeeper → 6. Kafka → 7. OpenSearch
                        ↓
              8. Logstash, OTEL Collector
                        ↓
              9. Backend → 10. Frontend
```

---

## 📈 Performans Metrikleri

- **API Yanıt Süresi:** < 100ms (cache hit), < 500ms (cache miss)
- **WebSocket Latency:** < 50ms
- **Kafka Throughput:** ~10,000 msg/sec
- **Redis Hit Rate:** > 90%

---

## 🛠️ Geliştirici Notları

### API Key Ekleme (Veri akışını başlatmak için)
1. Settings → API Keys → Add New
2. Provider seç (TCMB, Yahoo Finance, Alpha Vantage)
3. API Key gir (TCMB için herhangi bir değer)
4. Test & Kaydet

### Logları İzleme
```bash
# Backend logları
docker logs -f mintstack-backend

# Kafka topic'leri
docker exec mintstack-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic mintstack-market-data --from-beginning
```

### WebSocket Test
```javascript
// Browser console'da
const ws = new WebSocket('ws://localhost:18080/ws-native');
ws.onmessage = (e) => console.log(JSON.parse(e.data));
```

---

*Son Güncelleme: Ocak 2026*
