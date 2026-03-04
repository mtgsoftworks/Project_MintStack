# Mimari Karar Kayıtları (ADR)

Bu dosya, proje boyunca alınmış temel teknik kararların gerekçeleriyle birlikte kaydıdır. Her karar, alternatif seçenekler ve seçim gerekçesiyle belgelenmiştir.

---

## ADR-001: Backend — Spring Boot 3.4 + Java 17

- **Karar:** Backend framework olarak Spring Boot 3.4.2 ve Java 17 kullanıldı.
- **Alternatifler:** Node.js/Express, Python/FastAPI, Go/Gin, .NET
- **Neden:**
  - Kurumsal güvenlik ekosistemi olgunluğu (Spring Security, OAuth2 Resource Server)
  - Zengin scheduler, WebSocket, JPA/Hibernate, cache, mail desteği
  - Kafka, Redis, OpenSearch entegrasyonları için production-ready kütüphaneler
  - Resilience4j, Bucket4j, Micrometer gibi ekosistem araçları
  - MapStruct + Lombok ile boilerplate azaltma
- **Sonuç:** Modüler monolith yapıda hızlı teslimat + güçlü kurumsal altyapı. Gerektiğinde bounded context bazlı ayrıştırmaya uygun modüler paket yapısı.

---

## ADR-002: Frontend — React 18 + Vite + TypeScript

- **Karar:** SPA katmanı React 18, Vite 5, TypeScript ile geliştirildi.
- **Alternatifler:** Angular, Vue 3, Next.js (SSR)
- **Neden:**
  - Zengin bileşen ekosistemi (Radix UI, Recharts, Lucide)
  - Hızlı geliştirme döngüsü (Vite HMR < 100ms)
  - Redux Toolkit + RTK Query ile güçlü state ve API yönetimi
  - Code splitting (lazy loading) ile performans optimizasyonu
  - Keycloak JS ile sorunsuz OAuth2 entegrasyonu
- **Sonuç:** UI geliştirme hızı yüksek. Tip güvenliği aşamalı olarak güçlendiriliyor (strict mode geçiş planı mevcut).

---

## ADR-003: Veritabanı — PostgreSQL 15 + Redis 7

