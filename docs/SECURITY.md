# MintStack Finance Portal - Güvenlik Dokümantasyonu

Bu dokümantasyon, MintStack Finance Portal'ın güvenlik yapısını, secret yönetimini ve production için güvenlik en iyi uygulamalarını içerir.

---

## İçindekiler

1. [Secret Yönetimi](#secret-yönetimi)
2. [Secret Üretimi](#secret-üretimi)
3. [Ortam Değişkenleri Referansı](#ortam-değişkenleri-referansı)
4. [Keycloak Konfigürasyonu](#keycloak-konfigürasyonu)
5. [LDAP Konfigürasyonu](#ldap-konfigürasyonu)
6. [OpenSearch Güvenliği](#opensearch-güvenliği)
7. [Kafka SASL Konfigürasyonu](#kafka-sasl-konfigürasyonu)
8. [Production Güvenlik En İyi Uygulamaları](#production-güvenlik-en-iyi-uygulamaları)

---

## Secret Yönetimi

### Genel İlkeler

- **Asla** secret'ları kod reposuna commit etmeyin
- `.env` dosyası `.gitignore`'a eklenmelidir
- Production'da Docker Secrets veya HashiCorp Vault kullanın
- Secret'ları düzenli olarak rotate edin

### Secret Dosyaları

| Dosya | Amaç | Commit Edilmeli mi? |
|-------|------|---------------------|
| `.env.example` | Template | ✅ Evet |
| `.env` | Gerçek değerler | ❌ Hayır |
| `frontend/.env.production.example` | Template | ✅ Evet |
| `frontend/.env.production` | Gerçek değerler | ❌ Hayır |
| `backend/.env.production.example` | Template | ✅ Evet |
| `keycloak/realm-export.json` | Kullanıcı/rol verileri | ⚠️ Production'da dikkat |

---

## Secret Üretimi

### OpenSSL ile Secret Üretimi

```bash
# 24-byte base64 secret (192-bit)
openssl rand -base64 24

# 32-byte base64 secret (256-bit)
openssl rand -base64 32

# Hex format
openssl rand -hex 32

# Alpine/Linux container içinde
docker run --rm alpine:latest sh -c "apk add openssl && openssl rand -base64 32"
```

### Secret Türlerine Göre Öneriler

| Secret Türü | Minimum Uzunluk | Üretim Komutu |
|-------------|-----------------|---------------|
| Database Password | 24 chars | `openssl rand -base64 24` |
| Redis Password | 24 chars | `openssl rand -base64 24` |
| Keycloak Admin | 32 chars | `openssl rand -base64 32` |
| Kafka SASL Password | 24 chars | `openssl rand -base64 24` |
| OpenSearch Admin | 16+ chars (complex) | Manuel güçlü şifre |
| Grafana Admin | 24 chars | `openssl rand -base64 24` |

### OpenSearch Şifre Gereksinimleri

OpenSearch admin şifresi şu kriterleri karşılamalıdır:
- Minimum 8 karakter
- En az 1 büyük harf
- En az 1 küçük harf
- En az 1 rakam
- En az 1 özel karakter

```bash
# Örnek güçlü şifre oluşturma
echo "Admin@$(openssl rand -hex 4)2026!"
```

---

## Ortam Değişkenleri Referansı

### Database (PostgreSQL)

| Değişken | Açıklama | Örnek |
|----------|----------|-------|
| `POSTGRES_DB` | Ana veritabanı adı | `mintstack_finance` |
| `POSTGRES_USER` | Veritabanı kullanıcısı | `mintstack` |
| `POSTGRES_PASSWORD` | Veritabanı şifresi | `<base64-secret>` |

### Redis

| Değişken | Açıklama | Örnek |
|----------|----------|-------|
| `REDIS_PASSWORD` | Redis authentication şifresi | `<base64-secret>` |

### Keycloak

| Değişken | Açıklama | Örnek |
|----------|----------|-------|
| `KEYCLOAK_ADMIN` | Admin kullanıcı adı | `admin` |
| `KEYCLOAK_ADMIN_PASSWORD` | Admin konsol şifresi | `<base64-secret>` |
| `KEYCLOAK_FINANCE_BACKEND_SECRET` | Backend client secret | `<base64-secret>` |

### LDAP

| Değişken | Açıklama | Örnek |
|----------|----------|-------|
| `LDAP_ORGANISATION` | Organizasyon adı | `MintStack` |
| `LDAP_DOMAIN` | LDAP domain | `mintstack.local` |
| `LDAP_ADMIN_PASSWORD` | Admin bind şifresi | `<base64-secret>` |

### Kafka

| Değişken | Açıklama | Örnek |
|----------|----------|-------|
| `KAFKA_BROKER_ID` | Broker ID | `1` |
| `KAFKA_SASL_USER` | SASL kullanıcı adı | `kafka` |
| `KAFKA_SASL_PASSWORD` | SASL şifresi | `<base64-secret>` |

### OpenSearch

| Değişken | Açıklama | Örnek |
|----------|----------|-------|
| `OPENSEARCH_INITIAL_ADMIN_PASSWORD` | Admin şifresi | `<complex-password>` |

### External APIs

| Değişken | Açıklama | Nasıl Alınır |
|----------|----------|--------------|
| `ALPHA_VANTAGE_API_KEY` | Alpha Vantage API key | https://www.alphavantage.co/support/#api-key |
| `FINNHUB_API_KEY` | Finnhub API key | https://finnhub.io/register |

---

## Keycloak Konfigürasyonu

### Realm Yapısı

```
mintstack-finance (Realm)
├── Clients
│   ├── finance-backend (bearer-only)
│   │   └── Secret: KEYCLOAK_FINANCE_BACKEND_SECRET
│   └── finance-frontend (public, PKCE)
│       └── No secret (PKCE flow)
├── Roles
│   ├── USER
│   └── ADMIN
└── Users
    ├── admin (ADMIN role)
    └── testuser (USER role)
```

### Token Ayarları

```yaml
# Realm Settings → Sessions
SSO Session Idle: 30 minutes
SSO Session Max: 10 hours
Offline Session Idle: 30 days

# Client Settings → Advanced
Access Token Lifespan: 5 minutes
Refresh Token Max Reuse: 1
```

### Güvenlik Önerileri

1. **Production'da HTTPS zorunlu** - `KC_HOSTNAME_STRICT_HTTPS: "true"`
2. **2FA etkinleştir** - `docs/KEYCLOAK_2FA_SETUP.md` takip edin
3. **Brute force koruması** - Realm Settings → Security Defenses
4. **Strong password policy** - Realm Settings → Password Policy

### LDAP Federation

Keycloak, OpenLDAP ile federasyon yapılandırılmıştır:

```yaml
# Keycloak LDAP Settings
Vendor: Other
Connection URL: ldap://openldap:389
Bind DN: cn=admin,dc=mintstack,dc=local
Bind Credential: ${LDAP_ADMIN_PASSWORD}
Users DN: ou=users,dc=mintstack,dc=local
```

---

## LDAP Konfigürasyonu

### OpenLDAP Yapısı

```
dc=mintstack,dc=local
├── ou=users
│   ├── cn=admin
│   ├── cn=testuser
│   └── ...
├── ou=groups
│   ├── cn=finance-users
│   └── cn=finance-admins
└── cn=admin (admin user)
```

### TLS Konfigürasyonu

LDAP sertifikaları `docker/openldap/certs/` dizininde:

```
docker/openldap/certs/
├── ca.crt          # CA certificate
├── ldap.crt        # Server certificate
└── ldap.key        # Server private key
```

### LDIF Seed Data

Kullanıcılar `docker/openldap/ldif/` içindeki LDIF dosyaları ile seed edilir:

```ldif
# 01-users.ldif
dn: cn=testuser,ou=users,dc=mintstack,dc=local
objectClass: inetOrgPerson
cn: testuser
sn: User
uid: testuser
userPassword: {SSHA}hashed_password
```

---

## OpenSearch Güvenliği

### Authentication

OpenSearch, HTTPS ve basic authentication ile korunmaktadır:

```yaml
plugins:
  security:
    disabled: false
    ssl:
      http:
        enabled: true
        pemcert_filepath: esnode.pem
        pemkey_filepath: esnode-key.pem
        pemtrustedcas_filepath: root-ca.pem
```

### Bağlantı

```bash
# curl ile bağlantı
curl -k -u admin:${OPENSEARCH_INITIAL_ADMIN_PASSWORD} https://localhost:19200

# Health check
curl -k -u admin:${OPENSEARCH_INITIAL_ADMIN_PASSWORD} \
  https://localhost:19200/_cluster/health
```

### Index Erişim Kontrolü

Production'da roller ile index erişimi kısıtlanmalıdır:

```json
// Security roles
{
  "finance_backend_role": {
    "index_permissions": [
      {
        "index_patterns": ["mintstack-*"],
        "allowed_actions": ["read", "write", "create_index"]
      }
    ]
  }
}
```

---

## Kafka SASL Konfigürasyonu

### JAAS Configuration

Kafka SASL/PLAIN authentication kullanmaktadır:

```properties
# kafka_server_jaas.conf
KafkaServer {
  org.apache.kafka.common.security.plain.PlainLoginModule required
  username="kafka"
  password="${KAFKA_SASL_PASSWORD}"
  user_kafka="${KAFKA_SASL_PASSWORD}";
};
```

### Client Configuration

Backend Kafka client konfigürasyonu:

```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    properties:
      security.protocol: SASL_PLAINTEXT
      sasl.mechanism: PLAIN
      sasl.jaas.config: org.apache.kafka.common.security.plain.PlainLoginModule required username="kafka" password="${KAFKA_SASL_PASSWORD}";
```

### Logstash Consumer

```ruby
# logstash.conf
input {
  kafka {
    bootstrap_servers => "kafka:9092"
    topics => ["mintstack-logs"]
    sasl_mechanism => "PLAIN"
    sasl_jaas_config => "org.apache.kafka.common.security.plain.PlainLoginModule required username='kafka' password='${KAFKA_SASL_PASSWORD}';"
    security_protocol => "SASL_PLAINTEXT"
  }
}
```

---

## Production Güvenlik En İyi Uygulamaları

### 1. Secret Yönetimi

```bash
# Docker Secrets kullanımı (Swarm mode)
echo "your_secret_password" | docker secret create postgres_password -

# docker-compose.prod.yml
services:
  backend:
    secrets:
      - postgres_password
secrets:
  postgres_password:
    external: true
```

### 2. Network Güvenliği

```yaml
# Sadece gerekli portları expose et
services:
  postgres:
    ports:
      - "127.0.0.1:5432:5432"  # Sadece localhost

  # Veya hiç expose etme (internal network)
  redis:
    expose:
      - "6379"
    # ports: []  # External erişim yok
```

### 3. SSL/TLS

```yaml
# Keycloak Production
keycloak:
  environment:
    KC_HOSTNAME: your-domain.com
    KC_HOSTNAME_STRICT: "true"
    KC_HOSTNAME_STRICT_HTTPS: "true"
    KC_HTTP_ENABLED: "false"
    KC_PROXY: edge
```

### 4. Container Güvenliği

```yaml
# Read-only filesystem
services:
  backend:
    read_only: true
    tmpfs:
      - /tmp

# Non-root user
services:
  frontend:
    user: "1000:1000"
```

### 5. Güvenlik Duvarı Kuralları

```bash
# UFW örneği (Ubuntu)
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw enable
```

### 6. Backup Şifreleme

```bash
# PostgreSQL backup şifreleme
pg_dump -U mintstack mintstack_finance | \
  gzip | \
  openssl enc -aes-256-cbc -salt -pbkdf2 -out backup.sql.gz.enc

# Restore
openssl enc -aes-256-cbc -d -pbkdf2 -in backup.sql.gz.enc | \
  gunzip | \
  psql -U mintstack mintstack_finance
```

### 7. Security Checklist

- [ ] Tüm default şifreler değiştirildi
- [ ] HTTPS tüm servislerde etkin
- [ ] Secret'lar kod reposunda değil
- [ ] Database portları internal
- [ ] LDAP TLS etkin
- [ ] Keycloak 2FA etkin (admin için)
- [ ] Kafka SASL authentication etkin
- [ ] OpenSearch authentication etkin
- [ ] Regular backup schedule
- [ ] Security headers (CSP, X-Frame-Options, etc.)
- [ ] Rate limiting etkin
- [ ] Log rotation yapılandırıldı
- [ ] Monitoring alerts yapılandırıldı

---

## Güvenlik İhlali Durumunda

1. **Etkilenen servisleri izole edin**
2. **Tüm secret'ları rotate edin**
3. **Log'ları analiz edin** (OpenSearch Dashboards)
4. **Kullanıcı hesaplarını kontrol edin** (Keycloak)
5. **Gerekirse veritabanı backup'tan restore edin**

---

*Son Güncelleme: Şubat 2026*
