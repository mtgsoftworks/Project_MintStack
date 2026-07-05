# 🏆 MintStack Finance Portal — 25 Dakikalık Stratejik Sunum ve Canlı Demo Yönergesi

> **Stratejik Amaç:** 30 dakikalık final sunum süresinin **24-25 dakikasını** büyüleyici, kesintisiz ve yüksek teknik yoğunluklu slayt + canlı uygulama gösterimi ile doldurarak jüriye soru sorma zamanı (max 3-5 dk) bırakmamak; olası tüm teknik soruları anlatım ve demo sırasında "laf arasında" imha etmektir.

---

## ⏱️ 1. Zamanlama Matrisi (30 Dakika İdeal Kurgu)

```
[00:00 - 08:30] ──▶ Slayt Sunumu (8.5 Dk)      : Mimari, Güvenlik, Java 21, Log Pipeline
[08:30 - 24:30] ──▶ Canlı Demo (16.0 Dk)      : Dashboard, Portföy, FIFO, Analiz, Admin
[24:30 - 25:30] ──▶ Sonuç & Kapanış (1.0 Dk)   : %100 Karşılama & Teşekkür
[25:30 - 30:00] ──▶ Kalan Süre (4.5 Dk)       : Jüri Soru-Cevap (Tüm Sorular Zaten Yanıtlandı!)
```

---

## 📊 BÖLÜM 1: Etkileyici Slayt Sunumu (00:00 - 08:30 | 8.5 Dakika)

### 🟢 Slayt 1-3: Giriş, Problem ve Mimari Seçim (00:00 - 02:00)
- **Ne Gösterilecek:** Proje Başlığı, Kapsam (BIST, TEFAS, TCMB, VİOP) ve Mimari Diyagramı.
- **Konuşma Metni:** 
  > *"Hoş geldiniz. MintStack Finance Portal; Türkiye finans piyasalarına özel BIST Hisseleri, TEFAS Fonları, TCMB Kurları ve VİOP vadeli işlemlerini anlık izleyen, sanal portföy ve gelişmiş analizler sunan kurumsal bir platformdur.*
  > *Mimaride **Modüler Monolith** tercih ettik. Mikroservislerin getirdiği ağ gecikmesi ve Saga/2PC gibi dağıtık işlem karmaşıklıklarından kaçınarak tek bir Spring Boot 3.4.2 çatısı altında yüksek performans elde ettik. Modüllerimiz katı paket izolasyonuna sahiptir, istenildiği an mikroservise dönüştürülebilir."*
- 🎯 **Önlenen Jüri Sorusu:** *"Neden mikroservis yapmadınız?"* -> Zaten cevaplandı!

---

### 🟢 Slayt 4-6: Java 21, Veri Altyapısı ve Log Boru Hattı (02:00 - 04:30)
- **Ne Gösterilecek:** Java 21 Virtual Threads, PostgreSQL, Redis, Kafka -> Logstash -> OpenSearch akışı.
- **Konuşma Metni:**
  > *"Backend tarafında **Java 21 Virtual Threads (Project Loom)** kullanarak geleneksel OS thread maliyetlerini sıfırladık ve yüz binlerce eşzamanlı HTTP/WebSocket isteğini işleyebilir hale geldik.*
  > *Veritabanında ACID garantisi için **PostgreSQL 15** ve 29+ **Flyway** migrasyonu kullandık. Önbellekleme ve **Bucket4j** ile kullanıcı bazlı dağıtık Rate Limiting için **Redis 7** entegre ettik.*
  > *Log mimarimizde **Log4j2 AsyncAppender** ile loglar arka planda **Apache Kafka (KRaft)** otobüsüne iletilir, **Logstash** üzerinden **OpenSearch**'e indekslenir. Bu olay güdümlü yapı sayesinde yüksek log trafiği ana sistemi 1 milisaniye dahi yavaşlatmaz."*
