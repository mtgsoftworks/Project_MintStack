# Mimari Karar Kayıtları (ADR)

> Son doğrulama: 5 Temmuz 2026.

Bu doküman, MintStack Finance Portal projesindeki mimari kararları ve gerekçelerini içerir. Her karar, bağlamı, alınan kararı, gerekçeyi ve sonuçları belgeler.

---

## ADR-001: Backend Framework - Spring Boot 3.4

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

Finans portalı için güvenilir, ölçeklenebilir ve kurumsal standartlara uygun bir backend framework seçilmesi gerekiyordu. OAuth2 kimlik doğrulama, WebSocket ile gerçek zamanlı veri akışı ve zengin ekosistem desteği zorunluydu.

### Karar

Spring Boot 3.4 (Java 21) backend framework olarak seçildi.

### Gerekçe

- **Java 21:** Modern dil özellikleri (records, pattern matching, text blocks) ile daha okunabilir ve bakımı kolay kod
- **OAuth2/OIDC:** Spring Security ile Keycloak entegrasyonu için yerleşik destek
- **WebSocket:** STOMP protokolü ile gerçek zamanlı piyasa verisi yayını
- **Kurumsal destek:** Geniş topluluk, Pivotal/VMware desteği, uzun vadeli LTS sürümleri
- **Ekosistem:** Spring Data JPA, Spring Kafka, Spring WebFlux, Actuator, Micrometer ile tam stack desteği

### Sonuçlar

- Backend `backend/` altında Spring Boot 3.4.2 ile geliştirildi
- Controller, Service, Repository katmanlı mimari uygulandı
- Flyway ile veritabanı migrasyonları, Lombok ve MapStruct ile kod kalitesi artırıldı

---

## ADR-002: Frontend Framework - React 18 + TypeScript + Vite

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

Kullanıcı arayüzü için modern, performanslı ve geliştirici deneyimi yüksek bir frontend stack seçilmesi gerekiyordu. Gerçek zamanlı veri gösterimi, karmaşık formlar ve çoklu dil desteği gereksinimleri vardı.

### Karar

React 18, TypeScript ve Vite frontend stack olarak seçildi.

### Gerekçe

- **Component-based:** Yeniden kullanılabilir bileşenler ile modüler ve bakımı kolay UI
- **Hızlı dev:** Vite ile HMR (Hot Module Replacement) ve anlık build süreleri
- **Zengin ekosistem:** Redux Toolkit, RTK Query, Recharts, i18next, Tailwind CSS ile tam destek
- **TypeScript:** Tip güvenliği ile daha az runtime hatası ve daha iyi IDE desteği
- **React 18:** Concurrent rendering, Suspense ve otomatik batching ile performans iyileştirmeleri

### Sonuçlar

- Frontend `frontend/` altında React 18.3.1, TypeScript 5.9.3 ve Vite 7.3.6 ile geliştirildi
- Redux Toolkit state yönetimi, RTK Query API çağrıları için kullanıldı
- Code splitting (lazy loading) ile ilk yükleme süresi optimize edildi

---

## ADR-003: Veritabanı - PostgreSQL 15

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

Kalıcı iş verisi (kullanıcı, portföy, işlem geçmişi, fiyat verileri) için güvenilir ve esnek bir ilişkisel veritabanı seçilmesi gerekiyordu.

### Karar

PostgreSQL 15 veritabanı olarak seçildi.

### Gerekçe

- **ACID:** Finansal işlemler için tam tutarlılık ve atomiklik garantisi
- **JSONB:** Esnek veri yapıları (API yanıtları, konfigürasyonlar) için yerel JSON desteği
- **Full-text search:** Haber, enstrüman aramaları için yerleşik tam metin arama
- **Olgunluk:** 30+ yıllık geliştirme, güçlü topluluk ve kurumsal kullanım
- **Açık kaynak:** Lisans maliyeti yok, self-hosted veya yönetilen servis seçenekleri

### Sonuçlar

- PostgreSQL 15-alpine Docker imajı ile çalıştırılıyor
- Flyway ile 31 migrasyon ile şema yönetimi
- JPA/Hibernate ile ORM katmanı

---

## ADR-004: Cache - Redis 7

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

Piyasa verisi, döviz kurları ve sık sorgulanan veriler için yüksek performanslı bir cache katmanı gerekiyordu. Ayrıca oturum yönetimi ve rate limiting sayaçları için uygun bir çözüm aranıyordu.

### Karar

Redis 7 cache katmanı olarak seçildi.

### Gerekçe

- **Session:** Dağıtık oturum depolama için Spring Session entegrasyonu
- **Rate-limiting:** Bucket4j ile token bucket sayaçlarının Redis backend desteği
- **Market data cache:** Döviz kurları, hisse fiyatları gibi sıcak verilerin hızlı erişimi
- **Düşük gecikme:** Bellek tabanlı depolama ile mikrosaniye seviyesinde yanıt süreleri
- **Yaygın kullanım:** Spring Cache abstraction ile sorunsuz entegrasyon

