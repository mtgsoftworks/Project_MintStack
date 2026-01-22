# Architecture Decision Records (ADR)

Bu dokümantasyon, MintStack Finance projesinde alınan önemli mimari kararları içerir.

---

## ADR-001: Backend Framework Seçimi

**Tarih:** 2025-12-01  
**Durum:** Kabul Edildi

### Bağlam
Finansal veri işleme, real-time güncellemeler ve kurumsal güvenlik gereksinimleri olan bir backend framework'e ihtiyacımız var.

### Karar
**Spring Boot 3.2** seçildi.

### Gerekçe
- Java 17+ desteği ve modern features
- Kapsamlı security (Spring Security + OAuth2)
- WebSocket desteği (STOMP)
- Microservices-ready architecture
- Türkiye'de geniş geliştirici havuzu
- Enterprise-grade stability

### Alternatifler
- Node.js/NestJS - Real-time için iyi ama Java ekosistemi tercihi
- Go - Performance iyi ama ekosistem kısıtlı
- .NET - İyi alternatif ama Java tercih edildi

---

## ADR-002: Frontend Framework Seçimi

**Tarih:** 2025-12-01  
**Durum:** Kabul Edildi

### Bağlam
Modern, responsive ve performanslı bir SPA gerekiyor.

### Karar
**React 18 + Vite** seçildi.

### Gerekçe
- Component-based architecture
- Virtual DOM performance
- Zengin ekosistem (Redux, RTK Query)
- Vite ile hızlı development
- TypeScript uyumluluğu

### UI Kütüphaneleri
- **Tailwind CSS** - Utility-first CSS
- **shadcn/ui** - Radix-based accessible components
- **Lucide React** - Icon library

---

## ADR-003: Veritabanı Seçimi

**Tarih:** 2025-12-01  
**Durum:** Kabul Edildi

### Bağlam
Finansal verilerin güvenli ve performanslı depolanması gerekiyor.

### Karar
**PostgreSQL 15** seçildi.

### Gerekçe
- ACID compliance
- JSON/JSONB desteği
- Güçlü indexing
- Full-text search
- Time-series data için uygun
- Open-source ve ücretsiz

### Cache Layer
**Redis 7** - Session, rate-limiting ve market data cache için.

---

## ADR-004: Authentication & Authorization

**Tarih:** 2025-12-01  
**Durum:** Kabul Edildi

### Bağlam
Kurumsal seviyede güvenli kimlik doğrulama gerekiyor.

### Karar
**Keycloak** Identity Provider olarak seçildi.

### Gerekçe
- OAuth2/OIDC standartları
- LDAP/Active Directory entegrasyonu
- 2FA/MFA desteği
- Role-based access control (RBAC)
- SSO (Single Sign-On)
- Self-service password reset

### Token Strategy
- JWT access tokens (5 dakika)
- Refresh tokens (30 dakika)
- Realm roles: USER, ADMIN

---

## ADR-005: Real-time Data Delivery

**Tarih:** 2025-12-01  
**Durum:** Kabul Edildi

### Bağlam
Piyasa verileri real-time olarak frontend'e iletilmeli.

### Karar
**WebSocket + STOMP** protokolü seçildi.

### Gerekçe
- Bi-directional communication
- STOMP message broker abstraction
- SockJS fallback for older browsers
- Spring WebSocket entegrasyonu

### Topics
- `/topic/currencies` - Döviz güncellemeleri
- `/topic/stocks` - Hisse güncellemeleri
- `/topic/news` - Haber güncellemeleri

---

## ADR-006: API Provider Strategy

**Tarih:** 2025-12-15  
**Durum:** Kabul Edildi

### Bağlam
Farklı veri kaynakları entegre edilmeli ve kullanıcı seçebilmeli.

### Karar
Multi-provider architecture ile pluggable API clients.

### Desteklenen Providers
| Provider | Veri Türleri |
|----------|--------------|
| TCMB | Döviz kurları |
| Yahoo Finance | Hisse, kripto, döviz, haberler |
| Alpha Vantage | Hisse, kripto, döviz, haberler |
| Finnhub | Hisse, kripto, döviz, haberler |

### Rate Limiting
- Provider bazlı rate limiting
- Fallback mechanism
- Circuit breaker pattern (Resilience4j)

---

## ADR-007: Logging & Monitoring

**Tarih:** 2025-12-01  
**Durum:** Kabul Edildi

### Bağlam
Distributed tracing ve centralized logging gerekiyor.

### Karar
**ELK Stack alternatifi olarak OpenSearch** + **OpenTelemetry**

### Stack
- Log4j2 → Kafka → Logstash → OpenSearch
- OpenTelemetry Collector for traces
- Prometheus metrics (planned)

### Log Format
Structured JSON logging:
```json
{
  "timestamp": "2026-01-22T10:30:00",
  "level": "INFO",
  "traceId": "abc123",
  "service": "finance-backend",
  "message": "API request completed",
  "duration": 45
}
```

---

## ADR-008: Deployment Strategy

**Tarih:** 2025-12-01  
**Durum:** Kabul Edildi

### Bağlam
Development ve production ortamları için container-based deployment gerekiyor.

### Karar
**Docker Compose** (development) + **Kubernetes-ready** architecture

### Container Strategy
- Multi-stage Docker builds
- Health checks
- Resource limits
- Secrets management (Docker Secrets)

### CI/CD
- GitHub Actions
- Netlify (frontend deployment option)

---

## ADR-009: Database Migration Strategy

**Tarih:** 2025-12-01  
**Durum:** Kabul Edildi

### Bağlam
Database schema değişiklikleri versiyonlanmalı ve tracked olmalı.

### Karar
**Flyway** migration tool seçildi.

### Conventions
- `V{version}__{description}.sql` format
- Version numaraları ardışık
- Out-of-order migrations enabled (development)
- Baseline version: V1

---

## ADR-010: State Management (Frontend)

**Tarih:** 2025-12-01  
**Durum:** Kabul Edildi

### Bağlam
Complex application state yönetimi gerekiyor.

### Karar
**Redux Toolkit + RTK Query**

### Gerekçe
- Predictable state container
- DevTools support
- RTK Query for API caching
- Automatic cache invalidation
- Optimistic updates

### Slice Structure
```
store/
├── slices/
│   ├── authSlice.js
│   ├── uiSlice.js
│   └── portfolioSlice.js
└── api/
    ├── baseApi.js
    ├── marketApi.js
    ├── portfolioApi.js
    └── settingsApi.js
```

---

## Karar Geçmişi

| ADR | Tarih | Durum |
|-----|-------|-------|
| ADR-001 | 2025-12-01 | Kabul Edildi |
| ADR-002 | 2025-12-01 | Kabul Edildi |
| ADR-003 | 2025-12-01 | Kabul Edildi |
| ADR-004 | 2025-12-01 | Kabul Edildi |
| ADR-005 | 2025-12-01 | Kabul Edildi |
| ADR-006 | 2025-12-15 | Kabul Edildi |
| ADR-007 | 2025-12-01 | Kabul Edildi |
| ADR-008 | 2025-12-01 | Kabul Edildi |
| ADR-009 | 2025-12-01 | Kabul Edildi |
| ADR-010 | 2025-12-01 | Kabul Edildi |
