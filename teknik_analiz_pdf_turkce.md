# Teknik Analiz DokÃ¼manÄ± - MintStack Finance Portal

**SÃ¼rÃ¼m:** 2.0  
**Tarih:** 05 Mart 2026  
**HazÄ±rlayan:** Proje Ekibi  
**DokÃ¼man TÃ¼rÃ¼:** Teknik Analiz, Mimari ve Modelleme DokÃ¼manÄ±

---

## 1) Ã–zet

Bu dokÃ¼man, MintStack Finance Portal projesinin teknik analizini uÃ§tan uca tanÄ±mlar. AmaÃ§; toplantÄ±, jÃ¼ri deÄŸerlendirmesi ve geliÅŸtirme sÃ¼recinde tÃ¼m ekiplerin tek bir teknik referans Ã¼zerinde hizalanmasÄ±nÄ± saÄŸlamaktÄ±r.

### 1.1 AmaÃ§

- Sistem mimarisini, veri akÄ±ÅŸlarÄ±nÄ±, iÅŸ kurallarÄ±nÄ± ve teknik kararlarÄ± netleÅŸtirmek.
- Gereksinimleri test edilebilir ve izlenebilir biÃ§imde belgelemek.
- GÃ¼venlik, performans ve operasyonel iÅŸletim yaklaÅŸÄ±mÄ±nÄ± aÃ§Ä±k hale getirmek.
- Sonraki iterasyonlar iÃ§in teknik borÃ§ ve iyileÅŸtirme alanlarÄ±nÄ± gÃ¶rÃ¼nÃ¼r kÄ±lmak.

### 1.2 Sistem TanÄ±mÄ±

MintStack Finance Portal:

- TÃ¼rkiye odaklÄ± finans verisi izleme platformudur.
- DÃ¶viz, hisse, tahvil/bono, fon, VÄ°OP, endeks ve kripto verilerini yÃ¶netir.
- Sanal portfÃ¶y, emir yÃ¶netimi (MARKET/LIMIT/STOP), teknik analiz, simÃ¼lasyon ve bildirim yetenekleri sunar.
- OAuth2/OIDC (Keycloak), RBAC, 2FA, observability ve log analizi ile kurumsal seviyede Ã§alÄ±ÅŸÄ±r.

### 1.3 BaÅŸarÄ± Kriterleri

- KullanÄ±cÄ±, tek bir arayÃ¼zden piyasa verisi + portfÃ¶y + teknik analiz + haber akÄ±ÅŸÄ±nÄ± gÃ¶rebilmelidir.
- GerÃ§ek veri yoksa simÃ¼lasyon verisi Ã¼retimi kesintisiz devam etmelidir.
- SimÃ¼lasyon kaynaklÄ± veriler kullanÄ±cÄ±ya aÃ§Ä±k biÃ§imde iÅŸaretlenmelidir.
- Kritik iÅŸlevler (emir iÅŸleme, kimlik doÄŸrulama, veri toplama) test edilebilir olmalÄ±dÄ±r.

---

## 2) Kapsam

### 2.1 Kapsam Dahili

Bu dokÃ¼man aÅŸaÄŸÄ±daki alanlarÄ± kapsar:

- Frontend (React + TypeScript + Vite) sayfalarÄ± ve API tÃ¼ketim katmanÄ±.
- Backend (Spring Boot) controller/service/repository katmanlarÄ±.
- PortfÃ¶y yÃ¶netimi ve emir yaÅŸam dÃ¶ngÃ¼sÃ¼.
- Teknik analiz APIâ€™leri (MA, trend, RSI, MACD, Bollinger, Stochastic).
- SimÃ¼lasyon motoru (fiyat + haber + rastgele piyasa olaylarÄ±).
- Veri kaynaklarÄ± (TCMB, Yahoo, Alpha Vantage, Finnhub) ve provider seÃ§imi.
- Cache, mesajlaÅŸma, loglama ve gÃ¶zlemlenebilirlik (Redis, Kafka, OpenSearch, Prometheus, Grafana, OTEL).
- Kimlik ve yetki altyapÄ±sÄ± (Keycloak + OpenLDAP).
- Docker Compose ile geliÅŸtirme ve Ã¼retim topolojisi.

### 2.2 Kapsam Harici

- Mobil uygulama geliÅŸtirmesi.
- Algoritmik yÃ¼ksek frekanslÄ± iÅŸlem (HFT) motoru.
- GerÃ§ek para ile canlÄ± emir iletimi ve aracÄ± kurum entegrasyonu.
- Yasal uyum (SPK/MiFID) denetim sÃ¼reÃ§lerinin tam otomasyonu.
- Kubernetes tabanlÄ± daÄŸÄ±tÄ±m (mevcut durumda Docker Compose ana akÄ±ÅŸ).

### 2.3 VarsayÄ±mlar

- DÄ±ÅŸ APIâ€™lerde dÃ¶nemsel kesintiler olabilir; sistem fallback/simÃ¼lasyon ile Ã§alÄ±ÅŸmayÄ± sÃ¼rdÃ¼rÃ¼r.
- GeliÅŸtirme ortamÄ± tek makinede Docker ile ayaÄŸa kalkar.
- KullanÄ±cÄ±lar OIDC tabanlÄ± login akÄ±ÅŸÄ±nÄ± kullanÄ±r.

### 2.4 Teknoloji YÄ±ÄŸÄ±nÄ± ve SÃ¼rÃ¼m Matrisi

