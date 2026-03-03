# Toplanti 2 Konusma Notlari

Bu dokuman, toplantida akici ve guvenli konusman icin hazirlandi.

## 1) 60 Saniyelik Acilis Metni

"Merhaba, bugun Finans Portalinin mimari ve tasarim kararlarini, modelleme yaklasimimizi ve kod-dokuman uyumunu gosterecegiz. Ozellikle veri akisinda guvenlik, performans ve izlenebilirlik basliklarina odaklandik. Sunum sonunda canli olarak ilgili kod bolumlerini de acip tasarim kararlarinin uygulamadaki karsiligini gosterecegiz."

## 2) Mimaride Vurgu Cumleleri

- "Nginx'i tek giris noktasi yaparak API ve WebSocket trafigini merkezi yonettik."
- "Backend tarafinda moduler monolith yaklasimiyla domainleri ayri tuttuk."
- "Redis + scheduler + websocket kombinasyonuyla hem guncellik hem performans dengesini sagladik."
- "Kafka ve OpenSearch ile log akisinda asenkron ve olceklenebilir bir yapi kurduk."

## 3) Modellemede Vurgu Cumleleri

- "Cekirdek model User-Portfolio-Instrument-Transaction iliskisi uzerinde kurulu."
- "Emir yasam dongusu net state gecisleriyle tanimli; bu da testlenebilirligi artiriyor."
- "Provider bazli market veri modelinde fallback ve kaynak tercihi kurallari acikca modelde yer aliyor."

## 4) Muhtemel Zor Sorular ve Kisa Cevaplar

### Soru: Neden mikroservis degil?

Cevap: "Ihtiyac duyulan domain ayrimini kod seviyesinde koruyarak operasyonel karmasikligi dusuk tuttuk. Trafik ve ekip olcegi buyudukce domain bazli ayrisma icin tasarim uygun."

### Soru: BIST100 neden TCMB'den gelmiyor?

Cevap: "TCMB veri kaynagi kur/forex icin kullaniliyor. BIST100 index verisi provider/simulasyon/veritabani akisindan geliyor; bu beklenen davranis."

### Soru: Guvenlikte en kritik onlemler neler?

Cevap: "OAuth2/JWT, rol bazli yetki, secret yonetimi, OpenSearch security, TLS ve least-privilege ag erisimi."

### Soru: Performans darbogazi olursa ne yapacaksiniz?

Cevap: "Cache hit oranini artiracagiz, agir sorgulari optimize edecegiz, scheduler frekanslarini data source limitlerine gore ayarlayacagiz, gerektiğinde backend'i yatay olcekleyecegiz."

## 5) Canli Kod Gosterim Sirasi (Pratik)

1. `docs/ARCHITECTURE.md` (1 dk)
2. `docs/TASARIM_MIMARISI_VE_MODELLEME.md` (2 dk)
3. `backend/src/main/java/com/mintstack/finance/service/MarketDataService.java` (2 dk)
4. `backend/src/main/java/com/mintstack/finance/service/portfolio/` (2 dk)
5. `frontend/src/store/api/` (1 dk)
6. `docker/logstash/pipeline/logstash.conf` (1 dk)

## 6) Kapanis Metni

"Tasarim dokumanlari ile kodu hizali sekilde ilerlettik. Toplanti geri bildirimlerine gore teknik borc listemizi onceliklendirip bir sonraki iterasyonda servis parcalama, strict typing ve test kapsamini daha da guclendirecegiz."
