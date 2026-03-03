# Toplanti 2 Sunum Akisi (4 Mart, 15:30)

Bu dokuman, izleme toplantisinda ekran paylasirken izlenecek net sunum sirasini verir.

## 0. Acilis (1 dakika)

- Projenin hedefi: finans verisini guvenli ve gercek zamanli sekilde sunmak.
- Bu toplantida gostereceklerimiz: mimari, modelleme, kod uyumu, canli demo.

## 1. Problem ve Kapsam (2 dakika)

- Hedef kullanici ve temel senaryolar.
- MVP kapsami ve bugune kadar tamamlanan basliklar.

## 2. Mimari Gorunum (4 dakika)

- [ARCHITECTURE.md](./ARCHITECTURE.md) uzerinden C4 container akisi.
- Servis sorumluluklari ve veri akisinin neden bu sekilde kurgulandigi.

## 3. Tasarim ve Modelleme (5 dakika)

- [TASARIM_MIMARISI_VE_MODELLEME.md](./TASARIM_MIMARISI_VE_MODELLEME.md)
- ER model: User-Portfolio-Instrument-Transaction iliskisi.
- Emir yasam dongusu ve BIST100 veri akis sequence'i.

## 4. Kod - Dokuman Uyum Gosterimi (5 dakika)

Onerilen canli gezinti:

1. `backend/src/main/java/com/mintstack/finance/service/MarketDataService.java`
2. `backend/src/main/java/com/mintstack/finance/service/portfolio/`
3. `frontend/src/store/api/`
4. `docker-compose.yml` ve `docker/logstash/pipeline/logstash.conf`

## 5. Operasyon ve Guvenlik (3 dakika)

- [DEPLOYMENT.md](./DEPLOYMENT.md): Dev/prod, profil bazli calisma.
- [SECURITY.md](./SECURITY.md): secret yonetimi, kontrol listesi.

## 6. Kisa Demo Senaryosu (5 dakika)

- Login -> Dashboard
- BIST/Market verisi kontrolu
- Portfoyde alim/satim simulasyonu
- Bildirim veya alert goruntuleme
- Grafana panel acilisi

## 7. Kapanis ve Sonraki Adimlar (2 dakika)

- Bugun tamamlananlar
- Kalan teknik borclar
- Sonraki sprint odagi

## Toplam Sure

- Planli: 27 dakika
- Soru-cevap icin tampon: 15+ dakika

## Toplanti Oncesi Kontrol Listesi

- Docker servisleri ayakta mi? (`docker compose ps`)
- Internet ve ekran paylasim testi yapildi mi?
- Acilacak dosya sekmeleri onceden hazir mi?
- Demo datasi ve test kullanicisi hazir mi?
