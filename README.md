# 🏦 MintStack Finance Portal

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen?logo=springboot)
![React](https://img.shields.io/badge/React-18.2-blue?logo=react)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)
![License](https://img.shields.io/badge/License-MIT-yellow)

**Türkiye odaklı kapsamlı finansal veri takip, portföy yönetimi ve teknik analiz platformu**

[Özellikler](#-özellikler) •
[Kurulum](#-hızlı-başlangıç) •
[API Dokümantasyonu](#-api-dokümantasyonu) •
[Mimari](#-mimari)

</div>

---

## ✨ Özellikler

### 📊 Piyasa Verileri
- **Döviz Kurları**: TCMB'den anlık ve geçmiş döviz kurları (USD, EUR, GBP, CHF, vb.)
- **Hisse Senetleri**: BIST hisseleri takibi
- **Tahvil & Bono**: Devlet tahvili ve bono verileri
- **Fonlar**: Yatırım fonları bilgileri
- **VIOP**: Vadeli işlem ve opsiyon verileri

### 📰 Haber Modülü
- Kategorize edilmiş finans haberleri
- Haber arama ve filtreleme
- RSS/API entegrasyonu ile otomatik güncelleme

### 💼 Portföy Yönetimi
- Çoklu portföy oluşturma
- Gerçek zamanlı kar/zarar hesaplama
- Varlık dağılımı analizi
- İşlem geçmişi takibi

### 📈 Teknik Analiz
- Hareketli ortalamalar (SMA, EMA)
- Trend analizi
- Enstrüman karşılaştırma
- Tarihsel veri grafikleri

### 🔐 Güvenlik & Kimlik Yönetimi
- **OAuth2/OpenID Connect** (Keycloak)
- **LDAP entegrasyonu** (OpenLDAP)
- İki faktörlü kimlik doğrulama (2FA)
- "Beni Hatırla" özelliği
- Rol tabanlı yetkilendirme (RBAC)

### 📡 Observability
- **OpenTelemetry** ile dağıtık izleme
- **OpenSearch** ile log aggregation
- **Prometheus** metrikleri
- Gerçek zamanlı dashboard'lar

---

## 🛠 Teknoloji Stack

<table>
<tr>
<td valign="top" width="50%">

### Backend
| Teknoloji | Versiyon |
|-----------|----------|
| Java | 17 |
| Spring Boot | 3.2.1 |
| Spring Security | OAuth2 Resource Server |
| Spring Data JPA | Hibernate 6.4 |
| Flyway | 9.22 |
| PostgreSQL | 15 |
| Redis | 7 |
| Kafka | 3.5 |

</td>
<td valign="top" width="50%">

### Frontend
| Teknoloji | Versiyon |
|-----------|----------|
| React | 18.2 |
| Vite | 5.0 |
| TailwindCSS | 3.4 |
| React Query | 5.0 |
| React Router | 6.0 |
| Recharts | 2.10 |
| Keycloak JS | 23.0 |

</td>
</tr>
</table>

### Altyapı
- **Containerization**: Docker & Docker Compose
- **Identity**: Keycloak 23 + OpenLDAP
- **Logging**: Logstash → Kafka → OpenSearch
- **Tracing**: OpenTelemetry Collector
- **Reverse Proxy**: Nginx

---

## 🚀 Hızlı Başlangıç

### Gereksinimler
- Docker Desktop (Windows/Mac) veya Docker Engine (Linux)
- Docker Compose v2+
- Git

### Kurulum

```bash
# 1. Repository'yi klonlayın
git clone https://github.com/YOUR_USERNAME/MintStack-Finance.git
cd MintStack-Finance

# 2. Environment dosyasını oluşturun
cp env.local .env

# 3. Tüm servisleri başlatın
docker-compose up -d

# 4. Servislerin hazır olmasını bekleyin (~2-3 dakika)
docker-compose ps
```

### Servis URL'leri

| Servis | URL | Açıklama |
|--------|-----|----------|
| 🌐 Frontend | http://localhost:3000 | React Web Uygulaması |
| 🔌 Backend API | http://localhost:8080/api/v1 | REST API |
| 📚 Swagger UI | http://localhost:8080/swagger-ui.html | API Dokümantasyonu |
| 🔐 Keycloak | http://localhost:8180 | Identity Management |
| 📊 OpenSearch Dashboards | http://localhost:5601 | Log & Metrics Dashboard |

### Varsayılan Kullanıcılar

| Kullanıcı | Şifre | Rol | Açıklama |
|-----------|-------|-----|----------|
| `admin` | `Admin123!` | Admin | Tam yetki |
| `testuser` | `Test123!` | User | Standart kullanıcı |

**Keycloak Admin Konsolu**: `admin` / `KeycloakAdmin2026!`

---

## 📁 Proje Yapısı

```
MintStack-Finance/
├── 📂 backend/                    # Spring Boot API
│   ├── src/main/java/.../
│   │   ├── config/               # Security, Redis, Kafka config
│   │   ├── controller/           # REST endpoints
│   │   ├── service/              # Business logic
│   │   ├── repository/           # Data access layer
│   │   ├── entity/               # JPA entities
│   │   ├── dto/                  # Request/Response DTOs
│   │   ├── exception/            # Global exception handling
│   │   └── scheduler/            # Cron jobs (TCMB, haberler)
│   └── src/main/resources/
│       ├── application.yml       # Ana konfigürasyon
│       └── db/migration/         # Flyway SQL scripts
│
├── 📂 frontend/                   # React SPA
│   └── src/
│       ├── components/           # Reusable UI components
│       ├── pages/                # Route pages
│       ├── services/             # API client services
│       ├── context/              # Auth & Theme context
│       ├── hooks/                # Custom React hooks
│       └── utils/                # Helper functions
│
├── 📂 docker/                     # Docker configurations
│   ├── postgres/                 # DB init scripts
│   ├── otel/                     # OpenTelemetry config
│   └── logstash/                 # Logstash pipeline
│
├── 📂 keycloak/                   # Realm export (users, roles)
├── 📂 openldap/                   # LDIF seed data
├── 📂 .github/workflows/          # CI/CD pipelines
│
├── docker-compose.yml            # Service orchestration
├── env.local                     # Environment template
└── README.md
```

---

## 📖 API Dokümantasyonu

### Ana Endpointler

| Endpoint | Method | Açıklama | Auth |
|----------|--------|----------|------|
| `/api/v1/market/currencies` | GET | Tüm döviz kurları | ❌ |
| `/api/v1/market/currencies/{code}` | GET | Tek döviz kuru | ❌ |
| `/api/v1/market/instruments` | GET | Enstrüman listesi | ✅ |
| `/api/v1/news` | GET | Haberler (paginated) | ❌ |
| `/api/v1/news/{id}` | GET | Haber detayı | ❌ |
| `/api/v1/portfolio` | GET | Kullanıcı portföyleri | ✅ |
| `/api/v1/portfolio` | POST | Yeni portföy oluştur | ✅ |
| `/api/v1/portfolio/{id}/items` | POST | Portföye enstrüman ekle | ✅ |
| `/api/v1/analysis/history/{symbol}` | GET | Fiyat geçmişi | ✅ |
| `/api/v1/analysis/compare` | POST | Enstrüman karşılaştırma | ✅ |
| `/api/v1/user/profile` | GET | Kullanıcı profili | ✅ |

> 📘 Detaylı API dokümantasyonu için [Swagger UI](http://localhost:8080/swagger-ui.html) kullanın.

---

## 🔧 Geliştirme

### Backend (Spring Boot)

```bash
cd backend

# Bağımlılıkları indir
./mvnw dependency:resolve

# Geliştirme modunda çalıştır
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Testleri çalıştır
./mvnw test

# Production build
./mvnw clean package -DskipTests
```

### Frontend (React + Vite)

```bash
cd frontend

# Bağımlılıkları kur
npm install

# Geliştirme sunucusu
npm run dev

# Testleri çalıştır
npm test

# Production build
npm run build
```

---

## 🏗 Mimari

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│   Nginx     │────▶│   Backend   │
│   (React)   │     │  (Reverse)  │     │ (Spring Boot)│
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
        ┌──────────────────────────────────────┼──────────────────────┐
        │                                      │                      │
        ▼                                      ▼                      ▼
┌───────────────┐                    ┌─────────────────┐     ┌────────────┐
│   Keycloak    │                    │   PostgreSQL    │     │   Redis    │
│   (OAuth2)    │                    │   (Database)    │     │  (Cache)   │
└───────┬───────┘                    └─────────────────┘     └────────────┘
        │
        ▼
┌───────────────┐
│   OpenLDAP    │
│   (Users)     │
└───────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                         Observability Stack                              │
├─────────────────┬─────────────────┬─────────────────┬───────────────────┤
│  OTel Collector │    Logstash     │     Kafka       │    OpenSearch     │
│   (Tracing)     │   (Pipeline)    │   (Streaming)   │   (Storage/UI)    │
└─────────────────┴─────────────────┴─────────────────┴───────────────────┘
```

---

## 🤝 Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit'leyin (`git commit -m 'feat: Add amazing feature'`)
4. Push'layın (`git push origin feature/amazing-feature`)
5. Pull Request açın

---

## 📄 Lisans

Bu proje [MIT Lisansı](LICENSE) altında lisanslanmıştır.

---

<div align="center">

**[⬆ Başa Dön](#-mintstack-finance-portal)**

Made with ❤️ in Turkey

</div>