Bu bÃ¶lÃ¼mdeki sÃ¼rÃ¼mler proje dosyalarÄ±ndan doÄŸrulanmÄ±ÅŸtÄ±r (`backend/pom.xml`, Maven dependency tree, `frontend/package.json`/lockfile, `docker-compose*.yml`).

| Katman | Teknoloji | SÃ¼rÃ¼m |
|---|---|---|
| Backend | Java | 17 |
| Backend | Spring Boot | 3.4.2 |
| Backend | Spring Security Config | 6.4.2 |
| Backend | Spring Data JPA | 3.4.2 |
| Backend | Spring Kafka | 3.3.2 |
| Backend | Spring WebSocket / WebFlux | 6.2.2 / 6.2.2 |
| Backend | Flyway | 10.20.1 |
| Backend | Resilience4j / Bucket4j | 2.2.0 / 8.7.0 |
| Backend | Quartz / Log4j2 JSON Layout | 2.3.2 / 2.24.3 |
| Frontend | React / React DOM | 18.3.1 / 18.3.1 |
| Frontend | TypeScript / Vite | 5.9.3 / 5.4.21 |
| Frontend | Redux Toolkit | 2.11.2 |
| Frontend | React Router DOM | 6.30.3 |
| Frontend | Tailwind CSS | 3.4.19 |
| Frontend | Keycloak JS | 26.2.3 |
| Frontend | STOMP.js / SockJS | 7.2.1 / 1.6.1 |
| Frontend | i18next / Recharts | 23.16.8 / 2.15.4 |
| Test | Vitest / Coverage V8 | 1.6.1 / 1.6.1 |
| Test | Playwright / Testcontainers | 1.57.0 / 1.19.3 |
| AltyapÄ± | PostgreSQL / Redis | 15-alpine / 7-alpine |
| AltyapÄ± | Keycloak / OpenLDAP | 26.5.4 / 1.5.0 |
| AltyapÄ± | Kafka (KRaft) | 7.5.0 |
| AltyapÄ± | OpenSearch / Dashboards | 2.13.0 / 2.13.0 |
| AltyapÄ± | Logstash / OTEL Collector | 8.9.0 / 0.91.0 |
| AltyapÄ± | Prometheus / Grafana / AlertManager | 2.48.0 / 10.2.2 / 0.26.0 |

---

## 3) Terminoloji ve Standartlar

### 3.1 Terminoloji

| Terim | AÃ§Ä±klama |
|---|---|
| API Gateway | Nginx ile tek giriÅŸ noktasÄ± (`:8088`) |
| OIDC | OpenID Connect kimlik katmanÄ± |
| RBAC | Role-Based Access Control |
| STOMP | WebSocket mesajlaÅŸma protokolÃ¼ |
| KRaft | Zookeeperâ€™sÄ±z Kafka Ã§alÄ±ÅŸma modu |
| SimÃ¼lasyon Verisi | GerÃ§ek dÄ±ÅŸ kaynak yerine sistemin Ã¼rettiÄŸi veri |
| isSimulated | EnstrÃ¼manÄ±n simÃ¼le edildiÄŸini belirten alan |
| MARKET/LIMIT/STOP | Emir tipleri |

### 3.2 Kod ve Mimari StandartlarÄ±

- Backend: KatmanlÄ± mimari (`controller -> service -> repository`).
- Frontend: Fonksiyonel bileÅŸenler + hookâ€™lar + RTK Query.
- API sÃ¶zleÅŸmeleri: `/api/v1/...` versiyonlu URL yaklaÅŸÄ±mÄ±.
- Veri modeli: Flyway migrasyonlarÄ± ile yÃ¶netilir (19+ migration).
- Log formatÄ±: YapÄ±sal log + merkezi indeksleme (OpenSearch).

### 3.3 Ä°simlendirme KurallarÄ±

- Endpoint: Ã§oÄŸul kaynak adÄ± (`/portfolios`, `/market/stocks`).
- Entity alanlarÄ±: anlaÅŸÄ±lÄ±r, alan odaklÄ±.
- Testler: `*Test.java`, `*.test.ts(x)` kalÄ±bÄ±.
- Ortam deÄŸiÅŸkenleri: `UPPER_SNAKE_CASE`.

### 3.4 Hata StandardÄ±

Hata yanÄ±tÄ± JSON formatÄ±:

```json
{
  "timestamp": "2026-03-05T12:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validasyon hatasÄ±",
  "path": "/api/v1/portfolios/{id}/trades"
}
```

---

## 4) Gereksinimler

### 4.1 Fonksiyonel Gereksinimler

