# MintStack Finance Portal - Güvenlik & Secret Yönetimi

## 1. Secret Üretimi ve Yönetimi

Asla secret'ları koda commit etmeyin. `.env.example` dosyasını kopyalayarak kendi `.env` dosyanızı oluşturun ve tüm şifreleri güçlü, benzersiz değerlerle değiştirin.

**Güçlü Secret Üretmek İçin Komutlar:**

```bash
openssl rand -base64 24   # Database, Redis, Kafka için
openssl rand -base64 32   # Keycloak Admin, JWT vb. için
```

*OpenSearch Admin Şifresi minimum 8 karakter, büyük harf, küçük harf, özel karakter ve rakam içermelidir.*

## 2. Security Checklist (Production)

1. `.env` dosyasındaki tüm varsayılan şifreleri ve API Key'leri değiştirin.
2. Tüm web trafiğini HTTPS üzerinden yönlendirin (örn. Nginx + Let's Encrypt).
3. Veritabanı ve Redis gibi internal servis portlarını firewall ile kapalı tutun (sadece localhost veya docker network içine açık).
4. Keycloak'ta Admin kullanıcısı için 2FA'yı mutlaka aktifleştirin (`docs/KEYCLOAK_2FA_SETUP.md`).
5. OpenSearch'e default credential'lar ile dış bağlantıyı kesin veya şifreyi güçlendirin.
6. Yedek almayı (Backup) unutmayın.
