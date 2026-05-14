# MintStack Finance Portal
## Teknik Analiz, Mimari ve Modelleme Dokumani

## Belge Bilgileri

| Belge Alani | Deger |
|---|---|
| Surum | 1.1 |
| Tarih | 2026-05-14 |
| Dokuman Turu | Teknik Analiz, Mimari ve Modelleme |
| Kaynak Template | `teknik_analiz_pdf_turkce.md` |

## 1) Yonetici Ozeti

MintStack Finance Portal; portfoy yonetimi, RSS haber akisi, TEFAS fon verisi, teknik analiz, alarm ve yonetim modullerini tek bir platformda birlestirir. Bu surumde kalan 4 kritik bosluk kapatilmistir:

- RSS haberleri icin LLM enrichment akisi canli zincire baglandi.
- Fintables provider resmi olarak policy lock ile pasif hale getirildi.
- Template basliklariyla birebir doldurulmus teknik dokuman olusturuldu.
- Alert webhook icin production hardening (IP allowlist + HMAC signature) uygulandi.

## 2) Kapsam

- Backend: Scheduler, enrichment, webhook security, provider policy kontrolleri.
- DB: `news` tablosuna LLM metadata alanlari.
- Dokumantasyon: Template uyumlu teknik belge ve analiz raporu guncellemesi.
- CI/Test: Unit test kapsam genislemesi.

## 3) Terminoloji ve Standartlar

- LLM Enrichment: RSS haberinden ozet/sentiment/keyword uretimi.
- Policy Lock: Provider acik/kapali kararinin kod seviyesinde zorunlu uygulanmasi.
- Webhook HMAC: Payload butunlugunu imza ile dogrulama.
- IP Allowlist: Webhook kaynaklarini CIDR tabanli kisitlama.

## 4) Gereksinimler

- FR-L1: Haber scheduler, kaydedilmeden once LLM enrichment uygulayabilmeli.
- FR-F1: Fintables provider kapaliysa aktiflestirme denemesi reddedilmeli.
- FR-A1: Webhook payload imzasi dogrulanabilmeli.
- FR-A2: Webhook kaynagi allowlist disindaysa istek engellenmeli.
- FR-D1: Teknik dokuman template baslik yapisina birebir uymali.

## 5) Sistem Tasarimi

- `NewsScheduler -> NewsEnrichmentService -> NewsRepository` zinciri eklendi.
- `AlertWebhookController -> AlertWebhookSecurityService` guvenlik kapisi eklendi.
- `SettingsService` ve `DataSourceService` icinde Fintables policy guard uygulandi.
- `ApiKeyValidationService.validateFintables` policy lock durumunu dogrudan uygular.

## 6) Veri Akisi ve Is Kurallari

### 6.1 RSS + LLM Akisi

1. RSS feed okunur.
2. Haber benzersizlik kontrolu gecerse enrichment denenir.
3. LLM cevabindan `summary/sentiment/keywords` cikartilir.
4. `news.llm_*` alanlari doldurulur, kayit DB'ye yazilir.

### 6.2 Fintables Policy

- `APP_EXTERNAL_API_FINTABLES_ENABLED=false` ise:
  - API key aktivasyonu reddedilir.
  - Data source preference reddedilir.
  - Trigger fetch reddedilir.
  - Capability metadata `policy_disabled` durumunu doner.

### 6.3 Alert Webhook Guvenlik Akisi

1. Kaynak IP (`X-Forwarded-For` veya remote addr) alınır.
2. `X-Forwarded-For` yalnizca `APP_ALERT_WEBHOOK_TRUST_FORWARDED_FOR=true` ve istek `APP_ALERT_WEBHOOK_TRUSTED_PROXY_CIDRS` icinden geliyorsa kullanilir.
3. Allowlist tanimliysa CIDR eslesmesi zorunlu tutulur.
4. Imza zorunluysa HMAC SHA-256 dogrulanir.
5. Dogrulama basarisizsa `403` donulur.

## 7) Guvenlik ve Yetkilendirme

- OAuth2/JWT + RBAC aynen korunur.
- Webhook icin ek katman:
  - IP allowlist (opsiyonel)
  - HMAC imza dogrulama (opsiyonel/zorunlu mod)
- Hata durumlari `AccessDeniedException` ile merkezi handler tarafinda `403` olur.

## 8) Test ve Dogrulama

Eklenen/dokunulan testler:

- `AlertWebhookSecurityServiceTest`
- `NewsSchedulerTest` (enrichment bagimliligi ile)
- `DataSourceServiceTest` (Fintables policy disabled senaryolari)
- `SettingsServiceTest` (Fintables aktivasyon reddi)

Canli dogrulamalar:

- `/swagger-ui.html`, `/api-docs`, `/v3/api-docs` -> 200
- `/api/v1/glossary` -> 200

## 9) Riskler ve Kisitlamalar

- LLM endpoint kontrati OpenAI-compatible varsayimiyla tasarlandi; farkli saglayicilarda endpoint/path/header uyari gerekebilir.
- Webhook signature zorunlulugu acilmadan sadece allowlist ile calismak daha dusuk guvenlik seviyesidir.
- Fintables policy acildiginda rate-limit/lisans kotalari tekrar gozden gecirilmelidir.

## 10) Referanslar

- `backend/src/main/java/com/mintstack/finance/service/NewsEnrichmentService.java`
- `backend/src/main/java/com/mintstack/finance/service/AlertWebhookSecurityService.java`
- `backend/src/main/resources/db/migration/V21__news_llm_enrichment.sql`
- `docs/MintStack_Finance_Portal_Analiz_Raporu.md`

## 11) Ekler

- ER diyagrami: `docs/diagrams/er-diagram.mmd`
- Admin moduller: `docs/diagrams/admin-modules-flow.mmd`
- Alert sequence: `docs/diagrams/alert-sequence.mmd`