| ID | Gereksinim | Kabul Kriteri |
|---|---|---|
| FR-001 | Sistem, kullanÄ±cÄ±yÄ± Keycloak ile doÄŸrulamalÄ±dÄ±r. | GeÃ§erli JWT ile korumalÄ± endpoint eriÅŸimi saÄŸlanÄ±r. |
| FR-002 | Sistem, rol bazlÄ± yetkilendirme uygulamalÄ±dÄ±r. | `ADMIN` endpointâ€™leri `USER` iÃ§in 403 dÃ¶ner. |
| FR-003 | Sistem, kullanÄ±cÄ±ya portfÃ¶y oluÅŸturma/gÃ¼ncelleme/silme sunmalÄ±dÄ±r. | CRUD iÅŸlemleri API Ã¼zerinden tamamlanÄ±r. |
| FR-004 | Sistem, MARKET emrini anÄ±nda iÅŸleyebilmelidir. | Uygun koÅŸulda emir `FILLED` olur. |
| FR-005 | Sistem, LIMIT emrini tetik ÅŸartÄ±na gÃ¶re iÅŸlemelidir. | Fiyat koÅŸulu oluÅŸmadan emir `PENDING` kalÄ±r. |
| FR-006 | Sistem, STOP emrini tetik ÅŸartÄ±na gÃ¶re iÅŸlemelidir. | Stop seviyesi aÅŸÄ±lÄ±nca emir iÅŸlenir. |
| FR-007 | Sistem, iÅŸlem sonrasÄ± nakit ve pozisyonlarÄ± gÃ¼ncellemelidir. | `cashBalance`, `PortfolioItem`, `Transaction` tutarlÄ± deÄŸiÅŸir. |
| FR-008 | Sistem, fiyat verisini periyodik toplamalÄ±dÄ±r. | Scheduler belirlenen aralÄ±kta veri Ã§eker. |
| FR-009 | Sistem, dÄ±ÅŸ API yokluÄŸunda simÃ¼lasyon modunu desteklemelidir. | SimÃ¼lasyon aktifken veri Ã¼retimi sÃ¼rer. |
| FR-010 | Sistem, simÃ¼lasyon verisini kullanÄ±cÄ±ya gÃ¶rÃ¼nÃ¼r iÅŸaretlemelidir. | UIâ€™da simÃ¼lasyon etiketi/iÅŸareti gÃ¶rÃ¼nÃ¼r. |
| FR-011 | Sistem, dÃ¶viz kurlarÄ±nÄ± TCMBâ€™den Ã§ekebilmelidir. | `/market/currencies` gÃ¼ncel kayÄ±t dÃ¶ner. |
| FR-012 | Sistem, hisse/fon/tahvil/VÄ°OP verisini provider zinciri ile almalÄ±dÄ±r. | Provider fallback ile veri Ã¼retimi sÃ¼rer. |
| FR-013 | Sistem, WebSocket ile anlÄ±k fiyat yayÄ±nÄ± yapmalÄ±dÄ±r. | `topic/prices/*` kanallarÄ± Ã¼zerinden yayÄ±n yapÄ±lÄ±r. |
| FR-014 | Sistem, haber akÄ±ÅŸÄ± saÄŸlamalÄ±dÄ±r. | `/news` endpointâ€™i sayfalÄ± veri dÃ¶ner. |
| FR-015 | Sistem, simÃ¼lasyon haberini gerÃ§ek haberden ayÄ±rt etmelidir. | SimÃ¼lasyon kaynak adÄ±/iÅŸaretleme bulunur. |
| FR-016 | Sistem, teknik analiz gÃ¶stergelerini hesaplamalÄ±dÄ±r. | RSI/MACD/Bollinger/Stochastic endpointâ€™leri Ã§alÄ±ÅŸÄ±r. |
| FR-017 | Sistem, enstrÃ¼man karÅŸÄ±laÅŸtÄ±rma analizi yapmalÄ±dÄ±r. | `/analysis/compare` sonuÃ§ listesi dÃ¶ner. |
| FR-018 | Sistem, alarm kurallarÄ± ve bildirim Ã¼retmelidir. | Fiyat alarmÄ± tetiklenince bildirim oluÅŸur. |
| FR-019 | Sistem, kullanÄ±cÄ± tercihlerine gÃ¶re veri kaynaÄŸÄ± yÃ¶netebilmelidir. | `/data-sources/preferences` ile gÃ¼ncelleme yapÄ±lÄ±r. |
| FR-020 | Sistem, log eventâ€™lerini merkezi saklamalÄ±dÄ±r. | Kafka -> Logstash -> OpenSearch akÄ±ÅŸÄ± iÅŸler. |
| FR-021 | Sistem, market-data eventâ€™lerini de tÃ¼ketebilmelidir. | `MarketDataEventConsumer` eventâ€™i iÅŸler ve indeksler. |
| FR-022 | Sistem, portfÃ¶yÃ¼ Excel/PDF olarak dÄ±ÅŸa aktarabilmelidir. | Export endpointâ€™leri dosya yanÄ±tÄ± dÃ¶ner. |
| FR-023 | Sistem, admin panelinden simÃ¼lasyon kontrolÃ¼ sunmalÄ±dÄ±r. | SimÃ¼lasyon start/stop/config endpointâ€™leri Ã§alÄ±ÅŸÄ±r. |
| FR-024 | Sistem, Ã§oklu dil ve tema desteÄŸi sunmalÄ±dÄ±r. | TR/EN ve tema anahtarlama iÅŸlevsel olmalÄ±dÄ±r. |

### 4.2 Non-Fonksiyonel Gereksinimler

| ID | Alan | Hedef |
|---|---|---|
| NFR-001 | GÃ¼venlik | OAuth2/OIDC + JWT + RBAC zorunlu |
| NFR-002 | GÃ¼venlik | 2FA (TOTP) aktiflenebilir yapÄ± |
| NFR-003 | Performans | Okuma endpointâ€™lerinde hedef P95 < 800ms |
| NFR-004 | Performans | Yazma endpointâ€™lerinde hedef P95 < 1500ms |
| NFR-005 | Ã–lÃ§eklenebilirlik | Ãœretimde backend replica desteÄŸi |
| NFR-006 | DayanÄ±klÄ±lÄ±k | DÄ±ÅŸ API Ã§aÄŸrÄ±larÄ±nda retry + circuit breaker |
| NFR-007 | GÃ¶zlemlenebilirlik | Prometheus metrikleri + Grafana dashboard |
| NFR-008 | GÃ¶zlemlenebilirlik | Loglar OpenSearch Ã¼zerinden sorgulanabilir |
| NFR-009 | Ä°zlenebilirlik | Tracing altyapÄ±sÄ± OTEL Ã¼zerinden Ã§alÄ±ÅŸmalÄ± |
| NFR-010 | BakÄ±m KolaylÄ±ÄŸÄ± | Flyway ile ÅŸema versiyonlamasÄ± |
| NFR-011 | Operasyon | Docker Compose ile tek komutta kurulum |
| NFR-012 | Kod Kalitesi | Backend ve frontend testleri CIâ€™da Ã§alÄ±ÅŸmalÄ± |

