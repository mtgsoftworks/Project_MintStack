# MintStack Finance Portal

Finansal piyasa verilerini takip etme, portföy yönetimi ve teknik analiz yapma platformu.

## Özellikler

- **Piyasa Verileri**: Döviz kurları (TCMB), hisse senetleri (BIST), tahvil/bono, fonlar, VIOP
- **Haber Modülü**: Finans haberlerini kategorize edilmiş şekilde görüntüleme
- **Portföy Yönetimi**: Kendi portföyünüzü oluşturun, kar/zarar takibi yapın
- **Teknik Analiz**: Hareketli ortalamalar, trend analizi, enstrüman karşılaştırma
- **Güvenlik**: Keycloak ile OAuth2/OpenID Connect, 2FA desteği

## Teknoloji Stack

### Backend
- Java 17 + Spring Boot 3.2
- PostgreSQL + Spring Data JPA + Flyway
- Redis Cache
- Kafka + OpenSearch (Loglama)
- OpenTelemetry (Observability)

### Frontend
- React 18 + Vite
- TailwindCSS
- React Query
- Recharts
- Keycloak JS

### Altyapı
- Docker + Docker Compose
- Keycloak + OpenLDAP
- Nginx

## Kurulum

### Gereksinimler
- Docker & Docker Compose
- Node.js 20+ (geliştirme için)
- Java 17+ (geliştirme için)

### Hızlı Başlangıç

1. Repository'yi klonlayın:
```bash
git clone <repo-url>
cd Project_MintStack
```

2. Environment dosyasını oluşturun:
```bash
cp env.local .env
```

3. Docker Compose ile başlatın:
```bash
docker-compose up -d
```

4. Servislerin başlamasını bekleyin (~2-3 dakika)

### Erişim URL'leri

| Servis | URL |
|--------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Keycloak | http://localhost:8180 |
| OpenSearch Dashboards | http://localhost:5601 |

### Varsayılan Kullanıcılar

| Kullanıcı | Şifre | Rol |
|-----------|-------|-----|
| admin | Admin123! | Admin |
| testuser | Test123! | User |

## Geliştirme

### Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Proje Yapısı

```
Project_MintStack/
├── backend/                 # Spring Boot API
│   ├── src/main/java/
│   │   └── com/mintstack/finance/
│   │       ├── config/      # Konfigürasyon sınıfları
│   │       ├── controller/  # REST Controller'lar
│   │       ├── service/     # İş mantığı
│   │       ├── repository/  # JPA Repository'ler
│   │       ├── entity/      # JPA Entity'ler
│   │       ├── dto/         # Data Transfer Objects
│   │       ├── exception/   # Exception handling
│   │       └── scheduler/   # Zamanlanmış görevler
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/    # Flyway migration'ları
├── frontend/                # React Application
│   └── src/
│       ├── components/      # React componentleri
│       ├── pages/           # Sayfa componentleri
│       ├── services/        # API servisleri
│       ├── context/         # React context
│       ├── hooks/           # Custom hooks
│       └── utils/           # Utility fonksiyonları
├── docker/                  # Docker konfigürasyonları
├── keycloak/               # Keycloak realm export
├── openldap/               # OpenLDAP LDIF dosyaları
└── docs/                   # Dokümantasyon
```

## API Dokümantasyonu

Detaylı API dokümantasyonu için:
- [API Docs](docs/api-docs.md)
- Swagger UI: http://localhost:8080/swagger-ui.html

## Lisans

MIT License