### Sonuçlar

- Redis 7-alpine Docker imajı ile çalıştırılıyor
- `@Cacheable` ile servis katmanında cache kullanımı
- Rate limiting için IP ve kullanıcı bazlı bucket depolama

---

## ADR-005: Message Queue - Kafka 3.5

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

Uygulama loglarının merkezi toplanması, piyasa verisi event'lerinin yayınlanması ve asenkron işleme için bir mesaj kuyruğu sistemi gerekiyordu.

### Karar

Apache Kafka 3.5 (Confluent Platform 7.5.0) mesaj kuyruğu olarak seçildi.

### Gerekçe

- **Event-driven:** Olay tabanlı mimari ile gevşek bağlı bileşenler
- **Yüksek throughput:** Saniyede milyonlarca mesaj işleme kapasitesi
- **Kalıcılık:** Disk tabanlı log ile mesaj kaybı riski minimize
- **KRaft modu:** Zookeeper bağımlılığının kaldırılması ile operasyonel basitlik
- **Ekosistem:** Logstash, OpenSearch ile log pipeline entegrasyonu

### Sonuçlar

- Kafka KRaft modunda SASL/PLAIN ile güvenli çalışıyor
- Loglar Kafka üzerinden Logstash'e, oradan OpenSearch'e akıyor
- Market data event'leri WebSocket ile frontend'e yayınlanıyor

---

## ADR-006: Identity Provider - Keycloak 26

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

Merkezi kimlik doğrulama, rol bazlı yetkilendirme ve kurumsal LDAP entegrasyonu için bir kimlik sağlayıcı gerekiyordu. İki faktörlü kimlik doğrulama (2FA) desteği zorunluydu.

### Karar

Keycloak 26 kimlik sağlayıcı olarak seçildi.

### Gerekçe

- **OAuth2/OIDC:** Standart protokoller ile frontend ve backend entegrasyonu
- **LDAP federation:** Kurumsal dizin (OpenLDAP) ile kullanıcı senkronizasyonu
- **2FA (TOTP):** İki faktörlü kimlik doğrulama ile güvenlik artırımı
- **Realm yönetimi:** Çoklu tenant desteği, client tanımları
- **Açık kaynak:** Red Hat desteği, topluluk katkıları

### Sonuçlar

- `mintstack-finance` realm, `finance-frontend` ve `finance-backend` client'ları tanımlandı
- USER ve ADMIN rolleri ile yetkilendirme
- Keycloak JS ile frontend, JWT doğrulama ile backend entegrasyonu

---

## ADR-007: Logging - OpenSearch

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

Dağıtık sistemde merkezi log toplama, tam metin arama ve log analizi için bir log aggregasyon çözümü gerekiyordu.

### Karar

OpenSearch 2.13 log indeksleme ve arama platformu olarak seçildi.

### Gerekçe

- **Log aggregation:** Tüm servis loglarının tek merkezde toplanması
- **Distributed tracing:** Span ve trace verilerinin loglarla birlikte analiz edilmesi
- **Full-text search:** Elasticsearch uyumlu API ile güçlü arama yetenekleri
- **Açık kaynak:** Apache 2.0 lisansı, Elasticsearch fork'u
- **Güvenlik:** Yerleşik güvenlik eklentisi ile kimlik doğrulama ve yetkilendirme

### Sonuçlar

- Kafka → Logstash → OpenSearch pipeline ile log akışı
- OpenSearch Dashboards ile log arama ve görselleştirme
- Structured JSON (Log4j2) log formatı

---

## ADR-008: Tracing - OpenTelemetry

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

Dağıtık sistemde istek akışlarının izlenmesi, performans darboğazlarının tespiti ve vendor bağımsız bir gözlemlenebilirlik standardı gerekiyordu.

### Karar

OpenTelemetry (OTEL) dağıtık izleme standardı olarak seçildi.

### Gerekçe

- **Vendor-agnostic:** Tek bir API ile farklı backend'lere (Jaeger, Zipkin, OpenSearch) veri gönderimi
- **CNCF standardı:** Endüstri genelinde kabul gören açık standart
- **Spring Boot entegrasyonu:** Micrometer Tracing ile otomatik span oluşturma
- **Esneklik:** Collector ile veri işleme, sampling, export stratejileri

### Sonuçlar

- OTEL Collector 0.91.0 ile span toplama
- OpenSearch'e trace verisi gönderimi
- Spring Actuator + Micrometer ile metrik ve trace birleşimi

---

## ADR-009: API Versioning - Header-based (X-API-Version)

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

API'nin zaman içinde evrimleşmesi, geriye dönük uyumluluk ve istemcilerin kullandıkları sürümü bilmesi için bir versiyonlama stratejisi gerekiyordu.

