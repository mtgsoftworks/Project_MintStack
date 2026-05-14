# TEFAS API Secimi ve Entegrasyon Karari

## Ozet Karar
- `@firstthumb/tefas-api` ve `ahmethakanbesel/finance-api` dogrudan projeye baglanmadi.
- MintStack icinde native Java istemci ile TEFAS entegrasyonu kullaniliyor.
- Temel sebep: proje backend'i Spring Boot (Java), dis paketler Node/Go tabanli ve operasyon/lisans riskleri var.

## Resmi API Durumu
- TEFAS SSS metninde `API paylasimi yapilmamaktadir` ifadesi yer aliyor.
- Bu nedenle entegrasyon, TEFAS web tarafinda acik gorunen servis kontrati uzerinden adapter mantigiyla yapilmistir.

## Degerlendirilen Adaylar
1. `@firstthumb/tefas-api`
   - Artisi: Hizli PoC icin kolay Node istemcisi.
   - Eksisi: Projemiz Java backend; ek runtime ve bakim maliyeti olusturur.
2. `ahmethakanbesel/finance-api`
   - Artisi: Hazir bir REST katmani.
   - Eksisi: Go servis olarak ayrik deploy gerekir ve lisans `GPL-3.0`; kurumsal dagitimda uyum incelemesi ister.

## Uygulanan Entegrasyon
- TEFAS istemcisi: `backend/src/main/java/com/mintstack/finance/service/external/TefasFundClient.java`
- Fon verisi isleme: `backend/src/main/java/com/mintstack/finance/service/market/TefasFundDataService.java`
- Scheduler baglantisi: `backend/src/main/java/com/mintstack/finance/scheduler/MarketDataScheduler.java`
- Config:
  - `APP_EXTERNAL_API_TEFAS_ENABLED`
  - `APP_EXTERNAL_API_TEFAS_TIMEOUT`
  - `APP_EXTERNAL_API_TEFAS_FUND_LIST_ENDPOINT`
  - `APP_EXTERNAL_API_TEFAS_DEFAULT_FUND_KINDS`
  - `APP_EXTERNAL_API_TEFAS_FUND_CODES`

## Bu Turde Eklenen Iyilestirmeler
- TEFAS endpoint path'i config'e acildi (`fund-list-endpoint`).
- Istemci hata toleransi artirildi:
  - Tarih fallback (bugun -> onceki gunler) exception durumunda da devam eder.
  - Kind/code bazli hatada tum akisi kesmek yerine parcali devam eder.
- Frontend'de TEFAS provider UI'dan secilebilir hale getirildi:
  - API key ekrani: TEFAS public/no-key secenegi.
  - Data source ekrani: `FUNDS` veri turu ve `TEFAS` provider gorunur.

## Kaynaklar
- TEFAS: https://www.tefas.gov.tr/
- TEFAS web/SSS: https://tefasweb.ingetechnology.com/tr/fon-getirileri
- NPM paket: https://www.npmjs.com/package/@firstthumb/tefas-api
- finance-api repo: https://github.com/ahmethakanbesel/finance-api
- Go package metadata (lisans/yayin): https://pkg.go.dev/github.com/ahmethakanbesel/finance-api/cmd/finance-api
