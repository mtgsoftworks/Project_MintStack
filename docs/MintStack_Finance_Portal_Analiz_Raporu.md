# MintStack Finance Portal Analiz Raporu

Tarih: 2026-05-14

## 1. Amac

MintStack Finance Portal; portfoy takibi, periyodik piyasa verisi toplama, haber akisi, teknik analiz, alarm ve yonetim modullerini tek bir kurumsal finans portalinda birlestiren moduler monolith bir projedir.

Bu calismada hedeflenenler:

- Finans portali olarak eksik fonksiyonlari tamamlamak.
- TEFAS kaynakli fon verisini sisteme almak.
- Doviz al/sat islemini portfoye dahil etmek.
- Swagger/OpenAPI erisim problemini gidermek.
- CI/CD, rate limiting, scheduler, alarm ve dokumantasyon eksiklerini kapatmak.
- Admin/user yetki sinirlarini netlestirmek.

## 2. Kapsam

Degerlendirilen ve uygulanan alanlar:

- Backend: Spring Boot API, security, scheduler, external API clients, portfolio engine.
- Frontend: React/Vite ekranlari, RTK Query servisleri, navigation.
- Veritabani: Flyway migration, glossary/runtime tablolar, enum constraint genisletme.
- Docker: Compose ve Nginx reverse proxy.
- CI/CD: GitHub Actions validation.
- Dokumantasyon: teknik analiz raporu, ER diyagrami, yonetim modulu diyagrami.

## 3. Teknikler ve Uygulanan Cozumler

| Alan | Uygulama | Sonuc |
|---|---|---|
| TEFAS | `TefasFundClient` yeni TEFAS JSON endpointini kullanir: `/api/funds/fonGnlBlgSiraliGetir`. | Fon fiyatlari `FUND` enstrumanina ve `price_history` tablosuna yazilir. |
| Fintables | Provider enum, validation ve capability matrisi eklendi. | Resmi policy lock ile pasif (`APP_EXTERNAL_API_FINTABLES_ENABLED=false` varsayilan). |
| Haber | RSS akisi mevcut yapi uzerinden korunur. | Uluslararasi ve yerel RSS kaynaklari scheduler ile toplanir, LLM enrichment zincirine baglanir. |
| OpenAPI | `/api-docs`, `/swagger-ui.html`, `/swagger-ui/index.html`, `/v3/api-docs` uyumlulugu eklendi. | Eski ve yeni istemci pathleri calisir. |
| Rate limiting | Redis destekli token bucket karari eklendi; memory fallback korundu. | Dagitik ortamda ortak limit, testlerde geriye uyumlu memory bucket. |
| Doviz portfoy | Kur kaydi geldikce `USDTRY`, `EURTRY` gibi `CURRENCY` enstrumanlari olusur. | Doviz alim/satim portfoy emir motoruna dahil olur. |
| Teknik gostergeler | ATR, ADX, OBV, VWAP, CCI, MFI, Williams %R ve data quality eklendi. | Volatilite, trend gucu, hacim ve para akisi kapsami genisledi. |
| Kavram sozlugu | `glossary_terms` tablosu, public API ve frontend sayfasi eklendi. | Borsa/fon terimleri TEFAS kaynakli seed verilerle baslar. |
| Runtime ayarlar | `runtime_settings` tablosu ve admin endpointi eklendi. | Dinamik yonetilebilir ayarlar icin temel olustu. |
| Alarm | Alertmanager webhook endpointi eklendi. | CIDR allowlist + HMAC SHA-256 imza dogrulamasi ile production hardening uygulandi. |

## 4. Mimari Degerlendirme

### 4.1 Backend Mimari

Backend katmanlari:

- `controller`: REST API ve OpenAPI dokumantasyonu.
- `service`: is kurallari, portfoy, teknik analiz, glossary, runtime ayarlar.
- `service/external`: TCMB, TEFAS, Yahoo, Alpha Vantage, Finnhub, RSS istemcileri.
- `scheduler`: piyasa verisi, haber ve cleanup cron gorevleri.
- `repository`: JPA veri erisimi.
- `entity`: PostgreSQL domain modeli.
- `filter`: rate limiting.

Yeni moduller:

- `GlossaryService` ve `GlossaryController`
- `RuntimeSettingsService` ve `RuntimeSettingsController`
- `TefasFundClient` ve `TefasFundDataService`
- `NewsEnrichmentService`
- `AlertWebhookController`
- `AlertWebhookSecurityService`
- Redis destekli `RateLimitConfig.tryConsume`

### 4.2 Veri Akisi

TEFAS fon veri akisi:

1. `MarketDataScheduler.fetchFundPrices` cron ile calisir.
2. `TefasFundDataService.refreshFundPrices` TEFAS istemcisini cagirir.
3. `TefasFundClient` TEFAS JSON endpointinden fon fiyatlarini alir.
4. Her fon `Instrument(type=FUND, exchange=TEFAS)` olarak upsert edilir.
5. Gunluk fiyat `PriceHistory` tablosuna yazilir.

Doviz portfoy akisi:

1. TCMB/non-live FX scheduler kur bilgisini alir.
2. `MarketDataMaintenanceService.saveCurrencyRates` kur kaydini saklar.
3. Ayni kayittan `USDTRY` gibi `CURRENCY` enstrumani olusur/guncellenir.
4. Frontend doviz ekranindan portfoy secilerek alim/satim emri verilir.
5. `PortfolioOrderExecutionService` currency enstrumanlarini BIST seansina baglamaz.

### 4.3 Docker ve OpenAPI

Gelistirme gateway pathleri:

