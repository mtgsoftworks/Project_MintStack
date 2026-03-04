# Toplantı 2 — Sunum Akışı (4 Mart, 15:30–18:00)

Bu doküman, izleme toplantısında ekran paylaşırken izlenecek net sunum sırasını verir.

---

## 0. Açılış (2 dakika)

- Projenin hedefi: Türkiye finans piyasalarına yönelik güvenli, gerçek zamanlı ve kapsamlı finans platformu
- Bu toplantıda göstereceklerimiz:
  1. Mimari ve tasarım kararları
  2. Domain modelleme ve iş akışları
  3. Kod–doküman uyumu (canlı gezinti)
  4. Güvenlik ve operasyon altyapısı
  5. Canlı demo

---

## 1. Problem ve Kapsam (3 dakika)

- **Hedef kullanıcı:** Bireysel yatırımcı, finans öğrencisi, sistem yöneticisi
- **Temel senaryolar:** Piyasa takibi, sanal portföy yönetimi, teknik analiz, alarm/bildirim
- **MVP kapsamı:** 14 tamamlanmış özellik (README.md §1.2)
- **Teknoloji kısa özet:** Java 17 + Spring Boot 3.4 backend, React 18 + TypeScript frontend, 15 Docker konteyner

---

## 2. Mimari Görünüm (5 dakika)

**Dosya:** [ARCHITECTURE.md](./ARCHITECTURE.md)

- C4 Container diyagramı üzerinden servis topolojisi
- 3 katman: Uygulama (Backend, Frontend, Nginx) → Veri (PostgreSQL, Redis, Kafka) → Gözlemlenebilirlik (Prometheus, Grafana, OpenSearch, OTEL)
- Servis sorumlulukları tablosu
- Neden bu mimari? → *Tek giriş noktası, güvenlik, gözlemlenebilirlik, modülerlik*

**Konuşma notu:** "Nginx'i tek giriş noktası yaparak API ve WebSocket trafiğini merkezi yönettik. Modüler monolith ile domainleri ayrı tutarak operasyonel karmaşıklığı düşük tuttuk."

---

## 3. Tasarım ve Modelleme (7 dakika)

**Dosya:** [TASARIM_MIMARISI_VE_MODELLEME.md](./TASARIM_MIMARISI_VE_MODELLEME.md)

- **ER Model:** User → Portfolio → PortfolioItem → Instrument → PriceHistory ilişkisi
- **Emir yaşam döngüsü:** PENDING → FILLED/CANCELED/REJECTED state diyagramı
- **Piyasa verisi akışı:** Scheduler → ProviderResolver → Normalize → DB → Cache → WebSocket
- **Veri kaynağı matrisi:** TCMB, Yahoo, Alpha Vantage, Finnhub, Simülasyon
- **Migrasyon geçmişi:** V1-V18 şema evrimi

**Konuşma notu:** "Çekirdek model User–Portfolio–Instrument–Transaction ilişkisi üzerinde kurulu. 18 Flyway migrasyonuyla şemayı kontrollü evirdik. Provider bazlı veri modelinde fallback ve tercih kuralları netleştirildi."

---

## 4. Kod–Doküman Uyum Gösterimi (8 dakika)

Canlı gezinti sırası:

| # | Dosya/Dizin | Gösterilecek | Süre |
|---|---|---|---|
| 1 | `backend/src/.../service/MarketDataService.java` | Veri toplama akışı, provider dispatch | 2 dk |
| 2 | `backend/src/.../service/portfolio/` | PortfolioFinancialRulesService + OrderExecutionService | 2 dk |
| 3 | `backend/src/.../scheduler/MarketDataScheduler.java` | Zamanlanmış veri güncelleme | 1 dk |
| 4 | `backend/src/.../config/SecurityConfig.java` | OAuth2/JWT yapılandırması | 1 dk |
| 5 | `frontend/src/store/api/` | RTK Query API slice'ları | 1 dk |
| 6 | `docker-compose.yml` | 15 servis topolojisi | 1 dk |

**Konuşma notu:** "Tasarım dokümanlarındaki her kavramın kodda karşılığını gösteriyoruz. MarketDataService tasarım dokümanındaki veri akışı diyagramını birebir yansıtıyor."

---

## 5. Mimari Kararlar (ADR) (3 dakika)

**Dosya:** [ADR.md](./ADR.md)

- 10 temel mimari karar (ADR-001 — ADR-010)
- Her karar için: alternatifler, neden bu seçildi, sonuç
- Öne çıkan: Neden mikroservis değil? → Modüler monolith bilinçli tercih
- Simülasyon motoru: Borsa kapalıyken bile demo/test yapabilme

---

## 6. Güvenlik ve Operasyon (4 dakika)

**Dosyalar:** [SECURITY.md](./SECURITY.md) + [DEPLOYMENT.md](./DEPLOYMENT.md)

- 5 katmanlı güvenlik mimarisi (ağ → kimlik → yetki → uygulama → veri)
- Secret yönetimi: 12 farklı secret, üretim kontrol listesi
- 4 Docker Compose profili (full, light, secure, prod)
- Backup/restore mekanizması (Bash + PowerShell)
- Gözlemlenebilirlik: Prometheus + Grafana + OpenSearch

---

## 7. Test Altyapısı (2 dakika)

- **Backend:** 40 test sınıfı (unit + integration), JaCoCo %50/%40 minimum kapsamı
- **Frontend:** Vitest unit testleri, Playwright E2E testleri (9 test dosyası)
- **Testcontainers:** Entegrasyon testlerinde gerçek PostgreSQL
- **MSW:** Frontend API mock'ları

---

## 8. Kısa Demo Senaryosu (8 dakika)

| # | Senaryo | Süre |
|---|---|---|
| 1 | Login → Keycloak auth → Dashboard | 1 dk |
| 2 | Döviz kurları ekranı (TCMB verisi) | 1 dk |
| 3 | Hisse senetleri → Hisse detay (fiyat grafiği) | 1 dk |
| 4 | Portföy oluştur → Simülasyon alım emri | 2 dk |
| 5 | İzleme listesi + Fiyat alarmı oluştur | 1 dk |
| 6 | Grafana paneli → Metrikler | 1 dk |
| 7 | Admin dashboard | 1 dk |

---

## 9. Kapanış ve Sonraki Adımlar (2 dakika)

- **Tamamlananlar:** 14 MVP özelliği, tam mimari, 15 servisli altyapı
- **Teknik borçlar:** TypeScript strict mode, büyük servislerin bölünmesi, frontend test kapsamı
- **Sonraki sprint:** Strict typing, servis ayrıştırma, mobil uyumluluk

---

## Süre Özeti

| Bölüm | Süre |
|---|---|
| Sunum | ~44 dakika |
| Soru–cevap tampon | ~15+ dakika |
| **Toplam** | **~60 dakika** |

---

## Toplantı Öncesi Kontrol Listesi

- [ ] Docker servisleri ayakta? (`docker compose ps`)
- [ ] Backend sağlıklı? (`curl http://localhost:8088/actuator/health`)
- [ ] Frontend login çalışıyor?
- [ ] Demo verisi ve test kullanıcısı hazır?
- [ ] İnternet ve ekran paylaşım testi yapıldı?
- [ ] Açılacak dosya sekmeleri önceden hazır?
- [ ] Grafana dashboard'u açık ve verili?
- [ ] Swagger UI erişilebilir?