- 🎯 **Önlenen Jüri Sorusu:** *"Loglar sistemi yavaşlatıyor mu / Şema değişikliklerini nasıl yönetiyorsunuz?"* -> Zaten cevaplandı!

---

### 🟢 Slayt 7-8: Güvenlik, Keycloak 26 ve 2FA (04:30 - 06:30)
- **Ne Gösterilecek:** Keycloak 26, OAuth2/OIDC, JWT Payload, TOTP 2FA ve RBAC.
- **Konuşma Metni:**
  > *"Güvenlik tarafında kimlik yönetimini **Keycloak 26.5.4** IdP sunucusuna devrettik. İstemci PKCE akışı ile giriş yapıp RS256 imzalı JWT alır. Backend Spring Security `OAuth2 Resource Server` olarak JWKS üzerinden bu token'ı doğrular.*
  > *Portföy ve admin eylemleri için Keycloak üzerinde **TOTP (Google Authenticator)** tabanlı 2FA politikasını zorunlu kıldık. Rol bazlı erişim denetimi (RBAC) ise `@PreAuthorize` anotasyonları ile metod seviyesinde korunmaktadır."*
- 🎯 **Önlenen Jüri Sorusu:** *"Güvenliği ve 2FA'yi nasıl sağladınız?"* -> Zaten cevaplandı!

---

### 🟢 Slayt 9-12: Piyasa Araçları, FIFO Emir Motoru & Kapanış (06:30 - 08:30)
- **Ne Gösterilecek:** WebSocket/STOMP akışı, FIFO Emir Motoru, Prometheus/Grafana/OTEL Tracing ve %100 İster Karşılama.
- **Konuşma Metni:**
  > *"Sistemde BIST, TEFAS ve VİOP için **WebSocket/STOMP** ile anlık fiyat akışı sunulmaktadır. Portföy motorumuz **FIFO (First In First Out)** lot tüketimi ile çalışır ve anlık Gerçekleşen (Realized) ve Gerçekleşmeyen (Unrealized) PnL hesaplar.*
  > *Gözlemlenebilirlikte **Prometheus, Grafana ve OpenTelemetry (OTEL)** ile `trace_id` takibi yapıyoruz. Şimdi izninizle bu altyapıyı canlı uygulama üzerinde göstereyim."*

---

## 💻 BÖLÜM 2: Canlı Uygulama Gösterim Demosu (08:30 - 24:30 | 16.0 Dakika)

### 🖥️ Ekran 1: Dashboard ve Canlı Piyasa Akışı (08:30 - 11:30 | 3.0 Dk)
- **Aksiyon:** `http://localhost:3000` (veya `:8088`) adresine girin.
- **Gösterilecekler:**
  1. BIST 100 Kartı, Döviz Kurları ve Popüler Hisseler widget'ları.
  2. Sağ üstten Para Birimi değiştirme (`TRY` -> `USD` -> `EUR`).
  3. Fiyatların yeşil/kırmızı yakıp sönmesi (WebSocket STOMP akışı).
- **Vurgulanacak Laf Arası Cümle:** 
  > *"Gördüğünüz gibi piyasa kotasyonları işlem gördüğü yerel para biriminde (TRY) gelirken, üst panelden seçtiğim USD/EUR tercihiyle tüm portföy ve nakit değerleri anında çapraz kurla çevrilmektedir. Arka planda WebSocket ve Redis pub/sub bağlantısı aktiftir."*

---

### 🖥️ Ekran 2: Piyasalar ve Teknik Analiz Detayı (11:30 - 14:30 | 3.0 Dk)
- **Aksiyon:** Sol menüden **Piyasalar -> Hisse Senetleri -> THYAO** detayına tıklayın.
- **Gösterilecekler:**
  1. İnteraktif Fiyat Grafiği (Recharts / Candlestick).
  2. Teknik İndikatör Sekmeleri: **RSI (14)**, **MACD (12/26/9)**, **Bollinger Bands**, **Stochastic**.
  3. Sağ üstteki "İzleme Listesine Ekle" ve "Fiyat Alarmi Kur" butonları.
