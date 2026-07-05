# AGENTS.md - MintStack Finance Portal

Bu dosya, AI asistanlarının ve geliştiricilerin proje hakkında hızlı bilgi alması için hazırlanmıştır.

## Proje Yapısı

```
Project_MintStack/
├── backend/                    # Spring Boot 3.4.2 API (Java 21)
│   ├── src/main/java/com/mintstack/finance/
│   │   ├── config/            # Güvenlik, Redis, Kafka, WebSocket config
│   │   ├── controller/        # REST endpoints (20 controller)
│   │   ├── service/           # Business logic (67 service)
│   │   ├── repository/        # Data access layer (17 repository)
│   │   ├── entity/            # JPA entities (19 entity)
│   │   ├── dto/               # Request/Response DTOs
│   │   ├── exception/         # Global exception handling
│   │   ├── aspect/            # AOP aspects (RateLimit, Logging)
│   │   ├── scheduler/         # Cron jobs (MarketData, News, Cleanup)
│   │   └── mapper/            # MapStruct mappers
│   └── src/main/resources/
│       ├── application.yml    # Ana konfigürasyon
│       └── db/migration/      # Flyway SQL scripts (V1-V31)
│
├── frontend/                   # React 18 SPA (Vite + TypeScript)
│   └── src/
│       ├── components/
│       │   ├── ui/           # ShadCN/Radix UI components
│       │   ├── layout/       # Layout, Sidebar, Header
│       │   ├── market/       # Market domain components
│       │   └── charts/       # Recharts wrappers
│       ├── pages/             # Sayfa bileşenleri (22 page)
│       ├── services/         # API & WebSocket services
│       ├── store/             # Redux Toolkit + RTK Query
│       ├── hooks/             # Custom React hooks
│       ├── lib/               # Utils, currency, validation
│       └── locales/           # i18n translations
│
├── docker/                     # Docker konfigürasyonları
│   ├── postgres/              # DB init scripts
│   ├── redis/                 # Redis config
│   ├── kafka/                 # Kafka SASL config
│   ├── otel/                  # OpenTelemetry config
│   ├── logstash/              # Logstash pipeline
│   ├── prometheus/            # Prometheus & alerts
│   ├── grafana/               # Dashboard provisioning
│   └── openldap/              # OpenLDAP config
│
├── keycloak/                   # Keycloak 26.5.4 realm export
├── docs/                       # Dokümantasyon
├── docker-compose.yml          # Development orchestration
├── docker-compose.prod.yml      # Production orchestration
└── .env.example               # Environment template
```

## Teknoloji Stack

| Katman | Teknoloji | Versiyon |
|--------|-----------|----------|
| Backend | Spring Boot | 3.4.2 |
| Java | OpenJDK/Eclipse Temurin | 21 |
| Database | PostgreSQL | 15 |
| Cache | Redis | 7 |
| Messaging | Apache Kafka | 7.5.0 (KRaft) |
| Search | OpenSearch | 2.13.0 |
| Auth | Keycloak | 26.5.4 |
| Frontend | React | 18.3.1 |
| Build | Vite | 7.3.6 |
| State | Redux Toolkit | 2.11.2 (lock) |
| UI | Tailwind + Radix | 3.4.19 / lockfile |

## Komutlar

### Lint

```bash
# Frontend (ESLint)
cd frontend
npm run lint

# Backend (Maven Checkstyle - eğer yapılandırılmışsa)
cd backend
./mvnw checkstyle:check
```

### Testler

```bash
# Frontend (Vitest)
cd frontend
npm test

# Frontend E2E (Playwright)
cd frontend
npx playwright install    # İlk çalıştırmada
npm run test:e2e

# Backend (JUnit + Testcontainers)
cd backend
./mvnw test

# Backend integration tests
./mvnw verify -Pintegration-test
```

### Build

```bash
# Frontend (Vite)
cd frontend
npm run build              # dist/ klasörüne üretir

# Backend (Maven)
cd backend
./mvnw clean package       # target/ klasörüne JAR üretir
./mvnw clean package -DskipTests  # Testleri atlayarak
```

### Docker

```bash
# Tüm servisleri başlat
docker-compose up -d

# Minimal stack (düşük RAM)
docker-compose -f docker-compose.light.yml up -d

# Servisleri durdur
docker-compose down

# Logları izle
docker-compose logs -f backend
docker-compose logs -f frontend

# Yeniden build
docker-compose up -d --build
```

## Temel Mimari Kararlar