### 4.3 Gereksinim Ä°zlenebilirliÄŸi (Ã–rnek)

| Gereksinim | Endpoint/ModÃ¼l | Test AlanÄ± |
|---|---|---|
| FR-004/5/6 | `POST /portfolios/{id}/trades` + order engine | `PortfolioOrderExecutionServiceTest` |
| FR-016 | `/api/v1/indicators/*` | Technical indicator servis testleri |
| FR-009/10 | `SimulationDataService` + UI market kartlarÄ± | SimÃ¼lasyon entegrasyon testleri |
| FR-021 | `MarketDataEventConsumer` | Kafka tÃ¼ketici testleri |

---

## 5) Sistem TasarÄ±mÄ±

### 5.1 Mimari YaklaÅŸÄ±m

Sistem, **modÃ¼ler monolith + servis odaklÄ± katmanlama** yaklaÅŸÄ±mÄ±yla tasarlanmÄ±ÅŸtÄ±r:

- Tek backend uygulamasÄ± iÃ§inde domain bazlÄ± servisler.
- AyrÄ±k sorumluluk: piyasa verisi, portfÃ¶y, analiz, simÃ¼lasyon, kullanÄ±cÄ±.
- DÄ±ÅŸ sistemlerle gevÅŸek baÄŸlÄ± entegrasyon (WebClient + provider resolver).

### 5.2 Ãœst Seviye Mimari (Container)

```mermaid
flowchart LR
    U[KullanÄ±cÄ±/TarayÄ±cÄ±] --> N[Nginx :8088]
    N --> F[Frontend React :3002]
    N --> B[Backend Spring Boot :8080]

    B --> P[(PostgreSQL :5432)]
    B --> R[(Redis :6379)]
    B --> K[Kafka KRaft :9092]
    B --> KC[Keycloak :8180]
    KC --> LDAP[OpenLDAP :389]

    B --> EXT[DÄ±ÅŸ API'ler\nTCMB/Yahoo/AlphaVantage/Finnhub]
    K --> LS[Logstash]
    LS --> OS[OpenSearch]
    B --> OT[OTEL Collector]
    B --> PR[Prometheus]
    PR --> G[Grafana]
    OS --> OSD[OpenSearch Dashboards]
```

### 5.3 Backend ModÃ¼l TasarÄ±mÄ±

| ModÃ¼l | Sorumluluk |
|---|---|
| `controller` | REST endpoint tanÄ±mÄ±, request doÄŸrulama |
| `service` | Ä°ÅŸ kurallarÄ± ve orkestrasyon |
| `repository` | KalÄ±cÄ± veri eriÅŸimi |
| `scheduler` | Periyodik veri toplama/simÃ¼lasyon |
| `service/event` | Kafka publish/consume |
| `service/simulation` | SimÃ¼lasyon motoru ve haber senaryolarÄ± |
| `service/portfolio` | Emir tetikleme, komisyon, nakit/pozisyon yÃ¶netimi |

### 5.4 Veri TabanÄ± TasarÄ±mÄ± (Ã–zet)

Ana varlÄ±klar:

- `users`, `user_api_configs`, `user_data_preferences`
- `portfolios`, `portfolio_items`, `portfolio_transactions`
- `instruments`, `price_history`, `currency_rates`
- `watchlists`, `watchlist_items`, `price_alerts`
- `news`, `news_categories`, `user_notifications`
- `simulation_config`

Ã–ne Ã§Ä±kan tasarÄ±m kararÄ±:

- `instruments` tablosunda `(symbol, is_simulated)` unique kuralÄ± ile gerÃ§ek/simÃ¼le veri ayrÄ±mÄ± gÃ¼vence altÄ±na alÄ±nmÄ±ÅŸtÄ±r.

### 5.5 Entegrasyon TasarÄ±mÄ±

| Entegrasyon | Protokol | AmaÃ§ |
|---|---|---|
| Frontend -> Backend | HTTP/JSON | Ä°ÅŸlevsel API eriÅŸimi |
| Frontend <-> Backend | WebSocket/STOMP | AnlÄ±k fiyat gÃ¼ncellemesi |
| Backend -> Keycloak | OIDC/JWK | Token doÄŸrulama |
| Backend -> DÄ±ÅŸ API | HTTPS | Piyasa verisi toplama |
| Backend -> Kafka | Event | Log/bildirim/market data event |
| Kafka -> Logstash | Event tÃ¼ketimi | Log iÅŸleme |
| Logstash -> OpenSearch | HTTP | Log indeksleme |
| Backend -> OTEL | OTLP | Trace/metrik akÄ±ÅŸÄ± |

### 5.6 API SÃ¶zleÅŸmeleri

