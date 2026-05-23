---
title: "MintStack Finance Portal"
subtitle: "Proje Teslim Sunumu"
author: "Mesut Taha Güven"
date: "7 Haziran 2026"
---

# Kişisel Bilgiler

- Ad Soyad: Mesut Taha Güven
- Not ortalaması: [GPA bilgisi]
- Sınıf: [Sınıf bilgisi]
- Yabancı diller: [Dil ve seviye bilgisi]
- Mesleki ilgi alanları: Backend, finans teknolojileri, güvenlik, veri akışı, observability

---

# Projenin Amacı

MintStack Finance Portal, yatırımcıların piyasa verilerini, portföylerini, alarm kurallarını, haberleri ve teknik analiz çıktısını tek web portalı üzerinden takip etmesini sağlar.

Ana hedef, Türkiye odaklı finansal verileri kurumsal mimari standartlarına uygun, güvenli ve gözlemlenebilir bir uygulamada toplamaktır.

---

# Kullanılan Ana Teknolojiler

- Frontend: React 18, TypeScript, Vite, Redux Toolkit, RTK Query
- Backend: Java 21, Spring Boot 3.4.2, Spring Security, Spring Data JPA
- Veritabanı: PostgreSQL 15, Flyway
- Güvenlik: Keycloak, JWT, RBAC, TOTP 2FA
- DevOps: Docker Compose, GitHub Actions

---

# Ek Teknolojiler

- Redis cache ve rate limiting
- Kafka event/log pipeline
- Log4j2 JSON logging
- OpenSearch ve Logstash
- Prometheus, Grafana, Alertmanager
- OpenTelemetry Collector ve OTLP tracing
- WebSocket/STOMP canlı fiyat akışı

---

# Uygulama Modülleri

- Dashboard ve piyasa özeti
- BIST hisse, döviz, fon, tahvil/bono ve VIOP ekranları
- Sanal portföy, nakit, alım/satım ve emir yaşam döngüsü
- İzleme listesi ve fiyat alarmları
- Haber akışı ve bildirimler
- Admin, runtime settings, rate limit ve observability yönetimi

---

# Mimari

React SPA, Nginx gateway üzerinden Spring Boot API’ye bağlanır.

Backend katmanları controller, service, repository, entity, dto ve mapper olarak ayrılmıştır. PostgreSQL kalıcı veri, Redis cache, Kafka event/log hattı, Keycloak kimlik doğrulama ve OpenSearch log/arama altyapısı olarak kullanılır.

---

# Güvenlik

- OAuth2/OIDC tabanlı Keycloak entegrasyonu
- JWT resource server doğrulaması
- `ADMIN` ve `user` rolleri ile RBAC
- Demo kullanıcılarında ilk girişte TOTP kurulumu
- Public ve admin endpoint ayrımı
- Alert webhook için imza/CIDR sertleştirme seçenekleri

---

# Öğrendiklerim

- Spring Boot 3 ile katmanlı kurumsal uygulama geliştirme
- Flyway migration ve PostgreSQL veri modeli yönetimi
- Keycloak ile JWT, rol bazlı yetki ve 2FA entegrasyonu
- React tarafında RTK Query ile API state yönetimi
- Docker Compose ile çok servisli geliştirme ortamı kurma

---

# Zorlayan Bölümler

- Gerçek piyasa veri kaynaklarının farklı format ve güncelleme davranışları
- WebSocket, cache ve scheduler akışlarının tutarlı çalışması
- Admin/user yetki sınırlarının netleştirilmesi
- OpenSearch, Kafka, Logstash ve OTEL parçalarının birlikte doğrulanması
- CI üzerinde backend, frontend, Flyway ve Docker kontrollerini stabil hale getirmek

---

# Teslim Durumu

Finans Portalı isterleri büyük ölçüde tamamlanmıştır:

- React, Java 21, Spring Boot, PostgreSQL, JPA, Flyway
- JWT + Keycloak, 2FA
- Log4j2, Kafka log hattı, OpenSearch
- Prometheus, Grafana, OpenTelemetry
- Docker, README, API docs, unit test, error handling
- Analiz, teknik analiz, Javadocs ve PPTX sunum artifactleri

---

# Kapsam Dışı / Eksik Notu

IT Servis - Ticket Yönetimi ve jBPM isterleri bu repo kapsamında değildir.

Bu proje yalnızca Finans Portalı olarak teslim edilmektedir. jBPM maddesi sunum ve teslim checklistinde “kapsam dışı” olarak belirtilmiştir.
