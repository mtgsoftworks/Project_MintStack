# Toplantı 2 — Konuşma Notları ve Hazırlık Rehberi

Bu doküman, toplantıda akıcı ve güvenli konuşman için hazırlandı. Her bölümde vurgu cümleleri ve muhtemel zor sorulara cevaplar var.

---

## 1. Açılış Metni (60 saniye)

> "Merhaba hocam, bugün MintStack Finans Portalı projesinin mimari ve tasarım kararlarını, modelleme yaklaşımımızı ve kod–doküman uyumunu göstereceğiz. Özellikle veri akışında güvenlik, performans ve izlenebilirlik başlıklarına odaklandık. Sunumun sonunda canlı olarak ilgili kod bölümlerini açıp tasarım kararlarının uygulamadaki karşılığını göstereceğiz."

---

## 2. Proje Tanıtımı İçin Vurgu Cümleleri

- "Türkiye finans piyasalarına yönelik kapsamlı bir platform geliştirdik: döviz, hisse, tahvil, fon ve VİOP verilerini tek noktadan izleyebiliyorsunuz."
- "Sanal portföy yönetimi ile gerçek piyasa koşullarında alım-satım simülasyonu yapılabiliyor."
- "Teknik analiz araçlarımız var: Monte Carlo simülasyonu, backtesting, korelasyon matrisi, RSI ve Moving Average stratejileri."
- "15 Docker konteynerden oluşan tam bir altyapı kurduk: veritabanı, cache, mesaj kuyruğu, kimlik sunucusu ve gözlemlenebilirlik stack'i."

---

## 3. Mimaride Vurgu Cümleleri

- "Nginx'i tek giriş noktası yaparak API ve WebSocket trafiğini merkezi yönettik."
- "Backend tarafında modüler monolith yaklaşımıyla domain'leri ayrı paketlerde tuttuk."
- "Redis + Scheduler + WebSocket kombinasyonuyla hem güncellik hem performans dengesini sağladık."
- "Kafka ve OpenSearch ile log akışında asenkron ve ölçeklenebilir bir yapı kurduk."
- "Resilience4j circuit breaker ile dış API çağrılarında hata toleransı sağlıyoruz."
- "Bucket4j ile rate limiting uygulayarak API kötüye kullanımını engelliyoruz."

---

## 4. Modellemede Vurgu Cümleleri

- "Çekirdek model User–Portfolio–Instrument–Transaction ilişkisi üzerinde kurulu."
- "Emir yaşam döngüsü net state geçişleriyle tanımlı: PENDING, FILLED, PARTIALLY_FILLED, CANCELED, REJECTED. Bu testlenebilirliği artırıyor."
- "3 emir tipi destekliyoruz: MARKET anında çalışır, LIMIT belirlenen fiyatta, STOP belirlenen seviyede tetiklenir."
- "18 Flyway migrasyonuyla veritabanı şemasını kontrollü ve tekrarlanabilir şekilde evirdik."
- "Provider bazlı market veri modelinde fallback ve kaynak tercihi kuralları açıkça modelde yer alıyor."

---

## 5. Güvenlikte Vurgu Cümleleri

- "5 katmanlı güvenlik mimarisi kurduk: ağ, kimlik, yetki, uygulama ve veri katmanı."
- "Keycloak ile OAuth2/OIDC, PKCE S256 akışı, 2FA ve LDAP federation kullanıyoruz."
- "12 farklı secret'ı .env üzerinden yönetiyoruz, hiçbiri repoda yok."
- "İç servis portları 127.0.0.1'e bağlı, dışarıdan erişilemez."

---

## 6. Muhtemel Zor Sorular ve Kısa Cevaplar

### Soru: Neden mikroservis değil de monolith?

> "İhtiyaç duyulan domain ayrımını kod seviyesinde (service alt paketleri) koruyarak operasyonel karmaşıklığı düşük tuttuk. 52'den fazla servis sınıfı var ama tek deployment unit olduğu için DevOps yükü minimal. Trafik ve ekip ölçeği büyüdükçe bounded context bazlı ayrıştırma için tasarım zaten uygun, çünkü her domain kendi service/repository/entity grubunda."

### Soru: BIST100 verisi nereden geliyor?

> "BIST100 endeks verisi Yahoo Finance veya simülasyon motorundan geliyor. TCMB veri kaynağı yalnızca döviz kuru ve resmi kur verileri için kullanılıyor. Bu bilinçli bir tasarım; her sağlayıcının güçlü olduğu veri tipi farklı, biz yetenek matrisiyle otomatik eşleştirme yapıyoruz."

### Soru: Güvenlikte en kritik önlemler neler?