- Base URL: `http://localhost:8088/api/v1`
- Auth: `Authorization: Bearer <token>`
- Versiyonlama: URL bazlÄ± (`/api/v1`)
- Teknik analiz endpoint Ã¶rnekleri:
  - `GET /analysis/ma/{symbol}`
  - `GET /analysis/trend/{symbol}`
  - `POST /analysis/compare`
  - `GET /indicators/rsi/{symbol}`
  - `GET /indicators/macd/{symbol}`
  - `GET /indicators/bollinger/{symbol}`
  - `GET /indicators/stochastic/{symbol}`
  - `GET /indicators/all/{symbol}`

### 5.7 DaÄŸÄ±tÄ±m TasarÄ±mÄ±

Profiller:

- `docker-compose.yml`: tam geliÅŸtirme stackâ€™i.
- `docker-compose.light.yml`: dÃ¼ÅŸÃ¼k kaynaklÄ± geliÅŸtirme.
- `docker-compose.prod.yml`: production topolojisi (secret + network segmentasyonu + replica).

---

## 6) Veri AkÄ±ÅŸÄ± ve Ä°ÅŸ KurallarÄ±

### 6.1 GerÃ§ek Piyasa Verisi AkÄ±ÅŸÄ±

```mermaid
sequenceDiagram
    participant SCH as Scheduler
    participant MDS as MarketDataService
    participant EXT as DÄ±ÅŸ API
    participant DB as PostgreSQL
    participant C as Redis Cache
    participant WS as WebSocket
    participant UI as Frontend
    participant EP as EventPublisher
    participant KC as Kafka

    SCH->>MDS: periyodik tetikleme
    MDS->>EXT: veri Ã§ek
    EXT-->>MDS: ham veri
    MDS->>DB: normalize et + kaydet
    MDS->>C: cache gÃ¼ncelle
    MDS->>WS: anlÄ±k yayÄ±n
    WS-->>UI: topic/prices/*
    MDS->>EP: market data event yayÄ±nla
    EP->>KC: topic: mintstack-market-data
```

### 6.2 SimÃ¼lasyon Veri AkÄ±ÅŸÄ±

SimÃ¼lasyon aktif olduÄŸunda:

- DÄ±ÅŸ API Ã§aÄŸrÄ±sÄ± yerine simulation engine fiyat Ã¼retir.
- Hisse + tahvil + fon + VÄ°OP + dÃ¶viz + endeks + kripto iÃ§in cache gÃ¼ncellenir.
- Senaryo temelli haber Ã¼retimi yapÄ±lÄ±r.
- Ãœretilen veriler `isSimulated` veya kaynak adÄ± Ã¼zerinden iÅŸaretlenir.

### 6.3 Kritik Ä°ÅŸ KurallarÄ±

#### 6.3.1 Emir KurallarÄ±

- MARKET emirleri uygun fiyat varsa doÄŸrudan iÅŸlenir.
- LIMIT BUY: `marketPrice <= limitPrice` olursa tetiklenir.
- LIMIT SELL: `marketPrice >= limitPrice` olursa tetiklenir.
- STOP BUY: `marketPrice >= stopPrice` olursa tetiklenir.
- STOP SELL: `marketPrice <= stopPrice` olursa tetiklenir.
- Non-market emirlerde BIST seansÄ± kontrol edilir.
- Kripto enstrÃ¼manlarda seans kÄ±sÄ±tÄ± uygulanmaz.

#### 6.3.2 PortfÃ¶y ve Nakit KurallarÄ±

- Yetersiz nakitte BUY emri reddedilir veya uygun miktara dÃ¼ÅŸÃ¼rÃ¼lÃ¼r.
- SELL iÅŸleminde FIFO lot tÃ¼ketimi uygulanÄ±r.
- Komisyon + vergi enstrÃ¼man tipine gÃ¶re Ã§arpanlÄ± hesaplanÄ±r.
- Ä°ÅŸlem sonrasÄ± `cashBalance`, `filledQuantity`, `realizedPnL` gÃ¼ncellenir.

#### 6.3.3 Teknik Analiz KurallarÄ±

- RSI: varsayÄ±lan periyot 14.
- MACD: varsayÄ±lan 12/26/9.
- Bollinger: varsayÄ±lan 20 ve 2.0 std-dev.
- Stochastic: varsayÄ±lan `%K=14`, `%D=3`.
- TÃ¼m gÃ¶sterge endpointâ€™leri yetersiz veri durumunda aÃ§Ä±klayÄ±cÄ± mesaj dÃ¶ner.

#### 6.3.4 Veri KaynaÄŸÄ± KuralÄ± (Ã–nemli)

- TCMB yalnÄ±zca dÃ¶viz iÃ§in birincil kaynaktÄ±r.
- BIST 100 gibi endeksler TCMBâ€™den gelmez; Yahoo/AlphaVantage/Finnhub veya simÃ¼lasyon/fallback akÄ±ÅŸÄ±ndan gelir.
- Bu nedenle TCMB anahtarÄ± Ã§alÄ±ÅŸsa bile endeks tarafÄ±nda ayrÄ± provider gereklidir.

### 6.4 Validasyon ve Hata YÃ¶netimi

- DTO seviyesinde alan zorunluluk ve format kontrolleri.
- Servis katmanÄ±nda iÅŸ kuralÄ± ihlalleri `BadRequest`/domain exception ile yÃ¶netilir.
- TÃ¼m hatalar standardize JSON formatta dÃ¶ndÃ¼rÃ¼lÃ¼r.

---

