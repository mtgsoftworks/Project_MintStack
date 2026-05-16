# BIST DataStore Entegrasyonu

## Amac

Tahvil/bono ve VIOP ekranlarinda sahte veri gostermeden Borsa Istanbul public dosya/bulten kaynaklarini kullanmak.

## Kaynaklar

| Veri | Dosya | Not |
|---|---|---|
| Tahvil & Bono | `/data/ttb/{yyyy}/{MM}/ttb{yyyyMMdd}3.zip` | Gunluk Borclanma Araclari Piyasasi bulteni, CSV |
| VIOP | `/data/vadeli/viop_{yyyyMMdd}.csv` | Gunluk VIOP bulteni, CSV |
| Data/File Paths | `/files/datafilepaths_viop.zip` ve `/files/DataFilePaths.zip` | BIST dosya yolu referansi |

## Runtime Ayarlari

```env
APP_EXTERNAL_API_BIST_DATASTORE_ENABLED=true
APP_EXTERNAL_API_BIST_DATASTORE_BASE_URL=https://www.borsaistanbul.com
APP_EXTERNAL_API_BIST_DATASTORE_TIMEOUT=30000
APP_EXTERNAL_API_BIST_DATASTORE_LATEST_LOOKBACK_DAYS=7
APP_EXTERNAL_API_BIST_DATASTORE_BOND_DAILY_TEMPLATE=/data/ttb/{yyyy}/{MM}/ttb{yyyyMMdd}3.zip
APP_EXTERNAL_API_BIST_DATASTORE_VIOP_DAILY_TEMPLATE=/data/vadeli/viop_{yyyyMMdd}.csv
```

## Uygulama Akisi

- `BistDataStoreClient` dosyayi indirir, ZIP/CSV icerigini parse eder.
- `BistDataStoreMarketDataService` `BOND` ve `VIOP` enstrumanlarini upsert eder.
- Fiyat, onceki fiyat ve hacim `instruments` ve `price_history` tablolarina yazilir.
- Scheduler `fetchBondPrices` ve `fetchViopPrices` icinde BIST DataStore adapterini calistirir.
- Admin backfill ekrani secilen tarih araliginda BIST dosyalarini tekrar okuyabilir.

## Sinirlar

- Bu entegrasyon 7/24 anlik veri akisi degildir; BIST yayinlanan gunluk dosyalari okunur.
- Dosya formati Borsa Istanbul tarafinda degisirse parser guncellenmelidir.
- Sentetik fallback yoktur; dosya yoksa ilgili gunde veri atlanir.