### Karar

URL tabanlı versiyonlama (`/api/v1/*`, `/api/v2/*`) ile birlikte response header'larında (`X-API-Version`, `X-API-Deprecated`, `X-API-Sunset`) versiyon bilgisi iletilmesi kararlaştırıldı.

### Gerekçe

- **X-API-Version:** Yanıtta kullanılan API sürümünün açık iletilmesi
- **X-API-Deprecated:** Kullanımdan kaldırılan sürümlerin uyarılması
- **X-API-Sunset:** Kaldırılma tarihinin RFC 7231 formatında bildirilmesi
- **URL routing:** Path-based versiyonlama ile net endpoint ayrımı
- **Geçiş süresi:** Deprecated sürümler 6 ay desteklenir

### Sonuçlar

- `ApiVersioningConfig` ile tüm `/api/*` yanıtlarına header ekleniyor
- Mevcut sürüm: 1.0.0
- `X-API-Min-Version` ile minimum desteklenen sürüm bildirimi

---

## ADR-010: Rate Limiting - Bucket4j

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

API kötüye kullanımının önlenmesi, DDoS koruması ve adil kaynak paylaşımı için rate limiting mekanizması gerekiyordu. Farklı kullanıcı tipleri için farklı limitler tanımlanmalıydı.

### Karar

Bucket4j token bucket algoritması ile IP ve rol bazlı rate limiting uygulanacak.

### Gerekçe

- **Token bucket:** Doğal burst toleransı ile esnek limit yönetimi
- **IP bazlı:** Anonim kullanıcılar için IP adresi ile sınırlama (varsayılan 100 req/dk)
- **Rol bazlı:** Authenticated kullanıcılar 200 req/dk, Admin 500 req/dk
- **HTTP filter seviyesi:** Tüm `/api/**` trafiğine tutarlı limit uygulama
- **Redis uyumlu:** Dağıtık ortamda Redis backend ile ölçeklenebilirlik

### Sonuçlar

- `RateLimitConfig` ile IP, user ve admin bucket'ları yönetiliyor
- `RateLimitFilter` ile merkezi uygulama; kaldırılan annotation/aspect akışı kullanılmaz
- Runtime'da `app.rate-limit.*` ile ayar güncellemesi

---

## ADR-011: Circuit Breaker - Resilience4j

**Tarih:** 2025-03-07  
**Durum:** Kabul Edildi

### Bağlam

TCMB, Alpha Vantage, Finnhub, Yahoo Finance, RSS News gibi dış API'lara yapılan çağrılarda hata toleransı sağlanması gerekiyordu. Bir sağlayıcının kesintisi tüm sistemi etkilememeliydi.

### Karar

Resilience4j Circuit Breaker ve Retry mekanizmaları ile dış API koruması uygulanacak.

### Gerekçe

- **External API protection:** Sağlayıcı kesintilerinde hızlı başarısızlık (fail-fast)
- **Fallback:** Kesinti durumunda alternatif veri veya cache'ten yanıt
- **Retry:** Geçici hatalarda otomatik yeniden deneme
- **Micrometer entegrasyonu:** Circuit breaker metrikleri Prometheus/Grafana'da izlenebilir
- **Spring Boot native:** `@CircuitBreaker` annotation ile deklaratif kullanım

### Sonuçlar

- FinnhubClient, AlphaVantageClient, YahooFinanceClient, TcmbApiClient, RssNewsClient'ta `@CircuitBreaker` kullanımı
- Her sağlayıcı için ayrı circuit breaker instance (finnhubApi, alphaVantageApi, yahooFinanceApi, tcmbApi, rssNewsApi)
- `application.yml` içinde `resilience4j.circuitbreaker` konfigürasyonu
- Grafana'da circuit-breaker-dashboard ile izleme

---

## ADR İndeksi

| ADR | Konu | Durum |
|-----|------|-------|
| ADR-001 | Backend Framework - Spring Boot 3.4 | Kabul Edildi |
| ADR-002 | Frontend Framework - React 18 + TypeScript + Vite | Kabul Edildi |
| ADR-003 | Veritabanı - PostgreSQL 15 | Kabul Edildi |
| ADR-004 | Cache - Redis 7 | Kabul Edildi |
| ADR-005 | Message Queue - Kafka 3.5 | Kabul Edildi |
| ADR-006 | Identity Provider - Keycloak 26 | Kabul Edildi |
| ADR-007 | Logging - OpenSearch | Kabul Edildi |
| ADR-008 | Tracing - OpenTelemetry | Kabul Edildi |
| ADR-009 | API Versioning - Header-based | Kabul Edildi |
| ADR-010 | Rate Limiting - Bucket4j | Kabul Edildi |
| ADR-011 | Circuit Breaker - Resilience4j | Kabul Edildi |