## 7) GÃ¼venlik ve Yetkilendirme

### 7.1 Kimlik DoÄŸrulama

- Keycloak realm: `mintstack-finance`
- Frontend: PKCE tabanlÄ± login akÄ±ÅŸÄ±
- Backend: JWT imza doÄŸrulama (issuer + jwk-set)
- Opsiyonel 2FA (TOTP) desteÄŸi

### 7.2 Yetkilendirme

| Rol | Yetki KapsamÄ± |
|---|---|
| USER | PortfÃ¶y, piyasa, alarm, watchlist, analiz |
| ADMIN | USER + admin dashboard + simÃ¼lasyon yÃ¶netimi |

### 7.3 Uygulama GÃ¼venlik Ã–nlemleri

- Spring Security ile endpoint bazlÄ± eriÅŸim kontrolÃ¼.
- CORS whitelist yaklaÅŸÄ±mÄ± (ortam bazlÄ± daraltma Ã¶nerilir).
- Rate limiting (Bucket4j) ile suistimal Ã¶nleme.
- Secretâ€™lar `.env`/Docker secrets Ã¼zerinden taÅŸÄ±nÄ±r.
- Kafka SASL/PLAIN ve OpenSearch security aÃ§Ä±k yapÄ±.

### 7.4 Operasyonel GÃ¼venlik

- Ãœretimde iÃ§ servis aÄŸ segmentasyonu (`internal` network).
- Nginx tek giriÅŸ noktasÄ±.
- Prometheus/Grafana/OpenSearch eriÅŸimi kimlik doÄŸrulamalÄ±.

---

## 8) Test ve DoÄŸrulama

### 8.1 Mevcut Test VarlÄ±ÄŸÄ±

- Backend test sÄ±nÄ±fÄ±: **41**
- Frontend test dosyasÄ±: **25**
- Flyway migration: **19+**

### 8.2 Test Stratejisi

| Katman | AraÃ§ | AmaÃ§ |
|---|---|---|
| Unit (Backend) | JUnit5 + Mockito | Ä°ÅŸ kuralÄ± doÄŸrulama |
| Unit (Frontend) | Vitest + Testing Library | BileÅŸen/logic testi |
| Entegrasyon | Spring Test + Testcontainers | DB/Kafka/Redis entegrasyonu |
| E2E (Frontend) | Playwright | KullanÄ±cÄ± senaryolarÄ± |
| API doÄŸrulama | Swagger + manuel/otomatik Ã§aÄŸrÄ± | SÃ¶zleÅŸme doÄŸruluÄŸu |

### 8.3 Kritik Test SenaryolarÄ±

| ID | Senaryo | Beklenen |
|---|---|---|
| TS-001 | STOP BUY tetikleme | Fiyat stop Ã¼stÃ¼ne Ã§Ä±kÄ±nca emir iÅŸlenir |
| TS-002 | STOP SELL tetikleme | Fiyat stop altÄ±na inince emir iÅŸlenir |
| TS-003 | Seans dÄ±ÅŸÄ± non-crypto emir | Emir `PENDING` kalÄ±r |
| TS-004 | SimÃ¼lasyon aÃ§Ä±kken veri Ã¼retimi | Piyasa kartlarÄ± boÅŸ kalmaz |
| TS-005 | RSI/MACD/Bollinger/Stochastic endpoint | 200 + anlamlÄ± veri |
| TS-006 | SimÃ¼lasyon haber iÅŸareti | UIâ€™da simÃ¼lasyon etiketi gÃ¶rÃ¼nÃ¼r |
| TS-007 | Market data event consume | Event OpenSearchâ€™e yazÄ±lÄ±r |
| TS-008 | Role enforcement | USER, ADMIN endpointâ€™e eriÅŸemez |

### 8.4 Ã‡alÄ±ÅŸtÄ±rma KomutlarÄ±

```bash
# Backend
cd backend
./mvnw clean verify

# Frontend
cd frontend
npm run lint
npm run test -- --run --coverage
npm run build
```

### 8.5 Kabul Kriterleri

- Build kÄ±rÄ±lmadan tamamlanmalÄ±.
- Kritik iÅŸ akÄ±ÅŸlarÄ± (auth, market data, portfolio orders, analysis) en az birer test ile doÄŸrulanmalÄ±.
- CI akÄ±ÅŸÄ±nda test + build + paketleme adÄ±mlarÄ± yeÅŸil olmalÄ±.

---

## 9) Riskler ve KÄ±sÄ±tlamalar

### 9.1 Risk Tablosu

| Risk | OlasÄ±lÄ±k | Etki | AzaltÄ±m PlanÄ± |
|---|---|---|---|
| DÄ±ÅŸ API rate limit/kesinti | Orta | YÃ¼ksek | Retry + fallback + simÃ¼lasyon |
| BÃ¼yÃ¼k servis sÄ±nÄ±flarÄ± | Orta | Orta | Domain servislerine daha fazla bÃ¶lme |
| TypeScript strict gevÅŸekliÄŸi | Orta | Orta | AÅŸamalÄ± strict migration |
| Operasyonel kaynak tÃ¼ketimi | Orta | Orta | light profile + servis ayrÄ±ÅŸtÄ±rma |
| CORS yanlÄ±ÅŸ yapÄ±landÄ±rma | DÃ¼ÅŸÃ¼k | YÃ¼ksek | Ortam bazlÄ± whitelist |
| GÃ¶zlemlenebilirlik yanlÄ±ÅŸ alarm | Orta | DÃ¼ÅŸÃ¼k | Alert threshold tuning |
| Veri tutarlÄ±lÄ±ÄŸÄ± (cache/DB) | DÃ¼ÅŸÃ¼k | Orta | TTL + invalidation stratejisi |

