# AGENTS.md - MintStack Finance Portal

Bu dosya, AI asistanlarının ve geliştiricilerin proje hakkında hızlı bilgi alması için hazırlanmıştır.

## Proje Yapısı

```
MintStack-Finance/
├── backend/                    # Spring Boot API (Java 17)
│   ├── src/main/java/com/mintstack/finance/
│   │   ├── config/            # Güvenlik, Redis, Kafka, WebSocket config
│   │   ├── controller/        # REST endpoints
│   │   ├── service/           # Business logic
│   │   ├── repository/        # Data access layer
│   │   ├── entity/            # JPA entities
│   │   ├── dto/               # Request/Response DTOs
│   │   ├── exception/         # Global exception handling
│   │   └── scheduler/         # Cron jobs (TCMB, haberler)
│   └── src/main/resources/
│       ├── application.yml    # Ana konfigürasyon
│       └── db/migration/      # Flyway SQL scripts
│
├── frontend/                   # React SPA (Vite)
│   └── src/
│       ├── components/        # Yeniden kullanılabilir UI bileşenleri
│       ├── pages/             # Sayfa bileşenleri
│       ├── services/          # API client services
│       ├── store/             # Redux Toolkit state management
│       ├── context/           # Auth & Theme context
│       ├── hooks/             # Custom React hooks
│       └── utils/             # Helper functions
│
├── docker/                     # Docker konfigürasyonları
│   ├── postgres/              # DB init scripts
│   ├── redis/                 # Redis config
│   ├── kafka/                 # Kafka SASL config
│   ├── otel/                  # OpenTelemetry config
│   ├── logstash/              # Logstash pipeline
│   ├── prometheus/            # Prometheus & alerts
│   ├── grafana/               # Dashboard provisioning
│   └── openldap/              # LDAP certs & LDIF
│
├── keycloak/                   # Realm export (users, roles)
├── docs/                       # Dokümantasyon
├── docker-compose.yml          # Development orchestration
└── .env.example               # Environment template
```

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
| Backend Framework | Spring Boot 3.4 | Java 17+, OAuth2, WebSocket, kurumsal destek |
| Frontend Framework | React 18 + Vite | Component-based, hızlı dev, zengin ekosistem |
| Veritabanı | PostgreSQL 15 | ACID, JSONB, full-text search |
| Cache | Redis 7 | Session, rate-limiting, market data cache |
| Message Queue | Kafka 3.5 | Event-driven, yüksek throughput |
| Identity Provider | Keycloak 23 | OAuth2/OIDC, LDAP federation, 2FA |
| Logging | OpenSearch | Log aggregation, distributed tracing |
| Tracing | OpenTelemetry | Vendor-agnostic observability |

Detaylı ADR'ler için: `docs/ADR.md`

## Code Style Guidelines

### Java (Backend)

- **Java 17+** özellikleri kullanın (records, pattern matching, text blocks)
- **Lombok** kullanın (`@Slf4j`, `@RequiredArgsConstructor`, `@Builder`)
- **MapStruct** DTO mapping için
- Package-private constructor injection tercih edin
- Global exception handling: `GlobalExceptionHandler`
- Rate limiting: `Bucket4j` ile controller seviyesinde
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

### JavaScript/React (Frontend)

- **Functional components** + **hooks** kullanın
- **Redux Toolkit** global state için
- **RTK Query** API calls için
- **Tailwind CSS** styling için
- **Yup** form validasyon için
- **i18next** çoklu dil desteği

```javascript
// Service example
export const marketApi = createApi({
  reducerPath: 'marketApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api/v1' }),
  endpoints: (builder) => ({
    getCurrencies: builder.query({
      query: () => '/market/currencies',
      transformResponse: (response) => response.data,
    }),
  }),
});
```

### Git Conventions

- **Branch naming**: `feature/`, `bugfix/`, `hotfix/`, `release/`
- **Commit message**: `type(scope): description`
  - `feat(portfolio): add risk analysis endpoint`
  - `fix(auth): resolve token refresh issue`
  - `docs(readme): update installation steps`

## Ortam Değişkenleri

Tüm ortam değişkenleri `.env.example` dosyasında tanımlıdır.

| Kategori | Önemli Değişkenler |
|----------|-------------------|
| Database | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| Redis | `REDIS_PASSWORD` |
| Keycloak | `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_FINANCE_BACKEND_SECRET` |
| Kafka | `KAFKA_SASL_PASSWORD` |
| OpenSearch | `OPENSEARCH_INITIAL_ADMIN_PASSWORD` |
| External APIs | `ALPHA_VANTAGE_API_KEY`, `FINNHUB_API_KEY` |

## Yaygın Sorunlar

### Backend başlamıyor
- PostgreSQL çalışıyor mu? `docker-compose ps postgres`
- Keycloak hazır mı? `docker-compose logs keycloak`
- Environment variables doğru mu? `.env` dosyasını kontrol edin

### Frontend API hatası
- Backend ayağa kalktı mı? `http://localhost:18080/actuator/health`
- CORS ayarları doğru mu? `application.yml` içinde `cors.allowed-origins`
- Keycloak token geçerli mi? Browser console'da network tab

### WebSocket bağlantı hatası
- Backend WebSocket endpoint aktif mi? `http://localhost:18080/ws`
- SockJS fallback çalışıyor mu? Browser console'da kontrol

## İlgili Dokümantasyon

- `README.md` - Proje genel bakış
- `docs/ARCHITECTURE.md` - Sistem mimarisi detayları
- `docs/ADR.md` - Mimari karar kayıtları
- `docs/SECURITY.md` - Güvenlik dokümantasyonu
- `docs/DEPLOYMENT.md` - Deployment rehberi
- `docs/KEYCLOAK_2FA_SETUP.md` - 2FA kurulumu
