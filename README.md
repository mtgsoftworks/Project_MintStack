# MintStack Finance Portal

> **Türkiye Odaklı Finans Platformu** — Gerçek zamanlı piyasa izleme, portföy yönetimi, teknik analiz, simülasyon ve bildirim altyapısı.

![Java 17](https://img.shields.io/badge/Java-17-orange) ![Spring Boot 3.4](https://img.shields.io/badge/Spring_Boot-3.4.2-green) ![React 18](https://img.shields.io/badge/React-18-blue) ![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue) ![PostgreSQL 15](https://img.shields.io/badge/PostgreSQL-15-blue) ![Docker](https://img.shields.io/badge/Docker-Compose-blue) ![Keycloak 26](https://img.shields.io/badge/Keycloak-26-red)

---

## 1. Proje Tanımı

MintStack Finance Portal, Türkiye finans piyasalarına (BIST, döviz, tahvil, fon, VİOP) yönelik entegre bir finans veri ve portföy yönetim platformudur. Sistem; çoklu veri kaynağı entegrasyonu (TCMB, Yahoo Finance, Alpha Vantage, Finnhub), gerçek zamanlı fiyat akışı (WebSocket/STOMP), gelişmiş portföy simülasyonu (al/sat emirleri, limit/stop emir tipleri, emir yaşam döngüsü), teknik analiz araçları (Monte Carlo, backtesting, teknik indikatörler), haber agregasyonu ve kurumsal düzeyde güvenlik (OAuth2/OIDC, RBAC, 2FA) sunmaktadır.

### 1.1 Hedef Kullanıcı ve Senaryolar

| Kullanıcı Tipi | Temel Senaryolar |
|---|---|
| **Bireysel Yatırımcı** | Piyasa takibi, sanal portföy yönetimi, fiyat alarmları, watchlist |
| **Finans Öğrencisi** | Simülasyon ile deneyim kazanma, teknik analiz araçları |
| **Sistem Yöneticisi** | Admin paneli, kullanıcı yönetimi, simülasyon kontrolü, loglama |

### 1.2 MVP Kapsamı

- ✅ Çoklu veri kaynağından (TCMB, Yahoo, Alpha Vantage, Finnhub) piyasa verisi toplama
- ✅ Gerçek zamanlı fiyat güncelleme (WebSocket/STOMP)
- ✅ Sanal portföy oluşturma ve yönetimi (MARKET, LIMIT, STOP emir tipleri)
- ✅ Emir yaşam döngüsü (PENDING → PARTIALLY_FILLED → FILLED / CANCELED / REJECTED)
- ✅ Teknik analiz: Monte Carlo simülasyonu, backtesting, korelasyon matrisi, RSI/MA stratejileri
- ✅ Haber akışı (RSS entegrasyonu + kategori yönetimi)
- ✅ Fiyat alarmları ve bildirim sistemi (uygulama içi + e-posta)
- ✅ Keycloak ile OAuth2/OIDC kimlik doğrulama ve RBAC
- ✅ 2FA (TOTP) desteği
- ✅ Admin paneli (kullanıcı, simülasyon, sistem yönetimi)
- ✅ Tam observability stack (Prometheus, Grafana, OpenSearch, OTEL, Logstash)
- ✅ Excel/PDF raporlama ve dışa aktarım
- ✅ Çok dilli arayüz (TR/EN, i18next)
- ✅ Dark/Light tema desteği

---

## 2. Teknoloji Yığını

### 2.1 Backend

| Teknoloji | Versiyon | Kullanım Amacı |
|---|---|---|
| **Java** | 17 | Ana programlama dili |
| **Spring Boot** | 3.4.2 | Uygulama çatısı, REST API, güvenlik, scheduler |
| **Spring Security + OAuth2** | — | JWT tabanlı kimlik doğrulama ve yetkilendirme |
| **Spring Data JPA + Hibernate** | — | ORM, veri erişim katmanı |
| **Spring WebSocket (STOMP)** | — | Gerçek zamanlı veri akışı |
| **Spring Kafka** | — | Asenkron olay akışı ve log boru hattı |
| **Spring Cache + Redis** | — | Sıcak veri cache, performans optimizasyonu |
| **Spring WebFlux (WebClient)** | — | Dış API istemcisi (reaktif HTTP) |
| **Flyway** | — | Veritabanı şema migrasyon yönetimi (18 migrasyon) |
| **MapStruct** | 1.5.5 | DTO-Entity dönüşümleri |
| **Lombok** | 1.18.30 | Boilerplate kod azaltma |
| **Bucket4j** | 8.7.0 | Rate limiting |
| **Resilience4j** | 2.2.0 | Circuit breaker, hata toleransı |
| **SpringDoc OpenAPI** | 2.3.0 | Swagger UI, API dokümantasyonu |
| **Apache POI** | 5.2.5 | Excel dışa aktarım |
| **iText** | 8.0.2 | PDF dışa aktarım |
| **Quartz** | — | Zamanlanmış görevler (veri toplama, temizlik) |
| **Log4j2 + JSON Layout** | — | Yapısal loglama |

### 2.2 Frontend

| Teknoloji | Versiyon | Kullanım Amacı |
|---|---|---|
| **React** | 18.2 | UI kütüphanesi |
| **TypeScript** | 5.x | Tip güvenli geliştirme |
| **Vite** | 5.x | Build aracı ve dev server |
| **Redux Toolkit + RTK Query** | 2.x | State yönetimi ve API veri çekme |
| **React Router** | 6.x | Sayfa yönlendirme |
| **Tailwind CSS** | 3.4 | Utility-first CSS framework |
| **Radix UI** | — | Erişilebilir headless UI bileşenleri |
| **Recharts** | 2.10 | Grafik ve veri görselleştirme |
| **Formik + Yup** | — | Form yönetimi ve doğrulama |
| **Keycloak JS** | 26.x | Frontend tarafında kimlik doğrulama |
| **STOMP.js + SockJS** | — | WebSocket istemcisi |
| **i18next** | 23.x | Çoklu dil desteği (TR/EN) |
| **Lucide React** | — | İkon kütüphanesi |
| **Sonner** | — | Toast bildirimleri |

### 2.3 Altyapı ve DevOps

| Teknoloji | Versiyon | Kullanım Amacı |
|---|---|---|
| **Docker Compose** | v2+ | Konteyner orkestrasyon |
| **PostgreSQL** | 15 Alpine | İlişkisel veritabanı |
| **Redis** | 7 Alpine | Cache ve oturum yönetimi |
| **Keycloak** | 26.5 | OAuth2/OIDC kimlik sunucusu |
| **OpenLDAP** | 1.5.0 | Dizin hizmetleri, Keycloak federation |
| **Kafka (KRaft)** | 7.5.0 | Olay akışı, SASL/PLAIN doğrulama |
| **Nginx** | Alpine | API Gateway, reverse proxy |
| **Prometheus** | 2.48 | Metrik toplama |
| **Grafana** | 10.2 | Metrik görselleştirme ve alarm |
| **AlertManager** | 0.26 | Alarm yönetimi |
| **OpenSearch** | 2.13 | Log indeksleme ve arama |
| **OpenSearch Dashboards** | 2.13 | Log görselleştirme |
| **Logstash** | 8.9 | Log işleme pipeline |
| **OTEL Collector** | 0.91 | Distributed tracing |

### 2.4 Test Altyapısı

| Kategori | Araçlar |
|---|---|
| **Backend Unit Test** | JUnit 5, Mockito, Spring Boot Test (40 test sınıfı) |
| **Backend Entegrasyon** | Testcontainers (PostgreSQL), H2 |
| **Frontend Unit Test** | Vitest, Testing Library, MSW |
| **Frontend E2E** | Playwright |
| **Kod Kapsamı** | JaCoCo (minimum %50 satır, %40 branch) |

---

## 3. Sistem Mimarisi

### 3.1 Üst Seviye Mimari (C4 Container View)

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           KULLANICI (Tarayıcı)                          │
└─────────────────────────────────┬────────────────────────────────────────┘
                                  │ HTTPS
                                  ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    NGINX API GATEWAY (:8088)                            │
│         Reverse proxy, /api/v1 → Backend, /ws → WebSocket              │
└──────────┬─────────────────────────────────────┬─────────────────────────┘
           │                                     │
           ▼                                     ▼
┌─────────────────────┐               ┌─────────────────────┐
│  FRONTEND (:3002)   │               │  BACKEND (:8080)    │
│  React 18 + Vite    │               │  Spring Boot 3.4    │
│  TypeScript + Redux │               │  Java 17            │
│  TailwindCSS        │               │  REST + WebSocket   │
└─────────────────────┘               └──┬──┬──┬──┬──┬──────┘
                                         │  │  │  │  │
                    ┌────────────────────┘  │  │  │  └──────────────┐
                    │              ┌───────┘  │  └───────────┐     │
                    ▼              ▼          ▼              ▼     ▼
          ┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
          │ PostgreSQL   │ │  Redis   │ │  Kafka   │ │ Keycloak │ │ Dış API'ler  │
          │ (:5432)      │ │ (:6379)  │ │ (:9092)  │ │ (:8180)  │ │ TCMB, Yahoo  │
          │ 3 DB:        │ │ Cache +  │ │ KRaft +  │ │ OAuth2/  │ │ AlphaVantage │
          │ finance,     │ │ Hot Data │ │ SASL     │ │ OIDC     │ │ Finnhub      │
          │ keycloak,    │ │          │ │          │ │ + LDAP   │ │              │
          │ mintstack    │ │          │ │          │ │ + 2FA    │ │              │
          └──────────────┘ └──────────┘ └────┬─────┘ └──────────┘ └──────────────┘
                                             │
                                             ▼
                                   ┌──────────────────┐
                                   │    Logstash       │
                                   │ Kafka → OpenSearch │
                                   └────────┬─────────┘
                                            │
              ┌─────────────┬───────────────┼──────────────────┐
              ▼             ▼               ▼                  ▼
      ┌──────────────┐ ┌─────────────┐ ┌───────────────┐ ┌────────────┐
      │ Prometheus   │ │  Grafana    │ │  OpenSearch   │ │   OTEL     │
      │ Metrikler    │ │  Paneller   │ │  Log Arama    │ │  Tracing   │
      │ (:9090)      │ │  (:13030)   │ │  (:19200)     │ │  (:24317)  │
      └──────────────┘ └─────────────┘ └───────────────┘ └────────────┘
```

### 3.2 Katmanlı Backend Mimarisi

```
  ┌────────────────────────────────────────────────────────────────┐
  │ Controller Katmanı (15 controller)                            │
  │ REST endpoint'leri, request/response dönüşümü, validasyon     │
  ├────────────────────────────────────────────────────────────────┤
  │ Service Katmanı (52+ servis)                                  │
  │ İş kuralları, orkestrasyon, cache yönetimi, event yayınlama   │
  ├────────────────────────────────────────────────────────────────┤
  │ Repository Katmanı (15 repository)                            │
  │ JPA/Hibernate ile veri erişimi, özel sorgular                 │
  ├────────────────────────────────────────────────────────────────┤
  │ Entity Katmanı (17 entity)                                    │
  │ JPA domain modelleri, BaseEntity ile audit trail              │
  ├────────────────────────────────────────────────────────────────┤
  │ Altyapı: Scheduler(7) | Config(17) | Filter | Mapper | DTO   │
  └────────────────────────────────────────────────────────────────┘
```

---

## 4. Proje Dizin Yapısı

```
Project_MintStack/
│
├── backend/                          # Spring Boot API (Java 17)
│   ├── src/main/java/com/mintstack/finance/
│   │   ├── annotation/               # Özel anotasyonlar
│   │   ├── aspect/                   # AOP aspect'leri
│   │   ├── config/                   # Uygulama konfigürasyonları (17 dosya)
│   │   │   ├── SecurityConfig.java       # OAuth2/JWT güvenlik yapılandırması
│   │   │   ├── WebSocketConfig.java      # STOMP WebSocket yapılandırması
│   │   │   ├── KafkaConfig.java          # Kafka producer/consumer
│   │   │   ├── RedisCacheConfig.java     # Cache TTL ve politikaları
│   │   │   ├── RateLimitConfig.java      # Bucket4j rate limiting
│   │   │   └── ...                       # OpenSearch, CORS, Email, Async, JPA vb.
│   │   ├── controller/               # REST controller'lar (15 dosya)
│   │   │   ├── MarketDataController      # Piyasa verileri API
│   │   │   ├── PortfolioController       # Portföy CRUD + emir işlemleri
│   │   │   ├── NewsController            # Haber akışı
│   │   │   ├── AdminController           # Admin yönetim işlemleri
│   │   │   ├── AnalysisController        # Teknik analiz endpoint'leri
│   │   │   ├── BacktestingController     # Backtesting API
│   │   │   ├── MonteCarloController      # Monte Carlo simülasyonu
│   │   │   └── ...                       # Alert, Watchlist, User, Settings vb.
│   │   ├── dto/                       # Veri transfer nesneleri
│   │   │   ├── request/                  # İstek DTO'ları
│   │   │   ├── response/                 # Yanıt DTO'ları
│   │   │   ├── cache/                    # Cache DTO'ları
│   │   │   └── simulation/              # Simülasyon DTO'ları
│   │   ├── entity/                    # JPA entity'leri (17 dosya)
│   │   │   ├── User, Portfolio, PortfolioItem, PortfolioTransaction
│   │   │   ├── Instrument, PriceHistory, CurrencyRate
│   │   │   ├── Watchlist, WatchlistItem, PriceAlert
│   │   │   ├── News, NewsCategory, UserNotification
│   │   │   ├── UserApiConfig, UserDataPreference, SimulationConfig
│   │   │   └── BaseEntity (audit: createdAt, updatedAt)
│   │   ├── exception/                # Özel exception sınıfları
│   │   ├── filter/                   # HTTP filtreleri
│   │   ├── mapper/                   # MapStruct mapper'lar
│   │   ├── repository/               # Spring Data JPA repository'leri (15 dosya)
│   │   ├── scheduler/                # Zamanlanmış görevler (7 dosya)
│   │   │   ├── MarketDataScheduler       # Periyodik piyasa verisi toplama
│   │   │   ├── NewsScheduler             # Haber akışı güncelleme
│   │   │   ├── SimulationScheduler       # Simülasyon fiyat güncelleme
│   │   │   ├── CleanupScheduler          # Eski veri temizliği
│   │   │   └── MarketDataProviderResolver# Veri kaynağı çözümleme
│   │   └── service/                   # İş mantığı servisleri (52+ dosya)
│   │       ├── MarketDataService         # Piyasa veri orkestrasyon
│   │       ├── PortfolioService          # Portföy iş kuralları
│   │       ├── portfolio/                # Portföy alt servisleri
│   │       │   ├── PortfolioFinancialRulesService  # Finansal kurallar
│   │       │   └── PortfolioOrderExecutionService  # Emir çalıştırma
│   │       ├── external/                 # Dış API istemcileri
│   │       │   ├── TcmbApiClient         # TCMB döviz kuru
│   │       │   ├── YahooFinanceClient    # Yahoo Finance
│   │       │   ├── AlphaVantageClient    # Alpha Vantage
│   │       │   ├── FinnhubClient         # Finnhub
│   │       │   └── RssNewsClient         # RSS haber
│   │       ├── simulation/               # Simülasyon motoru (12 dosya)
│   │       ├── market/                   # Market veri bakımı
│   │       ├── search/                   # OpenSearch entegrasyonu
│   │       ├── event/                    # Kafka olay yayınlama
│   │       ├── strategy/                 # Trading stratejileri (RSI, MA)
│   │       └── ...                       # Analysis, News, Alert, Email vb.
│   ├── src/main/resources/
│   │   └── db/migration/             # Flyway migrasyonları (V1-V18)
│   └── src/test/                      # Test dosyaları (40 test sınıfı)
│
├── frontend/                          # React SPA (TypeScript)
│   ├── src/
│   │   ├── App.tsx                    # Ana uygulama, Keycloak init, routing
│   │   ├── pages/                     # Sayfa bileşenleri (20 sayfa)
│   │   │   ├── DashboardPage          # Ana gösterge paneli
│   │   │   ├── CurrencyPage           # Döviz kurları
│   │   │   ├── StocksPage / StockDetailPage  # Hisse senedi
│   │   │   ├── BondsPage              # Tahvil/bono
│   │   │   ├── FundsPage              # Yatırım fonları
│   │   │   ├── ViopPage               # VİOP
│   │   │   ├── PortfolioPage / PortfolioDetailPage  # Portföy yönetimi
│   │   │   ├── AnalysisPage           # Teknik analiz araçları
│   │   │   ├── NewsPage / NewsDetailPage  # Haberler
│   │   │   ├── WatchlistPage          # İzleme listesi
│   │   │   ├── AlertsPage             # Fiyat alarmları
│   │   │   ├── NotificationsPage      # Bildirimler
│   │   │   ├── SettingsPage           # Kullanıcı ayarları
│   │   │   ├── ProfilePage            # Profil yönetimi
│   │   │   ├── AdminDashboard         # Admin paneli
│   │   │   └── LoginPage / UnauthorizedPage
│   │   ├── components/                # Yeniden kullanılabilir bileşenler (49)
│   │   │   ├── ui/                    # ShadCN/Radix UI temel bileşenler
│   │   │   ├── common/                # Ortak bileşenler (MarketStatus, PriceCell vb.)
│   │   │   ├── layout/                # Layout, Header, Sidebar, ProtectedRoute
│   │   │   ├── charts/                # PriceChart, PieChart (Recharts)
│   │   │   └── admin/                 # Admin bileşenleri
│   │   ├── store/                     # Redux Toolkit state yönetimi
│   │   │   ├── api/                   # RTK Query API slice'ları (12 dosya)
│   │   │   └── slices/                # Auth, UI slice'ları
│   │   ├── services/                  # API servisleri (14 dosya)
│   │   ├── hooks/                     # Özel React hook'ları (5 dosya)
│   │   ├── utils/                     # Yardımcı fonksiyonlar
│   │   ├── locales/                   # TR/EN çeviri dosyaları
│   │   └── mocks/                     # MSW mock handler'lar
│   └── e2e/                           # Playwright E2E testleri (9 dosya)
│
├── docker/                            # Altyapı konfigürasyonları
│   ├── prometheus/                    # Prometheus config + alert kuralları
│   ├── grafana/                       # Dashboard ve datasource provisioning
│   ├── logstash/                      # Kafka → OpenSearch pipeline
│   ├── otel/                          # OpenTelemetry Collector config
│   ├── kafka/                         # Kafka konfigürasyonu
│   ├── redis/                         # Redis konfigürasyonu
│   ├── postgres/                      # Çoklu DB init script
│   ├── alertmanager/                  # AlertManager config
│   └── backup/                        # PG backup/restore scriptleri (Bash + PS)
│
├── keycloak/                          # Realm export (kullanıcı, rol, client tanımları)
├── nginx/                             # Dev/prod nginx konfigürasyonları
├── openldap/                          # LDAP seed verileri
├── secrets/                           # Secret şablonları
│
├── docker-compose.yml                 # Varsayılan Dev ortamı (15 servis)
├── docker-compose.light.yml           # Hafif Dev (minimum altyapı)
├── docker-compose.secure-dev.yml      # Güvenli Dev (TLS, HTTPS)
├── docker-compose.prod.yml            # Üretim ortamı
│
└── docs/                              # Proje dokümantasyonu
    ├── ARCHITECTURE.md                # Sistem mimarisi
    ├── TASARIM_MIMARISI_VE_MODELLEME.md  # Tasarım ve modelleme
    ├── ADR.md                         # Mimari karar kayıtları
    ├── api-docs.md                    # API referansı
    ├── SECURITY.md                    # Güvenlik rehberi
    ├── DEPLOYMENT.md                  # Dağıtım rehberi
    ├── KEYCLOAK_2FA_SETUP.md          # 2FA kurulum rehberi
    ├── TOPLANTI_2_SUNUM_AKISI.md      # Toplantı sunum planı
    └── TOPLANTI_2_KONUSMA_NOTLARI.md  # Konuşma notları
```

---

## 5. Hızlı Başlangıç

### 5.1 Gereksinimler

- Docker 24+
- Docker Compose v2+
- Git
- Önerilen kaynak: en az 4 CPU, 8 GB RAM

### 5.2 Kurulum ve Çalıştırma

```bash
# 1. Repoyu klonla
git clone https://github.com/YOUR_USERNAME/MintStack-Finance.git
cd MintStack-Finance

# 2. Ortam değişkenlerini yapılandır
cp .env.example .env
# .env dosyasındaki tüm <GENERATE_...> değerlerini gerçek değerlerle değiştir

# 3. Tüm servisleri başlat (15 konteyner)
docker compose up -d

# 4. Servislerin hazır olduğunu doğrula
docker compose ps
```

> ⚠️ **Güvenlik:** `.env` dosyasını asla repoya commit etmeyin. Tüm şifreleri güçlü ve benzersiz değerlerle oluşturun.

### 5.3 Erişim Noktaları

| Servis | URL | Açıklama |
|---|---|---|
| **Frontend** | http://localhost:3002 | React SPA arayüzü |
| **API Gateway** | http://localhost:8088 | Nginx reverse proxy |
| **REST API** | http://localhost:8088/api/v1 | Backend API |
| **Swagger UI** | http://localhost:8088/swagger-ui.html | API dokümantasyonu |
| **WebSocket** | ws://localhost:8088/ws | Gerçek zamanlı veri |
| **Keycloak** | http://localhost:8180 | Kimlik sunucusu |
| **Grafana** | http://localhost:13030 | Metrik panelleri |
| **Prometheus** | http://localhost:9090 | Metrik sorguları |
| **OpenSearch Dashboards** | http://localhost:15601 | Log arama |

### 5.4 Giriş Bilgileri (Dev)

| Hedef | Kullanıcı | Şifre |
|---|---|---|
| Uygulama (admin) | `admin` | `.env → KEYCLOAK_ADMIN_USER_PASSWORD` |
| Uygulama (test) | `test` | `.env → KEYCLOAK_TEST_USER_PASSWORD` |
| Keycloak Konsolu | `.env → KEYCLOAK_ADMIN` | `.env → KEYCLOAK_ADMIN_PASSWORD` |
| Grafana | `admin` | `.env → GRAFANA_ADMIN_PASSWORD` |

---

## 6. Çalışma Profilleri

### Varsayılan Dev (KRaft + SASL — tam stack)

```bash
docker compose up -d
```

Tüm 15 servisi ayağa kaldırır: PostgreSQL, Redis, Keycloak, OpenLDAP, Kafka, OpenSearch, Logstash, Prometheus, Grafana, AlertManager, OTEL Collector, Backend, Frontend, Nginx, OpenSearch Dashboards.

### Lightweight Dev (minimum altyapı)

```bash
docker compose -f docker-compose.light.yml up -d
```

Yalnızca PostgreSQL, Redis, Keycloak, Backend, Frontend ve Nginx. Observability servisleri hariç.

### Secure Dev (TLS aktif)

```bash
docker compose -f docker-compose.yml -f docker-compose.secure-dev.yml up -d
```

### Production

```bash
docker compose -f docker-compose.prod.yml up -d
```

---

## 7. Yerel Geliştirme

### Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Test Çalıştırma

```bash
# Backend unit testleri
cd backend && ./mvnw test

# Backend entegrasyon testleri
cd backend && ./mvnw verify -Pintegration-test

# Frontend unit testleri
cd frontend && npm test

# Frontend E2E testleri
cd frontend && npm run test:e2e
```

---

## 8. Dokümantasyon

| Doküman | Açıklama |
|---|---|
| [Sistem Mimarisi](docs/ARCHITECTURE.md) | C4 container view, servis sorumlulukları, veri akışları |
| [Tasarım ve Modelleme](docs/TASARIM_MIMARISI_VE_MODELLEME.md) | ER model, durum diyagramları, iş akışları |
| [Mimari Karar Kayıtları](docs/ADR.md) | Teknoloji seçim gerekçeleri (8 ADR) |
| [API Referansı](docs/api-docs.md) | Endpoint listesi ve kullanım örnekleri |
| [Güvenlik Rehberi](docs/SECURITY.md) | Secret yönetimi, güvenlik kontrol listesi |
| [Dağıtım Rehberi](docs/DEPLOYMENT.md) | Ortam kurulumu, backup/restore |
| [Keycloak 2FA Kurulumu](docs/KEYCLOAK_2FA_SETUP.md) | OTP ve Remember Me yapılandırması |
| [Toplantı 2 Sunum Akışı](docs/TOPLANTI_2_SUNUM_AKISI.md) | İzleme toplantısı sunum planı |
| [Toplantı 2 Konuşma Notları](docs/TOPLANTI_2_KONUSMA_NOTLARI.md) | Sunum konuşma rehberi |

---

## 9. Lisans

MIT License — Detaylar için [LICENSE](LICENSE) dosyasına bakınız.
