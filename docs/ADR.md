# Mimari Karar Kayitlari (ADR)

Bu dosya, proje boyunca alinmis ana teknik kararlarin nedenleriyle birlikte kaydidir.

## ADR-001: Backend olarak Spring Boot 3.4 + Java 17

- Karar: Backend icin Spring Boot 3.4 kullanildi.
- Neden: Guvenlik, scheduler, websocket ve kurumsal ekosistem olgunlugu.
- Sonuc: Moduler monolith yapida hizli teslimat + genis kutuphane destegi.

## ADR-002: Frontend olarak React 18 + Vite + TypeScript

- Karar: SPA katmani React/Vite ile gelistirildi.
- Neden: Bilesen ekosistemi, hizli gelistirme dongusu, RTK Query uyumu.
- Sonuc: UI gelistirme hizi yuksek; tip guvenligi asamali olarak guclendiriliyor.

## ADR-003: PostgreSQL + Redis kombinasyonu

- Karar: Kalici veri PostgreSQL, sicak veri ve cache Redis.
- Neden: ACID + performans dengesi.
- Sonuc: Okuma performansinda iyilesme ve is kurallarinda guvenilirlik.

## ADR-004: Kimlik yonetimi icin Keycloak

- Karar: OAuth2/OIDC icin Keycloak kullanildi.
- Neden: Rol bazli yetkilendirme, realm/client yonetimi, LDAP ve 2FA destegi.
- Sonuc: Kimlik yonetimi uygulamadan ayrildi, guvenlik merkezi hale geldi.

## ADR-005: Gercek zamanli guncelleme icin WebSocket (STOMP)

- Karar: Canli fiyat ve event aktarimi icin STOMP/SockJS.
- Neden: Polling maliyetini azaltmak ve kullaniciya anlik veri sunmak.
- Sonuc: Dashboard ve piyasa ekranlarinda daha akici deneyim.

## ADR-006: Coklu veri saglayici modeli

- Karar: TCMB, Yahoo, Alpha Vantage, Finnhub desteklendi.
- Neden: Veri kapsami ve saglayici bagimliligini azaltmak.
- Sonuc: Provider tercih/fallback mekanizmasi ile esnek veri toplama.

## ADR-007: Gozlemlenebilirlik katmani

- Karar: Prometheus + Grafana + OpenSearch + OTEL + Logstash.
- Neden: Metrik, log ve iz takibini tek bir operasyon modelinde birlestirmek.
- Sonuc: Sorun tespiti ve kok neden analizi hizlandi.

## ADR-008: Docker Compose ile profil bazli calisma

- Karar: Varsayilan, secure-dev ve light profiller.
- Neden: Farkli ortam ihtiyaclarina gore kaynak ve guvenlik dengesi.
- Sonuc: Gelistirme ve test sureclerinde esneklik.
