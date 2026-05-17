# İşletim Rehberi — Güvenlik, Dağıtım ve Yapılandırma

## 1. Güvenlik Mimarisi

```
┌────────────────────────────────────────────────────────┐
│ Katman 1: Ağ — Nginx tek giriş, iç portlar 127.0.0.1  │
├────────────────────────────────────────────────────────┤
│ Katman 2: Kimlik — Keycloak OAuth2/OIDC, JWT, 2FA     │
├────────────────────────────────────────────────────────┤
│ Katman 3: Yetki — Spring Security RBAC (USER, ADMIN)  │
├────────────────────────────────────────────────────────┤
│ Katman 4: Uygulama — Rate limiting, CORS, validasyon  │
├────────────────────────────────────────────────────────┤
│ Katman 5: Veri — Kafka SASL, Redis şifre, DB izolasyon│
└────────────────────────────────────────────────────────┘
```

### OAuth2/OIDC Akışı

1. Frontend → Keycloak `login-required` (PKCE S256)
2. Kullanıcı giriş + 2FA (TOTP) doğrulama
3. JWT access + refresh token alınır
4. API isteklerinde `Authorization: Bearer <token>`
5. Backend JWK Set ile imza doğrular, `realm_access.roles` ile yetki kontrol

### Rol Yapısı

| Rol | Yetki |
|---|---|
| `USER` | Portföy, piyasa izleme, alarm, izleme listesi; API key yönetimi yok |
| `ADMIN` | Tüm USER + admin panel, kullanıcı yönetimi, simülasyon, API key/backfill yönetimi |

---

## 2. Secret Yönetimi

- **Asla** repoya commit etmeyin — `.env` dosyası `.gitignore`'da
- `.env.example` kopyalayarak `.env` oluşturun
- Güçlü secret üretimi: `openssl rand -base64 24`

| Secret | Kullanım | Minimum |
|---|---|---|
| `POSTGRES_PASSWORD` | Veritabanı | 24 byte base64 |
| `REDIS_PASSWORD` | Cache | 24 byte base64 |
| `KEYCLOAK_ADMIN_PASSWORD` | Admin konsolu | 32 byte base64 |
| `KAFKA_SASL_PASSWORD` | Kafka auth | 24 byte base64 |
| `OPENSEARCH_INITIAL_ADMIN_PASSWORD` | Log arama | 8+ karakter |
| `ALPHA_VANTAGE_API_KEY` / `FINNHUB_API_KEY` | Dış API | API key |

---

## 3. 2FA (TOTP) Kurulumu

1. Keycloak admin → `mintstack-finance` realm
2. `Authentication > Policies > OTP Policy` — Type: `totp`, Digits: `6`, Period: `30`
3. `Authentication > Required Actions` — `Configure OTP` → Enabled + Default
4. `Authentication > Flows > Browser` — `OTP Form` → REQUIRED
5. `Realm Settings > Sessions` — `Remember Me` aktif

---

## 4. Dağıtım Profilleri

### Dev (Tam Stack — 15 servis, ~8 GB RAM)

```bash
docker compose up -d
```

### Lightweight Dev (6 servis, ~4 GB RAM)

```bash
docker compose -f docker-compose.light.yml up -d
```

### Production

```bash
docker compose -f docker-compose.prod.yml up -d
```

Production farkları: Docker Secrets, ağ segmentasyonu, kaynak limitleri, backend replica.

### Servis Bağımlılık Sırası

```
PostgreSQL → Keycloak → Backend → Frontend → Nginx
             Redis ──┘     └── Kafka → Logstash
OpenSearch → OTEL, Dashboards
Prometheus → Grafana, AlertManager
```

---

## 5. Backup & Restore

```bash
# Bash
./docker/backup/pg_backup.sh dev ./backups 7
./docker/backup/pg_restore.sh dev ./backups/<dosya>.sql.gz

# PowerShell
.\docker\backup\pg_backup.ps1 -Mode dev -BackupDir .\backups -RetentionDays 7

# Manuel
docker exec mintstack-postgres pg_dump -U mintstack -d mintstack_finance > backup.sql
```

---

## 6. Üretim Kontrol Listesi

- [ ] Tüm secret'lar güçlü ve benzersiz
- [ ] TLS/HTTPS aktif (Nginx SSL termination)
- [ ] İç portlar dışarıya kapalı (127.0.0.1)
- [ ] 2FA aktif
- [ ] CORS origin'leri yalnızca beklenen domain
- [ ] Alarm eşikleri aktif (Prometheus/AlertManager)
- [ ] Backup rutini otomatik ve test edilmiş

---

## 7. Sorun Giderme

| Sorun | Çözüm |
|---|---|
| Backend başlamıyor | `docker compose ps postgres` — DB healthcheck |
| Keycloak hatası | `docker logs mintstack-keycloak` — realm import süresi 60-90sn |
| Frontend auth hatası | Keycloak realm, client, CORS ayarları |
| Kafka bağlantısı | SASL password eşleşmesi kontrol |
| OpenSearch başlamıyor | Linux: `vm.max_map_count` ayarı |

---

## 8. Kullanici Fiyat Alarmlari ve Alertmanager

Kullanici fiyat alarmi ile Prometheus Alertmanager farkli mekanizmalardir.

Kullanici fiyat alarmi:

- Kullanici alarmi uygulama UI veya `/api/v1/alerts` endpointi uzerinden olusturur.
- Backend fiyat guncellemelerinde alarm kosulunu kontrol eder.
- Tetiklenen alarm pasife alinir ve bildirim event'i uretilir.
- Uygulama ici bildirim DB'ye kaydedilir ve WebSocket hattina gonderilir.
- SMTP aktifse e-posta gonderimi denenir.
- Sesli alarm/browser push bu surumde tam bagli degildir.

Alertmanager:

- Prometheus kurallarindan gelen sistem alarmlarini yonetir.
- Backend down, 5xx hata orani, latency, JVM heap, DB pool ve external API hata oranlarini izler.
- Alarm gruplama, tekrar azaltma, inhibit ve receiver routing yapar.
- Webhook receiver: `http://backend:8080/api/v1/admin/webhooks/alerts`.

## 9. Gecmis Veri Backfill

Admin/Ayarlar icinden gecmis veri backfill calistirilabilir.

- Periyot secenekleri: 7 gun, 30 gun, 90 gun, 1 yil.
- Enstruman tipleri: hisse, fon, doviz, tahvil/bono, VIOP.
- Cikti: `price_history` tablosu.
- Var olan veri veya optimistic lock conflict durumlari toplu isi tamamen bozmadan skip/retry mantigi ile ele alinir.

Operasyon notu:

- Backfill uzun surebilir; UI loading durumu gosterir.
- Production'da once kucuk periyot ve dar enstruman tipiyle test edilmelidir.
- TEFAS/NAV verileri gun sonu mantigi ile degerlendirilmelidir.

## 10. CI/Flyway Sorun Giderme

Bos CI veritabaninda `flyway:validate` tek basina calistirilmaz. Bos DB'de schema history yokken tum migration'lar pending gorunur ve validate fail eder.

Dogru sira:

```bash
./mvnw -B -ntp org.flywaydb:flyway-maven-plugin:10.17.0:migrate
./mvnw -B -ntp org.flywaydb:flyway-maven-plugin:10.17.0:validate
```

GitHub Actions bu nedenle once `migrate`, sonra `validate` calistirir.