- **Karar:** Kalıcı veri PostgreSQL, sıcak veri ve cache Redis ile yönetilir.
- **Alternatifler:** MySQL, MongoDB, sadece PostgreSQL (cache'siz)
- **Neden:**
  - PostgreSQL: ACID uyumluluğu, JSON desteği, güçlü sorgu optimizasyonu, Flyway ile kolay migrasyon
  - Redis: Sub-millisecond okuma latency, TTL-based cache invalidation, rate limiting desteği
  - İkisi birlikte: Yazma güvenilirliği + okuma performansı dengesi
- **Sonuç:** Okuma yoğun endpointlerde (piyasa verileri, döviz kurları) Redis cache hit oranı yüksek. İş kuralları PostgreSQL ACID garantisi altında.

---

## ADR-004: Kimlik Yönetimi — Keycloak 26

- **Karar:** OAuth2/OIDC kimlik doğrulama ve yetkilendirme için Keycloak kullanıldı.
- **Alternatifler:** Auth0, custom JWT auth, Firebase Auth, AWS Cognito
- **Neden:**
  - Self-hosted: Veri kontrolü tamamen projede
  - Realm ve client yönetimi (frontend + backend ayrı client)
  - Rol bazlı yetkilendirme (USER, ADMIN)
  - LDAP federation desteği (OpenLDAP entegrasyonu)
  - 2FA (TOTP) desteği
  - Remember Me ve session yönetimi
  - PKCE (S256) ile güvenli frontend auth akışı
- **Sonuç:** Kimlik yönetimi uygulamadan tamamen ayrıştırıldı. Güvenlik merkezi ve yapılandırılabilir hale geldi.

---

## ADR-005: Gerçek Zamanlı — WebSocket (STOMP/SockJS)

- **Karar:** Canlı fiyat ve event aktarımı için STOMP protokolü ve SockJS fallback kullanıldı.
- **Alternatifler:** SSE (Server-Sent Events), long polling, GraphQL subscriptions
- **Neden:**
  - Çift yönlü iletişim (client → server price update isteği)
  - STOMP topic yapısı ile esnek kanal yönetimi (`/topic/prices/*`)
  - SockJS fallback ile eski tarayıcı uyumluluğu
  - Spring WebSocket entegrasyonu ile kolay JWT doğrulama
  - Nginx üzerinden WebSocket reverse proxy desteği
- **Sonuç:** Dashboard ve piyasa ekranlarında polling olmadan anlık fiyat güncellemesi. Frontend tarafında `@stomp/stompjs` ile güvenli bağlantı.

---

## ADR-006: Çoklu Veri Sağlayıcı Modeli

- **Karar:** TCMB, Yahoo Finance, Alpha Vantage ve Finnhub olmak üzere 4 harici veri sağlayıcı + simülasyon motoru desteklendi.
- **Alternatifler:** Tek sağlayıcıya bağımlılık, ücretli premium API
- **Neden:**
  - Veri kapsamını genişletmek (her sağlayıcının güçlü olduğu alan farklı)
  - Tek sağlayıcı bağımlılığını azaltmak (rate limit, API downtime riskleri)
  - Provider tercih/fallback mekanizması ile esnek veri toplama
  - Kullanıcının kendi API anahtarını tanımlayabilmesi (UserApiConfig)
  - Simülasyon modu ile gerçek veri olmadan da çalışabilme
- **Sonuç:** `MarketDataProviderResolver` ile yetenek matrisi bazlı otomatik sağlayıcı seçimi. Sağlayıcı değişikliği uygulama koduna dokunmadan yapılabilir.

---

## ADR-007: Gözlemlenebilirlik — Prometheus + Grafana + OpenSearch + OTEL + Logstash

- **Karar:** Tam gözlemlenebilirlik (observability) stack'i kuruldu.
- **Alternatifler:** ELK Stack, Datadog, New Relic, sadece application log
- **Neden:**
  - **Metrikler:** Prometheus + Grafana ile uygulama performans izleme
  - **Loglar:** Kafka → Logstash → OpenSearch pipeline ile merkezi log yönetimi
  - **İzler (Traces):** OTEL Collector ile distributed tracing
  - **Alarmlar:** AlertManager ile proaktif sorun tespiti
  - Tüm bileşenler açık kaynak ve self-hosted
- **Sonuç:** Sorun tespiti ve kök neden analizi hızlandı. Log4j2 JSON layout ile yapısal loglama, Micrometer ile otomatik metrik toplama.

---

## ADR-008: Docker Compose ile Profil Bazlı Çalışma

- **Karar:** Varsayılan (full), secure-dev, lightweight ve production olmak üzere 4 Docker Compose profili tanımlandı.
- **Alternatifler:** Kubernetes, tek compose dosyası, manual kurulum
- **Neden:**
  - Geliştirme ortamında kaynak verimliliği (lightweight profil)
  - Güvenlik test senaryoları (secure-dev profil)
  - Üretim ortamı hazırlığı (prod profil)
  - Tek komutla tüm stack'i ayağa kaldırma kolaylığı
- **Sonuç:** `docker compose up -d` ile 15 servis dakikalar içinde hazır. Profil değişikliği `-f` flag'i ile anında.

---

## ADR-009: Simülasyon Motoru

- **Karar:** Gerçek piyasa verisi olmadan da portföy yönetimi ve emir testlerinin yapılabilmesi için kapsamlı bir simülasyon motoru geliştirildi.
- **Alternatifler:** Sadece gerçek veri, statik test verisi
- **Neden:**
  - Borsa kapalıyken bile geliştirme ve test yapabilme
  - API rate limit'lerinden bağımsız demo senaryoları
  - Jüri sunumları ve demo ortamları için güvenilir veri
  - Market event ve haber senaryoları ile gerçekçi simülasyon
- **Sonuç:** 12 dosyalık simülasyon modülü (PriceSimulationEngine, MarketEventEngine, NewsScenarioEngine) ile hisse, döviz, kripto, endeks ve VİOP fiyatları simüle edilebiliyor. `isSimulated` flag ile gerçek ve simüle veri ayrımı netleştirildi.

---

## ADR-010: Teknik Analiz ve Risk Yönetimi

- **Karar:** Monte Carlo simülasyonu, backtesting, korelasyon matrisi, RSI ve Moving Average stratejileri entegre edildi.
- **Alternatifler:** Sadece basit portföy takibi, 3. parti analiz araçlarına yönlendirme
- **Neden:**
  - Kullanıcıya portföy risk değerlendirmesi sunmak
  - Yatırım stratejilerini geçmiş verilerle test edebilmek
  - Eğitim amaçlı teknik analiz araçları sağlamak
- **Sonuç:** BacktestingService, MonteCarloService, TechnicalIndicatorService ve trading stratejileri ile zengin analiz altyapısı.