### 9.2 KÄ±sÄ±tlamalar

- GeliÅŸtirme ortamÄ±nda tek makine kaynaklarÄ± sÄ±nÄ±rlayÄ±cÄ±dÄ±r.
- DÄ±ÅŸ API anahtarlarÄ±nÄ±n geÃ§erliliÄŸi/veri kotasÄ± sistem davranÄ±ÅŸÄ±nÄ± etkiler.
- Finansal veri Ã§eÅŸitliliÄŸi saÄŸlayÄ±cÄ± kapsamÄ± ile sÄ±nÄ±rlÄ±dÄ±r.

### 9.3 Teknik BorÃ§ ve Ã–nceliklendirme

1. BÃ¼yÃ¼k servisleri daha kÃ¼Ã§Ã¼k domain servislerine bÃ¶lme.
2. Frontendâ€™de strict TypeScript seviyesini yÃ¼kseltme.
3. Performans testlerini CI pipelineâ€™a kalÄ±cÄ± ekleme.
4. Ãœretim ortamÄ±nda gÃ¼venlik sertleÅŸtirmesini artÄ±rma.

---

## 10) Referanslar

- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/api-docs.md`
- `docs/OPERATIONS.md`
- `backend/src/main/resources/application.yml`
- `docker-compose.yml`
- `docker-compose.prod.yml`
- `backend/src/main/resources/db/migration/*`

---

## Ek A) Sunumda AnlatÄ±m AkÄ±ÅŸÄ± (KÄ±sa Rehber)

Sunum sÄ±rasÄ±nda aÅŸaÄŸÄ±daki sÄ±rayÄ± takip etmek Ã¶nerilir:

1. Problemi tanÄ±mla: â€œTek platformda piyasa verisi + portfÃ¶y + analiz + simÃ¼lasyon.â€
2. Mimarideki 3 Ã§ekirdek akÄ±ÅŸÄ± gÃ¶ster:
   - veri toplama,
   - emir iÅŸleme,
   - analiz ve gÃ¶rselleÅŸtirme.
3. GÃ¼venlik katmanÄ±nÄ± aÃ§Ä±kla: Keycloak, JWT, rol modeli, 2FA.
4. SimÃ¼lasyon/gerÃ§ek veri ayrÄ±mÄ±nÄ± anlat: kullanÄ±cÄ± yanÄ±ltÄ±lmÄ±yor.
5. Test ve kalite yaklaÅŸÄ±mÄ±nÄ± gÃ¶ster: kritik senaryolar + CI.
6. Riskleri dÃ¼rÃ¼stÃ§e sÃ¶yle ve planlÄ± iyileÅŸtirmeleri belirt.

Bu akÄ±ÅŸ, hem teknik hem teknik olmayan paydaÅŸlar iÃ§in anlaÅŸÄ±lÄ±r bir hikaye oluÅŸturur.

---

## Ek B) Operasyonel Mimari Detaylari (Yonetim Sunumu Icin)

### B.1 OpenAPI / Swagger Erisim Kontrol Listesi

Gelistirme ortami adresleri:

- Swagger UI: `http://localhost:8088/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8088/api-docs`
- Geriye donuk uyumluluk: `http://localhost:8088/v3/api-docs`

Kapatilan problem:

- `springdoc.api-docs.path` backendde `/api-docs` idi.
- Nginx dev config yalnizca `/v3/api-docs/` proxyluyordu.
- Cozum: Nginx'e `/api-docs` route eklendi, security tarafina da `/v3/api-docs/**` uyumlulugu eklendi.

### B.2 Scheduler Frekans Plani (Enstruman Bazli)

| Veri Turu | Cron | Gerekce |
|---|---|---|
| TCMB doviz | `0 30 10,16 * * MON-FRI` | Resmi yayin ritmi |
| BIST hisse | `*/20 * * * * *` | Intraday degisim hizli |
| Tahvil/bono | `0 */2 * * * *` | Daha dusuk frekans yeterli |
| Yatirim fonu | `0 */3 * * * *` | Fon fiyatlari daha yavas |
| VIOP | `*/30 * * * * *` | Kontrat bazli daha hizli izleme |
| Doviz (TCMB disi) | `0 */5 * * * MON-FRI` | Rate limit dengesi |
| Kripto | `0 * * * * *` | 7/24 piyasa |
| Haber | `0 */15 * * * *` | Yeterli tazelik |

Ek teknik kararlar:

- Batch boyutu configurable: `APP_SCHEDULER_INSTRUMENT_BATCH_SIZE`
- Round-robin offset artik tip bazli tutuluyor; tipler arasi kayma kapanmis durumda.

### B.3 Teknik Gostergeler Tasarim Plani

Hesaplanan indikatorler:

- RSI
- MACD
- Bollinger Bands
- Stochastic
- SMA / EMA

Endpoint grubu:

- `/api/v1/indicators/rsi/{symbol}`
- `/api/v1/indicators/macd/{symbol}`
- `/api/v1/indicators/bollinger/{symbol}`
- `/api/v1/indicators/stochastic/{symbol}`
- `/api/v1/indicators/all/{symbol}`

Tasarim ilkeleri:

- Yetersiz veri durumunda kontrollu hata/mesaj donusu.
- Parametrelenebilir periyotlar.
- UI'da sekmeli ve grafik destekli gosterim.

### B.4 Yetki Matrisi (Admin / User / Public)

| Kaynak | Public | USER | ADMIN |
|---|---:|---:|---:|
| `GET /api/v1/market/**` | Evet | Evet | Evet |
| `GET /api/v1/news/**` | Evet | Evet | Evet |
| Portfoy / watchlist / alerts / users | Hayir | Evet | Evet |
| `/api/v1/data-sources/**` | Hayir | Evet | Evet |
| `/api/v1/simulation/**` | Hayir | Hayir | Evet |
| `/api/v1/admin/**` | Hayir | Hayir | Evet |

### B.5 ER Diyagrami

```mermaid
erDiagram
    USERS ||--o{ PORTFOLIOS : owns
    USERS ||--o{ WATCHLISTS : owns
    USERS ||--o{ PRICE_ALERTS : creates
    USERS ||--o{ USER_API_CONFIGS : configures
    USERS ||--o{ USER_DATA_PREFERENCES : sets
    USERS ||--o{ USER_NOTIFICATIONS : receives

    PORTFOLIOS ||--o{ PORTFOLIO_ITEMS : contains
    PORTFOLIOS ||--o{ PORTFOLIO_TRANSACTIONS : records

    INSTRUMENTS ||--o{ PRICE_HISTORY : has
    INSTRUMENTS ||--o{ PORTFOLIO_ITEMS : referenced
    INSTRUMENTS ||--o{ PORTFOLIO_TRANSACTIONS : referenced
    INSTRUMENTS ||--o{ WATCHLIST_ITEMS : tracked
    INSTRUMENTS ||--o{ PRICE_ALERTS : monitored

    WATCHLISTS ||--o{ WATCHLIST_ITEMS : includes
    NEWS_CATEGORIES ||--o{ NEWS : categorizes
```

DB tasarim notlari:

- UUID tabanli kimliklendirme
- Optimistic locking (`version`) kullanimi
- Flyway ile sema versiyonlama
- Islem/tarihce tablolari analitik odakli saklaniyor

### B.6 Yonetim Modulleri Bilesen Diyagrami

```mermaid
flowchart TB
    AD[Admin UI] --> ADM[AdminController]
    AD --> SIM[SimulationController]
    AD --> OBS[ObservabilityController]
    AD --> RL[RateLimitAdminController]

    ADM --> AS[AdminService]
    SIM --> SDS[SimulationDataService]
    OBS --> OSS[OpenSearchService]
    RL --> RLC[RateLimitConfig]

    AS --> UDB[(users)]
    SDS --> SCFG[(simulation_config)]
    OSS --> OSD[(OpenSearch)]
    RLC --> BUCKET[(Bucket4j Buckets)]
```

### B.7 Rate Limiting Entegrasyonu

Mimari:

- `RateLimitFilter` global giris filtresi
- Bucket4j token-bucket
- Anonymous(IP) / User / Admin ayrik limit

Runtime yonetim:

- `GET /api/v1/admin/rate-limit`
- `PUT /api/v1/admin/rate-limit`

Ornek update payload:

```json
{
  "enabled": true,
  "anonymousRequestsPerMinute": 120,
  "authenticatedRequestsPerMinute": 300,
  "adminRequestsPerMinute": 900,
  "clearBuckets": true
}
```

### B.8 Alarm Akisi ve Is Kurallari

```mermaid
sequenceDiagram
    participant S as Scheduler/PriceUpdate
    participant A as AlertService
    participant R as PriceAlertRepository
    participant K as Kafka
    participant E as EmailService
    participant UI as Frontend

    S->>A: checkAlertsForSymbol(symbol, currentPrice)
    A->>R: aktif alarmlari getir
    A->>A: shouldTrigger kurallari
    A->>R: trigger + persist
    A->>K: notification event publish
    A->>E: e-posta gonder
    K-->>UI: kullanici bildirimi
```

Kurallar:

- `PRICE_ABOVE`, `PRICE_BELOW`, `PERCENT_UP`, `PERCENT_DOWN`
- Tetiklenen alarm tekrar tetiklenmez (`isActive=false`, `isTriggered=true`)
- Kullanici basina aktif alarm limiti uygulanir

### B.9 Hata Analizi (RSS dahil)

RSS/harici servis hatalarinda:

- Job exception yakalar
- Tum scheduler durmaz
- Bir sonraki cron cevriminde tekrar denenir

Standart hata cevaplari:

- `BAD_REQUEST`
- `RESOURCE_NOT_FOUND`
- `BUSINESS_RULE_VIOLATION`
- `RATE_LIMIT_EXCEEDED`
- `INTERNAL_SERVER_ERROR`

### B.10 TraceID / OpenTelemetry Kontrol Standardi

Standart:

- Header: `X-Trace-Id`
- Format: 32 karakter lowercase hex
- Log baglama: `%X{traceId}`

Uygulama:

- `TraceIdResponseFilter` ile her requestte trace id normalize edilir.
- 16 hex gelen trace id 32 hex formatina pad edilir.
- Gecersiz formatta yeni trace id uretilir.

### B.11 Minimum Sistem Ihtiyaci

Full stack gelistirme:

- CPU: 4 vCPU minimum, 6 vCPU onerilen
- RAM: 8 GB minimum, 12 GB onerilen
- Disk: 20 GB bos alan

Hafif profil:

- Observability stack kapatilinca daha dusuk RAM ile calisabilir.

