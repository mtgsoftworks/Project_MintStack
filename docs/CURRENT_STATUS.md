# MintStack Current Status

Son guncelleme: 2026-05-23

Bu dokuman README ve teknik dokumanlar icin kisa, guncel durum referansidir.

## Kapsam

MintStack Finance Portal web tabanli bir finans portali olarak konumlanir. Mobil uygulama bu surumun kapsami disindadir.

IT Servis - Ticket Yonetimi ve jBPM bu repo kapsaminda degildir. 7 Haziran tesliminde bu madde finans portali icin kapsam disi olarak sunulmalidir.

Aktif ana moduller:

- Dashboard ve piyasa ozeti.
- Haberler: RSS kaynaklari, kategori filtreleme ve opsiyonel LLM enrichment.
- Piyasalar: BIST, doviz, fon, tahvil/bono ve VIOP gorunumleri.
- Portfoy: sanal portfoy, nakit, pozisyon, alim/satim ve islem gecmisi.
- Izleme listesi: liste olusturma ve enstruman ekleme.
- Fiyat alarmlari: sembol secimi, hedef fiyat/yuzde alarmi, tetiklenme kaydi.
- Bildirimler: uygulama ici bildirim kaydi ve okundu yonetimi.
- Analiz: teknik indikatorler, backtesting, Monte Carlo ve karsilastirma ekranlari.
- Ayarlar: ADMIN icin API anahtarlari/backfill, tum kullanicilar icin veri kaynagi tercihi ve profil tercihleri.
- Admin: sistem yonetimi, rate limit, cache/market data bakimi ve operasyonel aksiyonlar.

## Veri Kaynaklari

| Kaynak | Durum | API key |
|---|---|---|
| TCMB | Aktif, resmi doviz kuru kaynagi | Gerekmez |
| TEFAS | Aktif, fon verisi icin native Java adapter | Gerekmez |
| Yahoo Finance | Aktif, public/keyless market data fallback | Gerekmez |
| BIST DataStore | Aktif, tahvil/bono ve VIOP public bulten dosyalari | Gerekmez |
| Alpha Vantage | Aktiflenebilir | Gerekir |
| Finnhub | Aktiflenebilir | Gerekir |
| Fintables | Policy lock ile pasif | Varsayilan kapali |
| RSS | Aktif | Gerekmez |
| LLM enrichment | OpenAI-compatible endpoint ile aktiflenebilir | Saglayici token'i gerekir |

Notlar:

- TEFAS icin resmi public API paylasimi olmadigi kabul edilir; sistem TEFAS web tarafinda gorunen servis kontratini adapter mantigi ile kullanir.
- Yahoo Finance keyless calisir; kullanicidan API key/base URL istenmemelidir.
- BIST DataStore keyless calisir; tahvil/bono icin `ttbYYYYAAGG3.zip`, VIOP icin `viop_YYYYAAGG.csv` dosyalari okunur.
- Alpha Vantage, Finnhub ve LLM enrichment anahtarlari sadece ADMIN tarafindan yonetilir; test/normal kullanici kendi API key'ini ekleyemez.
- Fintables resmi olarak pasif kabul edilir. `APP_EXTERNAL_API_FINTABLES_ENABLED=false` guvenli varsayilandir.
- GitHub Models kullanimi icin LLM endpoint `https://models.github.ai/inference`, model ornegi `openai/gpt-5`, token ise `GITHUB_TOKEN` veya UI uzerinden girilen provider key'i olabilir.

## Kripto Durumu

Kripto para destegi kullanici arayuzu ve aktif market akisi kapsamindan cikarilmistir. Eski migration/seed kalintilari varsa bunlar geriye donuk uyumluluk icindir; yeni demo ve juri anlatiminda kripto modul olarak sunulmamalidir.

## Gecmis Veri Backfill

Admin/Ayarlar uzerinden gecmis veri backfill akisi eklenmistir.

Desteklenen kullanim:

- Periyot: 7 gun, 30 gun, 90 gun, 1 yil.
- Enstruman tipi: hisse, fon, doviz, tahvil/bono, VIOP.
- Kaynak: secili data source policy ve provider yeteneklerine gore.
- Hedef tablo: `price_history`.

Backfill idempotent olacak sekilde tasarlanir. Var olan veri veya optimistic lock/data conflict durumlari toplu islemi tamamen bozmadan skip/retry davranisi ile ele alinir.

## Alarm ve Bildirim Davranisi

Kullanici fiyat alarmi ile Prometheus Alertmanager ayni sey degildir.

Kullanici fiyat alarmi:

- Kullanici `/api/v1/alerts` uzerinden alarm olusturur.
- Fiyat guncellemesi geldikce backend alarm kosulunu kontrol eder.
- Tetiklenen alarm `isTriggered=true`, `isActive=false` olur.
- Uygulama ici bildirim Kafka/WebSocket hattina yayilir ve DB'ye kaydedilir.
- E-posta yalnizca SMTP ayarlari aktifse gonderilir.
- Sesli alarm ve browser push bu surumde tam bagli degildir.

Alertmanager:

- Prometheus sistem metriklerinden alarm uretir.
- Backend down, 5xx hata orani, latency, JVM heap, DB pool ve external API failure gibi operasyonel durumlari izler.
- Webhook ve SMTP receiver ile operasyon ekibine bildirim gonderebilir.
- Kullanici fiyat alarmi yerine sistem sagligi alarmi olarak anlatilmalidir.

## CI/CD

GitHub Actions guncel CI akisi:

- Backend: `./mvnw -B -ntp clean verify`.
- Backend runtime/CI hedefi: Java 21.
- Backend coverage: JaCoCo line minimum `0.50`, branch minimum `0.35`.
- Flyway: bos CI PostgreSQL uzerinde once `migrate`, sonra `validate`.
- Frontend: lint, typecheck, Vitest ve build.
- Docker: compose config validation otomatik, image build manuel workflow ile.

Flyway notu:

- Bos DB'de tek basina `flyway:validate` fail eder, cunku migration'lar pending gorunur.
- CI bu nedenle `migrate -> validate` sirasini kullanir.

## Dokuman Haritasi

- `README.md`: proje tanimi, hizli baslangic, servisler ve gelistirme komutlari.
- `docs/TEKNIK_DOKUMANTASYON.md`: teknik kapsam, moduller, kaynaklar, scheduler ve CI/CD.
- `docs/DELIVERY_CHECKLIST.md`: 7 Haziran teslim isterleri ve kanit dosyalari.
- `docs/MintStack_Finance_Portal_Sunum.pptx`: PDF olmayan teslim sunumu.
- `docs/ARCHITECTURE.md`: mimari, servis sorumluluklari ve veri modeli.
- `docs/DEPLOYMENT.md`: Docker, ortam degiskenleri, CI/CD ve monitoring.
- `docs/OPERATIONS.md`: isletim, secret, alerting, backfill ve sorun giderme.
- `docs/BORSA_TERIM_SOZLUGU_TEFAS_KAYNAKLI.md`: kullaniciya acik olmayan, ic kullanim borsa/fon kavram sozlugu.
- `docs/TEFAS_API_SECIMI_VE_ENTEGRASYON.md`: TEFAS entegrasyon karari.
- `docs/BIST_DATASTORE_ENTEGRASYONU.md`: BIST tahvil/bono ve VIOP dosya entegrasyonu.