- **Vurgulanacak Laf Arası Cümle:**
  > *"Teknik analiz motorumuz indikatörleri anlık veri serisi üzerinden hesaplar. Yetersiz veri durumunda sistem çökmez, açıklayıcı sinyal mesajı döner (OVERBOUGHT/NEUTRAL/OVERSOLD). Alarm kurduğumuzda arka planda Kafka üzerinden kullanıcı bildirimi üretilir."*

---

### 🖥️ Ekran 3: Sanal Portföy ve FIFO Emir Motoru Demosu (14:30 - 18:30 | 4.0 Dk)
- **Aksiyon:** Sol menüden **Portföyüm** sayfasına geçin.
- **Gösterilecekler:**
  1. "Yeni Alış Emri" butonuna basın -> `THYAO` seçin -> `10 Lot` alın.
  2. Nakit bakiyesinden düştüğünü ve Pozisyonlara eklendiğini gösterin.
  3. Ardından `5 Lot` satış emri verin.
  4. FIFO kuralına göre gerçekleşen kâr/zararın (Realized PnL) hesaplandığını gösterin.
- **Vurgulanacak Laf Arası Cümle:**
  > *"Emir motorumuz veritabanı seviyesinde `Pessimistic Locking (SELECT FOR UPDATE)` kullanarak bakiye çakışmalarını ve race condition durumlarını %100 engeller. Satış yapıldığında FIFO yöntemiyle en eski lotlar harcanır ve gerçekleşen kâr/zarar anında hesaplanır."*
- 🎯 **Önlenen Jüri Sorusu:** *"Aynı anda iki işlem yapılırsa bakiye bozulur mu?"* -> Zaten cevaplandı!

---

### 🖥️ Ekran 4: Gelişmiş Analiz Araçları (Monte Carlo & Backtest) (18:30 - 21:30 | 3.0 Dk)
- **Aksiyon:** Sol menüden **Analiz & Araçlar** sayfasına geçin.
- **Gösterilecekler:**
  1. **Backtesting:** Hareketli Ortalama Kesişimi (SMA Crossover) stratejisini çalıştırın -> Başarı oranını ve grafik üzerindeki Al/Sat noktalarını gösterin.
  2. **Monte Carlo Simülasyonu:** 1000 iterasyonlu gelecek 30 gün fiyat olasılık dağılım grafiğini gösterin.
  3. **Korelasyon Matrisi:** Hisse ve dövizler arasındaki ilişki matrisini gösterin.
- **Vurgulanacak Laf Arası Cümle:**
  > *"Backtest ve Monte Carlo algoritmalarımız geçmiş fiyat serileri üzerinde istatistiksel olasılık dağılımı hesaplar. Veri setinin yetersiz olduğu durumlarda graceful fallback mekanizması devreye girer."*

---

### 🖥️ Ekran 5: Admin Paneli, Aktif API Koruması ve Veri Sıfırlama (21:30 - 24:30 | 3.0 Dk)
- **Aksiyon:** Admin hesabıyla **Ayarlar -> Veri Kaynakları / API Keys** sekmesine geçin.
- **Gösterilecekler:**
  1. API Key yönetimi (TEFAS, TCMB, BIST DataStore, Yahoo).
  2. **Katı API Koruması Gösterimi:** Bir API kaynağı kapatıldığında UI üzerinde kırmızı uyarı rozeti çıktığını ve "Geçmiş Verileri İndir (Backfill)" butonunun otomatik pasife (`disabled`) geçtiğini gösterin.
  3. **Tüm Verileri Sıfırlama:** "Kullanıcı Verilerini Sıfırla" butonuna basın -> Portföy, pozisyon ve alarmların `@Transactional` atomik silindiğini gösterin.
