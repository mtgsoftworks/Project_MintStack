# MintStack Finance Portal

**👤 Yazar / Bakım Yapan:** Mesut Taha Güven
**📅 Son Güncelleme:** 7 Haziran 2026
**📝 Teslim Güncelleme:** 7 Haziran 2026 (Analiz ve teslim dokümanları eşitlendi)
**🔖 Sürüm:** 2.0.0

> **Türkiye Odaklı Finans Platformu** — Gerçek zamanlı piyasa izleme, portföy yönetimi, teknik analiz, simülasyon ve bildirim altyapısı.
> *Bu doküman projenin profesyonel teknik haritasıdır. Projeyi araştıran veya projeye dahil olan kişiler için neyin nerede olduğu, proje yapısı ve çalışma prensipleri aşağıda detaylandırılmıştır.*

![Java 21](https://img.shields.io/badge/Java-21-orange) ![Spring Boot 3.4](https://img.shields.io/badge/Spring_Boot-3.4.2-green) ![React 18](https://img.shields.io/badge/React-18-blue) ![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue) ![PostgreSQL 15](https://img.shields.io/badge/PostgreSQL-15-blue) ![Docker](https://img.shields.io/badge/Docker-Compose-blue) ![Keycloak 26](https://img.shields.io/badge/Keycloak-26-red)

---

## 📌 İçindekiler / Proje Haritası

1. [Proje Tanımı ve Kapsam](#1-proje-tanımı)
2. [Teknoloji Yığını](#2-teknoloji-yığını)
3. [Sistem Mimarisi](#3-sistem-mimarisi)
4. [Proje İçerikleri ve Dizin Yapısı (Nerede Ne Var?)](#4-proje-i̇çerikleri-ve-dizin-yapısı-nerede-ne-var)
5. [Hızlı Başlangıç](#5-hızlı-başlangıç)
6. [Çalışma Profilleri](#6-çalışma-profilleri)
7. [Yerel Geliştirme](#7-yerel-geliştirme)
8. [Komutlar ve Skriptler](#8-komutlar)
9. [Code Style (Kodlama Standartları)](#9-code-style)
10. [Ortam Değişkenleri (.env)](#10-ortam-değişkenleri)
11. [Yaygın Sorunlar ve Çözümler](#11-yaygın-sorunlar)
12. [Detaylı Dokümantasyon Haritası](#12-detaylı-dokümantasyon-haritası)
13. [Lisans](#13-lisans)

---

## 1. Proje Tanımı

MintStack Finance Portal, Türkiye finans piyasalarına (BIST, döviz, tahvil, fon, VİOP) yönelik entegre bir finans veri ve portföy yönetim platformudur. Sistem; çoklu veri kaynağı entegrasyonu (TCMB, TEFAS, BIST DataStore, Yahoo Finance, Alpha Vantage, Finnhub), gerçek zamanlı fiyat akışı (WebSocket/STOMP), gelişmiş portföy simülasyonu (al/sat emirleri, limit/stop emir tipleri, emir yaşam döngüsü), teknik analiz araçları (Monte Carlo, backtesting, teknik indikatörler), haber agregasyonu ve kurumsal düzeyde güvenlik (OAuth2/OIDC, RBAC, 2FA) sunmaktadır.

### 1.1 Hedef Kullanıcı ve Senaryolar

| Kullanıcı Tipi | Temel Senaryolar |
|---|---|
| **Bireysel Yatırımcı** | Piyasa takibi, sanal portföy yönetimi, fiyat alarmları, watchlist |
| **Finans Öğrencisi** | Simülasyon ile deneyim kazanma, teknik analiz araçları |
| **Sistem Yöneticisi** | Admin paneli, kullanıcı yönetimi, simülasyon kontrolü, loglama |

### 1.2 MVP Kapsamı

- ✅ Çoklu veri kaynağından (TCMB, TEFAS, BIST DataStore, Yahoo, Alpha Vantage, Finnhub) piyasa verisi toplama
- ✅ Gerçek zamanlı fiyat güncelleme (WebSocket/STOMP)
- ✅ Sanal portföy oluşturma ve yönetimi (MARKET, LIMIT, STOP emir tipleri)
- ✅ Emir yaşam döngüsü (PENDING → PARTIALLY_FILLED → FILLED / CANCELED / REJECTED)
- ✅ Teknik analiz: Monte Carlo simülasyonu, backtesting, korelasyon matrisi, RSI/MA stratejileri
- ✅ Haber akışı (RSS entegrasyonu + kategori yönetimi)
- ✅ Fiyat alarmları ve bildirim sistemi (uygulama içi + SMTP aktifse e-posta)
- ✅ Keycloak ile OAuth2/OIDC kimlik doğrulama ve RBAC
- ✅ 2FA (TOTP) desteği
- ✅ Admin paneli (kullanıcı, simülasyon, sistem yönetimi)
- ✅ Tam observability stack (Prometheus, Grafana, OpenSearch, OTEL, Logstash)
- ✅ Excel/PDF raporlama ve dışa aktarım
- ✅ Çok dilli arayüz (TR/EN, i18next)
- ✅ Dark/Light tema desteği

### 1.3 Güncel Durum Notları

- Mobil uygulama bu sürümün kapsamında değildir; proje web portalı olarak sunulur.
- Kripto para desteği aktif kullanıcı akışından kaldırılmıştır; demo ve jüri anlatımında kripto modülü sunulmamalıdır.
- TCMB, TEFAS, BIST DataStore ve Yahoo Finance keyless/public kaynak olarak çalışır; kullanıcıdan API key/base URL istenmez.
- Alpha Vantage, Finnhub ve LLM enrichment API anahtarları sadece `ADMIN` rolü tarafından yönetilir; test/normal kullanıcı kendi API key'ini ekleyemez.
- RSS haberleri isteğe bağlı LLM enrichment ile zenginleştirilebilir. GitHub Models/OpenAI-compatible endpoint desteklenir.
- Admin/Ayarlar altında geçmiş veri backfill akışı vardır; 7/30/90 gün ve 1 yıl gibi periyotlarla `price_history` doldurulabilir.
- Fiyat alarmı uygulama içi bildirim ve opsiyonel e-posta üretir; sesli alarm/browser push bu sürümde tam bağlı değildir.
- CI akışında backend verify, Flyway migrate+validate, frontend lint/typecheck/test/build ve compose validation bulunur.

---

## 2. Teknoloji Yığını

### 2.1 Backend

| Teknoloji | Versiyon | Kullanım Amacı |
|---|---|---|
| **Java** | 21 | Ana programlama dili |
| **Spring Boot** | 3.4.2 | Uygulama çatısı, REST API, güvenlik, scheduler |
| **Spring Security (Config)** | 6.4.2 | JWT tabanlı kimlik doğrulama ve yetkilendirme |
| **Spring Data JPA + Hibernate** | 3.4.2 | ORM, veri erişim katmanı |
| **Spring WebSocket (STOMP)** | 6.2.2 | Gerçek zamanlı veri akışı |
| **Spring Kafka** | 3.3.2 | Asenkron olay akışı ve log boru hattı |
| **Spring Cache + Redis** | 3.4.2 | Sıcak veri cache, performans optimizasyonu |
| **Spring WebFlux (WebClient)** | 6.2.2 | Dış API istemcisi (reaktif HTTP) |
| **Flyway** | 10.20.1 | Veritabanı şema migrasyon yönetimi (24 migrasyon) |
| **MapStruct** | 1.5.5.Final | DTO-Entity dönüşümleri |
| **Lombok** | 1.18.30 | Boilerplate kod azaltma |
| **Bucket4j** | 8.7.0 | Rate limiting |
| **Resilience4j** | 2.2.0 | Circuit breaker, hata toleransı |
| **SpringDoc OpenAPI** | 2.8.6 | Swagger UI, API dokümantasyonu |
| **Apache POI** | 5.2.5 | Excel dışa aktarım |
| **iText** | 8.0.2 | PDF dışa aktarım |
| **Quartz** | 2.3.2 | Zamanlanmış görevler (veri toplama, temizlik) |
| **Log4j2 + JSON Layout** | 2.24.3 | Yapısal loglama |
| **Micrometer Prometheus Registry** | 1.14.3 | Prometheus export metrikleri |

### 2.2 Frontend

| Teknoloji | Versiyon | Kullanım Amacı |
|---|---|---|
| **React** | 18.3.1 | UI kütüphanesi |
| **TypeScript** | 5.9.3 | Tip güvenli geliştirme |
| **Vite** | 5.4.21 | Build aracı ve dev server |
| **Redux Toolkit + RTK Query** | 2.11.2 | State yönetimi ve API veri çekme |
| **React Router** | 6.30.3 | Sayfa yönlendirme |
| **Tailwind CSS** | 3.4.19 | Utility-first CSS framework |
| **Radix UI** | 1.x / 2.x (karma) | Erişilebilir headless UI bileşenleri |
| **Recharts** | 2.15.4 | Grafik ve veri görselleştirme |
| **Formik + Yup** | 2.4.9 / 1.7.1 | Form yönetimi ve doğrulama |
| **Keycloak JS** | 26.2.3 | Frontend tarafında kimlik doğrulama |
| **STOMP.js + SockJS** | 7.2.1 / 1.6.1 | WebSocket istemcisi |
| **i18next** | 23.16.8 | Çoklu dil desteği (TR/EN) |
| **Lucide React** | 0.303.0 | İkon kütüphanesi |
| **Sonner** | 1.3.1 | Toast bildirimleri |

### 2.3 Altyapı ve DevOps

| Teknoloji | Versiyon | Kullanım Amacı |
|---|---|---|
| **Docker Compose** | v2.24+ | Konteyner orkestrasyon |
| **PostgreSQL** | 15-alpine | İlişkisel veritabanı |
| **Redis** | 7-alpine | Cache ve oturum yönetimi |
| **Keycloak** | 26.5.4 | OAuth2/OIDC kimlik sunucusu |
| **OpenLDAP** | 1.5.0 | Dizin hizmetleri, Keycloak federation |
| **Kafka (KRaft)** | 7.5.0 | Olay akışı, SASL/PLAIN doğrulama |
| **Nginx** | alpine | API Gateway, reverse proxy |
| **Prometheus** | 2.48.0 | Metrik toplama |
| **Grafana** | 10.2.2 | Metrik görselleştirme ve alarm |
| **AlertManager** | 0.26.0 | Alarm yönetimi |
| **OpenSearch** | 2.13.0 | Log indeksleme ve arama |
| **OpenSearch Dashboards** | 2.13.0 | Log görselleştirme |
| **Logstash** | 8.9.0 | Log işleme pipeline |
| **OTEL Collector** | 0.91.0 | Distributed tracing |

### 2.4 Test Altyapısı

| Kategori | Araçlar |
|---|---|
| **Backend Unit Test** | JUnit 5, Mockito, Spring Boot Test (40 test sınıfı) |
| **Backend Entegrasyon** | Testcontainers 1.19.3, H2 |
| **Frontend Unit Test** | Vitest 1.6.1, Testing Library 14.3.1, MSW 2.12.7 |
| **Frontend E2E** | Playwright 1.57.0 |
| **Kod Kapsamı** | JaCoCo 0.8.11 (minimum %50 satır, %35 branch) |

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
│  TypeScript + Redux │               │  Java 21            │
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

## 4. Proje İçerikleri ve Dizin Yapısı (Nerede Ne Var?)

Bu bölüm, projeyi araştıran, kodu okuyan veya özellik eklemek isteyen geliştiricilerin, aradıkları servisleri, tasarımları ve dosyaları nerede bulacağını gösteren **Profesyonel İçerik Haritasıdır**. Mimarinin tam olarak nasıl yapılandırıldığı sistemin içerisinde aşağıda ifade edildiği gibidir:

```
Project_MintStack/
│
├── backend/                          # Spring Boot API (Java 21)
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
│   │   └── db/migration/             # Flyway migrasyonları (V1-V24)
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
├── docker-compose.prod.yml            # Üretim ortamı
│
└── docs/                              # Proje dokümantasyonu
    ├── ARCHITECTURE.md                # Sistem mimarisi
    ├── TASARIM_MIMARISI_VE_MODELLEME.md  # Tasarım ve modelleme
    ├── ADR.md                         # Mimari karar kayıtları
    ├── api-docs.md                    # API referansı
    ├── SECURITY.md                    # Güvenlik rehberi
    ├── DEPLOYMENT.md                  # Dağıtım rehberi
    ├── RAILWAY_DEPLOYMENT.md          # Railway minimum servis deploy rehberi
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
| **OpenAPI JSON** | http://localhost:8088/api-docs | Makine okunur API sözleşmesi |
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

Yalnızca PostgreSQL, Redis, Keycloak, OpenLDAP, Backend, Frontend ve Nginx. Kafka, OpenSearch ve observability servisleri hariç. Düşük RAM'li makineler için idealdir (~4 GB yeterli).

### Production

```bash
docker compose -f docker-compose.prod.yml up -d
```

Docker Secrets, ağ segmentasyonu (data, app, auth, obs katmanları ayrı network), kaynak limitleri, backend replica desteği.

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

## 8. Komutlar

### Lint

```bash
# Frontend (ESLint)
cd frontend && npm run lint

# Frontend type check
cd frontend && npm run typecheck
```

### Build

```bash
# Frontend
cd frontend && npm run build        # dist/ klasörüne üretir

# Backend
cd backend && ./mvnw clean package   # target/ klasörüne JAR üretir
./mvnw clean package -DskipTests     # Testleri atlayarak
```

### Docker

```bash
docker compose up -d                 # Tüm servisleri başlat
docker compose down                  # Servisleri durdur
docker compose logs -f backend       # Backend loglarını izle
docker compose up -d --build         # Yeniden build ve başlat
```

---

## 9. Code Style

### Java (Backend)

- **Java 21** özellikleri ve modern Java söz dizimi kullanılır (records, pattern matching, text blocks)
- **Lombok** (`@Slf4j`, `@RequiredArgsConstructor`, `@Builder`)
- **MapStruct** DTO-Entity dönüşümleri için
- Global exception handling: `GlobalExceptionHandler`
- Rate limiting: `Bucket4j` ile controller seviyesinde
- Log format: Structured JSON (Log4j2)

### TypeScript/React (Frontend)

- **Functional components** + **hooks**
- **Redux Toolkit** global state, **RTK Query** API çağrıları
- **Tailwind CSS** styling
- **i18next** çoklu dil (TR/EN)

### Git Conventions

- **Branch:** `feature/`, `bugfix/`, `hotfix/`, `release/`
- **Commit:** `type(scope): description` — örn: `feat(portfolio): add risk analysis endpoint`

---

## 10. Ortam Değişkenleri

Tüm değişkenler `.env.example` dosyasında tanımlıdır:

| Kategori | Değişkenler |
|---|---|
| Database | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| Redis | `REDIS_PASSWORD` |
| Keycloak | `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_FINANCE_BACKEND_SECRET` |
| Kafka | `KAFKA_SASL_PASSWORD` |
| OpenSearch | `OPENSEARCH_INITIAL_ADMIN_PASSWORD` |
| External APIs | `ALPHA_VANTAGE_API_KEY`, `FINNHUB_API_KEY` |

Kaynak notları:

- External API anahtarları UI üzerinden sadece `ADMIN` rolü ile yönetilir; normal/test kullanıcı API key ekleyemez, silemez veya test edemez.
- `APP_EXTERNAL_API_TEFAS_ENABLED=true` ile TEFAS fon verisi keyless çalışır.
- TCMB, BIST DataStore ve Yahoo Finance için API key/base URL gerekmez.
- `APP_EXTERNAL_API_BIST_DATASTORE_ENABLED=true` ile tahvil/bono günlük bülteni ve VIOP günlük bülteni gerçek Borsa İstanbul dosyalarından okunur.
- `APP_EXTERNAL_API_FINTABLES_ENABLED=false` güvenli varsayılandır; Fintables UI'da aktif kaynak gibi sunulmamalıdır.
- LLM enrichment için `APP_NEWS_LLM_*` değişkenleri kullanılır. GitHub Models örneği: `APP_NEWS_LLM_BASE_URL=https://models.github.ai/inference`, `APP_NEWS_LLM_ENDPOINT=/chat/completions`, `APP_NEWS_LLM_MODEL=openai/gpt-5`.

---

## 11. Yaygın Sorunlar

| Sorun | Çözüm |
|---|---|
| Backend başlamıyor | PostgreSQL çalışıyor mu? `docker compose ps postgres` |
| Frontend API hatası | Backend sağlıklı mı? `curl http://localhost:8088/actuator/health` |
| WebSocket bağlantı hatası | Nginx proxy + SockJS fallback kontrol edin |
| Keycloak token geçersiz | Token süresi, realm ayarları ve CORS kontrol edin |

---

## 12. Detaylı Dokümantasyon Haritası

Projeye ait diğer tüm sistem ve operasyon notları, araştırma yapan takım üyeleri ve denetçiler için `docs/` klasöründe kategorilere ayrılmıştır:

| Doküman | Açıklama |
|---|---|
| [Güncel Durum](docs/CURRENT_STATUS.md) | Son sürüm kapsamı, veri kaynakları, alarm davranışı, CI/CD ve bilinen sınırlar |
| [Teslim Kontrol Listesi](docs/DELIVERY_CHECKLIST.md) | 7 Haziran isterlerinin madde madde durum ve kanıt eşleştirmesi |
| [Sistem Mimarisi](docs/ARCHITECTURE.md) | C4 container view, servis sorumlulukları, veri akışları, ER modeli |
| [İşletim Rehberi](docs/OPERATIONS.md) | Güvenlik, dağıtım profilleri, secret yönetimi ve 2FA kurulumu |
| [Mimari Karar Kayıtları](docs/ADR.md) | Teknoloji seçim gerekçeleri (10 ADR) |
| [API Referansı](docs/api-docs.md) | Endpoint listesi ve kullanım örnekleri |
| [Teknik Dokümantasyon](docs/TEKNIK_DOKUMANTASYON.md) | Modüller, kaynaklar, scheduler, ortam değişkenleri ve kabul kriterleri |
| [Railway Deployment](docs/RAILWAY_DEPLOYMENT.md) | Railway minimum servis kurulumu, Variables ve auto-deploy akışı |
| [TEFAS Entegrasyon Kararı](docs/TEFAS_API_SECIMI_VE_ENTEGRASYON.md) | TEFAS kaynak seçimi, adapter yaklaşımı ve runtime ayarları |
| [BIST DataStore Entegrasyonu](docs/BIST_DATASTORE_ENTEGRASYONU.md) | Tahvil/bono ve VIOP public dosya/bülten entegrasyonu |
| [Borsa Terim Sözlüğü](docs/BORSA_TERIM_SOZLUGU_TEFAS_KAYNAKLI.md) | Kullanıcıya açık olmayan iç kullanım kavram sözlüğü |
| [Teslim Sunumu Kaynağı](docs/MintStack_Finance_Portal_Sunum.md) | PPTX üretimi için Pandoc sunum kaynağı |

---

## 13. Lisans

MIT License — Detaylar için [LICENSE](LICENSE) dosyasına bakınız.