> "Beş katman: (1) Nginx ile tek giriş noktası ve port izolasyonu, (2) Keycloak OAuth2/OIDC ile JWT doğrulama ve PKCE, (3) Spring Security ile rol bazlı yetkilendirme, (4) Bucket4j rate limiting ve Resilience4j circuit breaker, (5) SASL şifreli Kafka, parola korumalı Redis ve OpenSearch security plugin."

### Soru: Performans darboğazı olursa ne yapacaksınız?

> "Üç seviyeli stratejimiz var: (1) Redis cache hit oranını artıracağız — sık sorgulanan veriler zaten cache'te, (2) ağır sorguları veritabanı seviyesinde optimize edeceğiz (index, join optimizasyonu), (3) scheduler frekanslarını data source API limitlerini aşmadan ayarlayacağız. Gerektiğinde backend'i yatay ölçeklendireceğiz."

### Soru: Simülasyon ile gerçek veriyi nasıl ayırıyorsunuz?

> "V11 migrasyonuyla `isSimulated` flag'i ekledik. V18'de gerçek ve simüle enstrüman sembollerinin çakışmasını çözdük. Frontend ve backend'de bu flag ile filtreleme yapılabiliyor. Simülasyon motoru borsa kapalıyken bile çalışarak demo ve test ortamı sağlıyor."

### Soru: Test kapsamınız nedir?

> "Backend'de 40 test sınıfı var: unit testler (Mockito, JUnit 5) ve entegrasyon testleri (Testcontainers ile gerçek PostgreSQL). JaCoCo ile minimum %50 satır ve %40 branch kapsamı hedefliyoruz. Frontend'de Vitest unit testleri ve Playwright E2E testleri mevcut."

### Soru: Monitoring nasıl çalışıyor?

> "Prometheus Spring Actuator metriklerini topluyor, Grafana'da görselleştiriyoruz. Uygulama logları Log4j2 JSON layout ile Kafka'ya yazılıyor, Logstash ile OpenSearch'e indeksleniyor. OTEL Collector ile distributed tracing de yapıyoruz. AlertManager ile kritik eşikler aşıldığında alarm gönderiliyor."

### Soru: Neden bu kadar çok konteyner var?

> "Her servis tek bir sorumluluk üstleniyor. Lightweight profille yalnızca 6 konteynerle de çalıştırabiliyoruz. Tam stack, production-ready bir mimari sunmak için gerekli. Kaynak limitleri Docker Compose'da tanımlı, her konteyner izole."

---

## 7. Canlı Kod Gösterim Sırası (Pratik Rehber)

| # | Dosya | Ne göster | Konuşma |
|---|---|---|---|
| 1 | `ARCHITECTURE.md` | Mermaid diyagramı | "İşte C4 container görünümümüz" |
| 2 | `TASARIM_MIMARISI_VE_MODELLEME.md` | ER model + state diyagramı | "Domain modelimizin çekirdeği bu" |
| 3 | `MarketDataService.java` | `fetchAndSave` metodu | "Veri akışı tasarımdaki sequence'i birebir yansıtıyor" |
| 4 | `PortfolioFinancialRulesService.java` | Doğrulama kuralları | "Emir kuralları burada — nakit ve pozisyon kontrolü" |
| 5 | `PortfolioOrderExecutionService.java` | Emir çalıştırma | "State geçişleri burada gerçekleşiyor" |
| 6 | `SecurityConfig.java` | JWT yapılandırması | "OAuth2 resource server olarak JWT doğrulaması" |
| 7 | `frontend/src/store/api/` | RTK Query slice | "Frontend API çağrıları bu slice'lardan geçiyor" |
| 8 | `docker-compose.yml` | Servis tanımları | "15 servis, healthcheck, depends_on zincirleri" |

---

## 8. Kapanış Metni (30 saniye)

> "Tasarım dokümanları ile kodu hizalı şekilde ilerlettik. 14 MVP özelliğini tamamladık, kapsamlı bir altyapı kurduk. Toplantıdaki geri bildirimlere göre TypeScript strict mode geçişi, büyük servislerin bölünmesi ve test kapsamı artırımı üzerinde çalışacağız. Teşekkürler."

---

## 9. Olası Teknik Demolar İçin Hazırlık

- **Swagger UI:** `http://localhost:8088/swagger-ui.html` — API endpoint'lerini göstermek için
- **Grafana:** `http://localhost:13030` — Metrik panel'ini göstermek için
- **Keycloak Admin:** `http://localhost:8180/admin` — Realm, client, rol yapısını göstermek için
- **OpenSearch Dashboards:** `http://localhost:15601` — Log arama için