- **Vurgulanacak Laf Arası Cümle:**
  > *"Sistemimizde veri güvenliği ve bütünlüğü esastır. Aktif bir dış API yoksa sistem uydurma veri üretmez, Backfill butonunu kilitler. Tüm verileri sıfırla dediğimizde ise `@Transactional` cascade silme işlemi çalışarak veritabanında yetim (orphan) kayıt bırakmaz."*
- 🎯 **Önlenen Jüri Sorusu:** *"Dış API kesilirse ne olur / Veriler silinince veritabanı bozulur mu?"* -> Zaten cevaplandı!

---

## 🏁 BÖLÜM 3: Kapanış ve Kalan 5 Dakikanın Yönetimi (24:30 - 30:00)

### 🟢 Kapanış Cümlesi (24:30 - 25:30)
> *"Özetle MintStack Finance Portal; Java 21, Spring Boot 3.4.2, Keycloak 26, Redis, Kafka ve OpenTelemetry altyapısı üzerinde çalışan, %100 teknik isterleri karşılamış, yüksek performanslı ve tam gözlemlenebilir bir kurumsal finans platformudur. Dinlediğiniz için teşekkür ederim. Sorularınız varsa yanıtlamaktan memnuniyet duyarım."*

---

### 🛡️ Jüri Sorularını Laf Arasında İmha Etme Tablosu

| Jüri Sorusu (Gelebilecek Tuzak) | Laf Arasında Sıkıştırılan Cevap Cümlesi | Sunumun Hangi Aşamasında Söylenmeli? |
| :--- | :--- | :--- |
| **"Neden Mikroservis yapmadınız?"** | *"Gereksiz ağ latency'si ve Saga karmaşıklığı yerine modüler monolit tercih ettik; istenildiği an ayrıştırılabilir."* | Slayt 3 (Mimari Slaytı) |
| **"Bakiye çakışmasını (Race condition) nasıl önlediniz?"** | *"Emir motorumuz veritabanı seviyesinde `Pessimistic Locking (SELECT FOR UPDATE)` kullanır."* | Canlı Demo - Ekran 3 (Portföy) |
| **"Dış API kesilirse sistem patlar mı?"** | *"Katı API koruması sayesinde aktif API yoksa sistem uydurma veri basmaz, butonları pasife kilitler."* | Canlı Demo - Ekran 5 (Admin/Ayarlar) |
| **"Loglar sistemi yavaşlatıyor mu?"** | *"Log4j2 AsyncAppender + Kafka sayesinde log yazma işlemi tamamen non-blocking çalışır, 1ms dahi yavaşlatmaz."* | Slayt 6 (Log Boru Hattı) |
| **"2FA ve Rol Güvenliği var mı?"** | *"Keycloak 26 üzerinde TOTP (2FA) zorunluluğu ve Spring Security `@PreAuthorize` rol yetkilendirmesi mevcuttur."* | Slayt 7 (Güvenlik Slaytı) |

---

## 🛠️ BÖLÜM 4: Toplantı Öncesi 5 Dakikalık Hazırlık Check-List

1. [ ] **Docker Stack Çalışıyor mu?**  
   `docker compose ps` atarak 14 servisin de `running` veya `healthy` olduğunu doğrulayın.
2. [ ] **Tarayıcı Tabları Hazır mı?**
   - Tab 1: `http://localhost:3000` (React Frontend - Giriş yapılmış durumda)
   - Tab 2: `http://localhost:8180` (Keycloak Admin Console)
   - Tab 3: `http://localhost:13030` (Grafana Dashboards)
   - Tab 4: `http://localhost:5601` (OpenSearch Dashboards)
   - Tab 5: `http://localhost:8088/swagger-ui.html` (Swagger API Docs)
3. [ ] **Slayt Dosyası Açık mı?**  
   Sunum dosyası tam ekran modunda arkada hazır beklemelidir.