- Swagger UI: `http://localhost:8088/swagger-ui.html`
- Swagger UI index: `http://localhost:8088/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8088/api-docs`
- OpenAPI v3 uyumluluk: `http://localhost:8088/v3/api-docs`

Nginx dev konfigi `/v3/api-docs` ve `/v3/api-docs/` pathlerini backend'e proxy eder. Backend tarafinda da compatibility forward controller eklidir.

## 5. Guvenlik Degerlendirmesi

| Kontrol | Durum |
|---|---|
| OAuth2/OIDC | Keycloak JWT resource server aktif. |
| RBAC | `ADMIN` ve authenticated user ayrimi Spring Security ve method security ile uygulanir. |
| Public endpointler | Market read-only, news read-only, glossary read-only, health, prometheus, swagger/api-docs. |
| Admin endpointler | `/api/v1/admin/**` admin rolune bagli. |
| Alert webhook | Alertmanager entegrasyonu icin webhook endpointi + source IP + HMAC signature dogrulama eklendi. |
| Rate limiting | Redis dagitik token bucket; memory fallback. |
| Secret yonetimi | `.env.example` ve env placeholder kullanimi mevcut; runtime ayar tablosu secret saklamak icin degil. |
| API key validation | TCMB/TEFAS/RSS public, Fintables ve LLM key format/konfigurasyon bazli. |

## 6. Admin/User Yetki Matrisi

| Modul | Anonymous | User | Admin |
|---|---:|---:|---:|
| Health/Info | OK | OK | OK |
| Swagger/OpenAPI | OK | OK | OK |
| Market read-only | OK | OK | OK |
| News read-only | OK | OK | OK |
| Glossary read-only | OK | OK | OK |
| Portfolio CRUD/trade | Yok | OK | OK |
| Watchlist | Yok | OK | OK |
| Price alerts | Yok | OK | OK |
| Settings/API keys | Yok | OK | OK |
| Data source preference | Yok | OK | OK |
| Admin dashboard | Yok | Yok | OK |
| Rate limit admin | Yok | Yok | OK |
| Glossary admin CRUD | Yok | Yok | OK |
| Runtime settings | Yok | Yok | OK |
| Simulation admin | Yok | Yok | OK |
| Alertmanager webhook | OK* | OK* | OK* |

`*` Alertmanager webhook otomasyon icindir; production'da gateway/IP allowlist onerilir.

## 7. Scheduler Plani

| Enstruman/Veri | Varsayilan Sikligi | Gerekce |
|---|---|---|
| TCMB doviz | 10:30 ve 16:30, hafta ici | Resmi kur gun icinde sinirli yayinlanir. |
| Non-TCMB forex | 5 dakikada bir, hafta ici | Canli 7/24 zorunlu degil; API limitleri korunur. |
| BIST hisse | 20 saniye | Gelistirme/test icin sik; production'da piyasa saatine gore gevsetilebilir. |
| Fon/TEFAS | 18:30, hafta ici onerilir | Fon fiyatlari genellikle gun sonu/NAV bazlidir. |
| VIOP | 30 saniye | Daha yuksek hassasiyet. |
| Kripto | Kapsam disi | Aktif kullanici akisi ve juri demosundan kaldirildi. |
| Haber RSS | 15 dakika | RSS kaynaklari icin dengeli polling. |
| Cleanup | 02:00 | Dusuk trafik penceresi. |

## 8. DB Tasarimi ve ER

Ana tablolar:

- `users`, `user_api_configs`, `user_data_preferences`
- `portfolios`, `portfolio_items`, `portfolio_transactions`
- `instruments`, `price_history`, `currency_rates`
- `news`, `news_categories`
- `watchlists`, `watchlist_items`, `price_alerts`, `user_notifications`
- `glossary_terms`, `runtime_settings`

ER diyagrami: `docs/diagrams/er-diagram.mmd`

## 9. Teknoloji Yigini

| Katman | Teknoloji |
|---|---|
| Backend | Java 17, Spring Boot 3.4.2, Spring Security, Spring Data JPA |
| API Docs | springdoc-openapi 2.8.6 |
| DB | PostgreSQL 15, Flyway |
| Cache/Rate Limit | Redis 7, Bucket4j 8.7.0 |
| Messaging | Kafka 3.5/Confluent 7.5 |
| Frontend | React 18, TypeScript 5.9, Vite 5, Redux Toolkit |
| Auth | Keycloak 26 |
| Observability | Prometheus, Grafana, AlertManager, OpenSearch, Logstash, OTEL |
| CI | GitHub Actions, Maven, npm, Docker Buildx |

## 10. Sonuc ve Kalan Riskler

Tamamlanan kritik eksikler:

- TEFAS fon verisi sisteme eklendi.
- Fintables provider resmi policy lock ile pasif olarak tanimlandi.
- Kavram sozlugu backend/frontend tamamlandi.
- Redis rate limiting eklendi.
- OpenAPI/Swagger erisim uyumlulugu saglandi.
- Doviz portfoy alim/satim akisi eklendi.
- Teknik gosterge kapsami genisletildi.
- Alert webhook ve runtime setting altyapisi eklendi.

Kalan riskler:

- Fintables policy lock su an bilincli olarak kapalidir; aktif etmeden once endpoint kontrati ve lisans/kota dogrulamasini tamamlayin.
- `runtime_settings` su an ayar envanteri ve admin yonetimi saglar; tum ayarlarin hot-reload edilmesi icin ek adaptor gerekir.
- Frontend TypeScript strict modu kapali; orta vadede strict migration onerilir.
