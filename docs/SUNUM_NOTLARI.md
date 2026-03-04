# Ön İzleme Toplantısı — Sunum Notları

> **Toplantı:** 4 Mart 2026, 15:30–18:00  
> **Amaç:** Tasarım dokümanları ve kod üzerinden proje ilerlemesini gösterme

---

## Sunum Sırası (~45 dk + soru-cevap)

| # | Konu | Süre | Gösterilecek |
|---|---|---|---|
| 1 | Proje tanıtımı ve hedef | 2 dk | README.md |
| 2 | Mimari genel bakış | 5 dk | ARCHITECTURE.md, docker-compose.yml |
| 3 | Veri modeli ve entity'ler | 5 dk | ER diyagramı, Flyway migrasyonları |
| 4 | Backend katman yapısı | 5 dk | SecurityConfig, PortfolioService, scheduler |
| 5 | Veri akışı ve entegrasyonlar | 5 dk | MarketDataScheduler, Kafka, Redis, OTEL |
| 6 | Frontend | 5 dk | RTK Query, i18n, ProtectedRoute |
| 7 | Test altyapısı | 2 dk | JUnit, Vitest, Playwright |
| 8 | Canlı demo | 8 dk | Login → Dashboard → Portföy → Grafana |
| 9 | Teknik borçlar (dürüst) | 3 dk | Aşağıda detay |
| 10 | Kapanış | 2 dk | — |

---

## Teknik Borçlar — Dürüst Açıklama

### 1. TypeScript `strict: false`
> "Hızlı ilerleme için strict kapalı bıraktık. Geri açtığımızda yüzlerce hata çıktı — pipeline kırılmasın diye kademeli geçiş izliyoruz. Test altyapısını tip kapsamına aldık."

### 2. Büyük Servis Sınıfları
> "MarketDataService 575 satır, bölme planımız var ama aceleci refactor yapmak yerine güvenli turu bekliyoruz. Portföy tarafında zaten alt servislere bölündü."

### 3. Actuator `/prometheus` Public
> "Dev ortamı için açık, production'da internal ağa kısıtlanacak. 5 dakikalık konfigürasyon değişikliği."

---

## Muhtemel Zor Sorular

| Soru | Cevap |
|---|---|
| Neden mikroservis değil? | Modüler monolith — tek takım, 52+ servis ama tek deploy birimi. Mikroservis operasyonel yükü bu aşamada gereksiz. |
| Kafka consumer'sız topic var mıydı? | Vardı, düzelttik — DLT + retry + metrik sayaçları ekledik. |
| Strict mode neden kapalı? | Yüzlerce implicit-any hatası — kademeli geçiş stratejisi. |
| OTEL gerçekten çalışıyor mu? | 34 @Observed annotation — tüm dış API, scheduler, Kafka, WebSocket izleniyor. |
| Redis TTL uygun mu? | Hisse 60sn, döviz 5dk, haber 10dk — env-based konfigüre edilebilir. |

---

## Demo Senaryosu

1. Login → Keycloak auth → Dashboard (1 dk)
2. Döviz kurları ekranı — TCMB verisi (1 dk)
3. Hisse senetleri → Detay → Fiyat grafiği (1 dk)
4. Portföy oluştur → Simülasyon alım emri (2 dk)
5. İzleme listesi + Fiyat alarmı (1 dk)
6. Grafana → Metrikler (1 dk)
7. Admin dashboard (1 dk)

---

## Kapanış

> "14 MVP özelliğini tamamladık. DLT korumalı Kafka, 34 observation point ile distributed tracing, borsa saatlerine uygun Redis TTL var. Teknik borçları dürüstçe paylaştık — bunlar sonraki sprint hedeflerimiz."
