# 🔑 MintStack Finance Portal — Servis Şifreleri ve Erişim Bilgileri Rehberi

> **Önemli Not:** Bu doküman, geliştirme ve yerel test ortamında kullanılan tüm servislerin giriş bilgileri, port numaraları ve varsayılan şifrelerini içerir.

---

## 📋 1. Tüm Servislerin Şifre ve Erişim Matrisi

| Servis Adı | URL / Port | Kullanıcı / Admin | Parola / Secret | Açıklama |
| :--- | :--- | :--- | :--- | :--- |
| **OpenSearch** | `http://localhost:19200` | `admin` | `MintStack#2026!SecOps` | Merkezi Log arama API |
| **OpenSearch Dashboards** | `http://localhost:15601` | `admin` | `MintStack#2026!SecOps` | Log görselleştirme arayüzü |
| **Grafana** | `http://localhost:13030` | `admin` | `admin` *(veya `.env` şifresi)* | Metrik ve sağlık panoları |
| **Keycloak Admin Console** | `http://localhost:8180` | `admin` | `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA` | Kimlik yönetimi konsolu |
| **Keycloak (Demo Admin User)**| `http://localhost:8180` | `admin` | `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA` | Sistem Admin test kullanıcısı |
| **Keycloak (Demo Test User)** | `http://localhost:8180` | `testuser` | `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA` | Normal test kullanıcısı |
| **Keycloak Client Secret** | `finance-backend` | `finance-backend` | `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA` | Backend confidential client secret |
| **PostgreSQL 15** | `localhost:5432` | `mintstack` | `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA` | Veritabanı (`mintstack_finance`) |
| **Redis 7** | `localhost:16379` | *(Yok)* | `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA` | Önbellek ve Rate Limit store |
| **Apache Kafka (SASL)** | `localhost:29092` | `kafka` | `kF9mQ2XvL8pZ5wN3rY7tB4jM` | KRaft Event Bus SASL PLAIN |
| **OpenLDAP** | `localhost:389` | `admin` | `admin` | Şirket dizini (`dc=mintstack,dc=local`)|
| **Prometheus** | `http://localhost:9090` | *(Yok)* | *(Kimlik doğrulamasız)* | Sistem ve JVM metrik toplayıcı |
| **AlertManager** | `http://localhost:9093` | *(Yok)* | *(Kimlik doğrulamasız)* | Operasyonel alarm yönlendirici |
| **Nginx API Gateway** | `http://localhost:8088` | *(Dış Erişim)* | *(Public Reverse Proxy)* | Ana giriş kapısı |
| **React Frontend** | `http://localhost:3000` | *(Web UI)* | Keycloak ile login | Kullanıcı arayüzü |

---

## 🔒 2. Servis Bazlı Detaylı Açıklamalar

### 🔎 OpenSearch & Dashboards
- **API Endpoint:** `https://localhost:9200` veya `http://localhost:19200`
- **Dashboard UI:** `http://localhost:15601`
- **Kullanıcı:** `admin`
- **Şifre:** `MintStack#2026!SecOps`
- **Sağlık Kontrolü Komutu:**
  ```bash
  curl -k -u admin:MintStack#2026!SecOps https://localhost:9200/_cluster/health
  ```

---

### 📊 Grafana
- **Arayüz URL:** `http://localhost:13030`
- **Kullanıcı:** `admin`
- **Şifre:** `admin`
- **Bağlı DataSource:** Prometheus (`http://prometheus:9090`)

---

### 🔐 Keycloak Kimlik Yönetimi
- **Konsol URL:** `http://localhost:8180`
- **Realm:** `mintstack-finance`
- **Admin Kullanıcı:** `admin` / `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA`
- **Test Kullanıcısı:** `testuser` / `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA`
- **Backend Client Secret:** `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA`

---

### 🐘 PostgreSQL 15
- **Host / Port:** `localhost:5432`
- **Veritabanı Adı:** `mintstack_finance`
- **Kullanıcı:** `mintstack`
- **Şifre:** `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA`
- **Bağlantı String'i:**
  ```text
  jdbc:postgresql://localhost:5432/mintstack_finance?user=mintstack&password=9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA
  ```

---

### ⚡ Redis 7
- **Host / Port:** `localhost:16379` (Docker içinden `redis:6379`)
- **Şifre:** `9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA`
- **CLI Test Komutu:**
  ```bash
  redis-cli -p 16379 -a 9mIiXfX3tjYK26tNo2ql2KL1pIuF1XIA ping
  ```

---

### 📨 Apache Kafka (SASL PLAIN)
- **Host / Port:** `localhost:29092`
- **SASL Kullanıcı:** `kafka`
- **SASL Şifre:** `kF9mQ2XvL8pZ5wN3rY7tB4jM`
- **JAAS Yapılandırması:**
  ```text
  org.apache.kafka.common.security.plain.PlainLoginModule required username="kafka" password="kF9mQ2XvL8pZ5wN3rY7tB4jM";
  ```

---

### 🌳 OpenLDAP
- **Host / Port:** `localhost:389`
- **Admin DN:** `cn=admin,dc=mintstack,dc=local`
- **Şifre:** `admin`