| Karar | Seçim | Gerekçe |
|-------|-------|---------|
| Backend Framework | Spring Boot 3.4.2 | Java 21+, OAuth2, WebSocket, kurumsal destek |
| Frontend Framework | React 18 + TypeScript + Vite | Component-based, hızlı dev, zengin ekosistem |
| Veritabanı | PostgreSQL 15 | ACID, JSONB, full-text search |
| Cache | Redis 7 | Session, rate-limiting, market data cache |
| Message Queue | Kafka 7.5.0 | Event-driven, yüksek throughput |
| Identity Provider | Keycloak 26.5.4 | OAuth2/OIDC, LDAP federation, 2FA |
| Logging | OpenSearch 2.13.0 | Log aggregation, distributed tracing |
| Tracing | OpenTelemetry | Vendor-agnostic observability |

Detaylı ADR'ler için: `docs/ADR.md`

## Code Style Guidelines

### Java (Backend)

- **Java 21** özellikleri kullanın (records, pattern matching, virtual threads)
- **Lombok** kullanın (`@Slf4j`, `@RequiredArgsConstructor`, `@Builder`)
- **MapStruct** DTO mapping için
- Constructor injection tercih edin
- Global exception handling: `GlobalExceptionHandler`
- Rate limiting: `Bucket4j` ile Redis destekli HTTP filter seviyesinde
- Log format: Structured JSON (Log4j2)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {
    private final MarketDataRepository repository;
    private final CacheManager cacheManager;
    
    @Cacheable(value = "currencyRates", key = "#code")
    public CurrencyRate getCurrencyRate(String code) {
        log.debug("Fetching currency rate for: {}", code);
        return repository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + code));
    }
}
```

### TypeScript/React (Frontend)

- **Functional components** + **hooks** kullanın
- **Redux Toolkit** global state için
- **RTK Query** API calls için
- **Tailwind CSS** styling için
- **Yup** form validasyon için
- **i18next** çoklu dil desteği

```typescript
// RTK Query example
export const marketApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getCurrencies: builder.query<CurrenciesResponse, void>({
      query: () => '/market/currencies',
      providesTags: ['Currencies'],
    }),
  }),
});
```

## Güvenlik Yapılandırması

| Özellik | Konum | Not |
|---------|-------|-----|
| JWT/OAuth2 | `SecurityConfig.java` | Keycloak JWKS validation |
| CSP | `SecurityConfig.java`, `nginx/nginx.prod.conf` | Dev backend relaxed; production Nginx CSP halen `unsafe-inline` içerir |
| Redis Type Safety | `RedisConfig.java` | Whitelist-based PolymorphicTypeValidator |
| Webhook Signature | `AlertWebhookSecurityService.java` | HMAC-SHA256 with constant-time comparison |
| Rate Limiting | `Bucket4j` | Redis-backed distributed |

## ortam Değişkenleri

Tüm ortam değişkenleri `.env.example` dosyasında tanımlıdır.

| Kategori | Önemli Değişkenler |
|----------|-------------------|
| Database | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| Redis | `REDIS_PASSWORD` |
| Keycloak | `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_FINANCE_BACKEND_SECRET` |
| Kafka | `KAFKA_SASL_PASSWORD`, `KAFKA_CLUSTER_ID` |
| OpenSearch | `OPENSEARCH_INITIAL_ADMIN_PASSWORD` |
| External APIs | `ALPHA_VANTAGE_API_KEY`, `FINNHUB_API_KEY` |

## CI/CD Pipeline

GitHub Actions workflow: `.github/workflows/ci.yml`

| Stage | Araçlar |
|-------|---------|
| Backend Test | Maven, JaCoCo, OWASP Dependency Check |
| Frontend Test | ESLint, TypeScript, Vitest |
| Compose Validation | docker-compose config |
| Security Scan | Gitleaks; OWASP Dependency Check ve Trivy raporları (şu anda non-blocking) |

## Yaygın Sorunlar

### Backend başlamıyor
- PostgreSQL çalışıyor mu? `docker-compose ps postgres`
- Keycloak hazır mı? `docker-compose logs keycloak`
- Environment variables doğru mu? `.env` dosyasını kontrol edin

### Frontend API hatası
- Backend ayağa kalktı mı? `http://localhost:8088/actuator/health`
- CORS ayarları doğru mu? `application.yml` içinde `cors.allowed-origins`
- Keycloak token geçerli mi? Browser console'da network tab

### WebSocket bağlantı hatası
- Backend WebSocket endpoint aktif mi? `http://localhost:8088/ws`
- SockJS fallback çalışıyor mu? Browser console'da kontrol

## İlgili Dokümantasyon

- `README.md` - Proje genel bakış
- `docs/api-docs.md` - API quick reference
- `docs/ARCHITECTURE.md` - Sistem mimarisi
- `docs/ADR.md` - Mimari kararlar
- `docs/DEPLOYMENT.md` - Deployment rehberi
- `docs/SECURITY.md` - Güvenlik checklist
- `docs/KEYCLOAK_2FA_SETUP.md` - 2FA kurulumu
- `docs/API_VERSIONING.md` - API versiyonlama
