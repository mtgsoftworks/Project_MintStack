# Keycloak 2FA (İki Faktörlü Kimlik Doğrulama) Kurulum Rehberi

Bu doküman, MintStack Finance Portal projesinde Keycloak ile 2FA (TOTP tabanlı) kurulumunu adım adım açıklar.

---

## 1. Genel Bakış

### 2FA Nedir?

**İki Faktörlü Kimlik Doğrulama (2FA)**, kullanıcıların hesaba erişim için iki farklı doğrulama yöntemi kullanmasını gerektirir:

1. **Bildiğiniz bir şey**: Kullanıcı adı ve şifre
2. **Sahip olduğunuz bir şey**: Mobil cihazda üretilen tek seferlik kod (OTP)

### Neden Gerekli?

- **Güvenlik**: Şifre sızdırılsa bile hesaba erişim engellenir
- **Uyumluluk**: Finansal uygulamalarda regülasyon gereksinimleri
- **Kurumsal politika**: Hassas verilere erişimde ek doğrulama

### Proje Yapılandırması

| Parametre | Değer |
|-----------|-------|
| Keycloak Sürümü | 26.5.4 |
| Realm | `mintstack-finance` |
| Admin Console | http://localhost:8180 |
| TOTP Algoritması | HmacSHA1 |
| OTP Hanesi | 6 |
| OTP Periyodu | 30 saniye |
| Desteklenen Uygulamalar | Google Authenticator, Microsoft Authenticator, FreeOTP |

### Teslim Demo Notu

`keycloak/realm-export.json` icindeki seed `admin` ve `test` kullanicilarinda `CONFIGURE_TOTP` required action tanimlidir. Bu nedenle kullanici ilk giriste OTP kurulum ekranina yonlendirilir ve TOTP kurulumu sunumda gosterilebilir.

---

## 2. Keycloak Yapılandırması

### 2.1 OTP Policy Ayarları

Realm export (`keycloak/realm-export.json`) içinde OTP politikası şu şekilde tanımlıdır:

```json
{
  "otpPolicyType": "totp",
  "otpPolicyAlgorithm": "HmacSHA1",
  "otpPolicyInitialCounter": 0,
  "otpPolicyDigits": 6,
  "otpPolicyLookAheadWindow": 1,
  "otpPolicyPeriod": 30,
  "otpSupportedApplications": [
    "Google Authenticator",
    "Microsoft Authenticator",
    "FreeOTP"
  ]
}
```

| Ayar | Değer | Açıklama |
|------|-------|----------|
| `otpPolicyType` | totp | Zaman tabanlı OTP (RFC 6238) |
| `otpPolicyAlgorithm` | HmacSHA1 | TOTP hash algoritması |
| `otpPolicyDigits` | 6 | OTP kod uzunluğu |
| `otpPolicyPeriod` | 30 | Kod geçerlilik süresi (saniye) |
| `otpPolicyLookAheadWindow` | 1 | Saat kayması toleransı (önceki/sonraki pencere) |

### 2.2 OTP Policy Değiştirme

1. http://localhost:8180 adresine gidin
2. **admin** kullanıcısı ile giriş yapın
3. **mintstack-finance** realm'ini seçin
4. **Authentication** → **Policies** → **OTP Policy**
5. İstenen değerleri güncelleyin (örn. period, digits)

### 2.3 Browser Flow ve OTP

OTP'yi giriş akışına eklemek için:

1. **Authentication** → **Flows** → **browser**
2. **OTP Form** (veya **Configure OTP**) adımını **Browser Forms** altına sürükleyin
3. **OTP Form**'u **Username Password Form**'dan sonra konumlandırın
4. **Required** veya **Optional** olarak işaretleyin
5. **Save** ile kaydedin

**Zorunlu 2FA için**: OTP Form'u **Required** yapın.

---

## 3. Kullanıcı Kurulumu

### 3.1 İlk Girişte 2FA Kurulumu

Kullanıcı ilk kez 2FA ile giriş yaptığında:

1. Kullanıcı adı ve şifre ile giriş yapın
2. **Configure OTP** ekranı açılır
3. Mobil uygulamayı açın (Google Authenticator, Microsoft Authenticator veya FreeOTP)
4. **QR kodu tarayın** veya **Manuel giriş** ile secret key'i girin
5. Uygulamanın ürettiği 6 haneli kodu girin
6. **Save** ile kaydedin

### 3.2 QR Kod ile Kurulum (Adım Adım)

1. Keycloak, ekranda bir **QR kod** gösterir
2. Telefonunuzda **Google Authenticator** (veya diğer uygulama) açın
3. **+** veya **Hesap ekle** → **QR kod tara**
4. Ekrandaki QR kodu tarayın
5. Uygulama hesabı ekleyecek ve 6 haneli kod üretecek
6. Bu kodu Keycloak ekranına girin
7. **Verify** veya **Save** ile onaylayın

### 3.3 Manuel Secret Key ile Kurulum

QR kod taranamıyorsa:

1. **Can't scan?** veya **Manuel giriş** bağlantısına tıklayın
2. **Account** ve **Key** bilgileri görünür
3. Bu bilgileri uygulamaya manuel girin
4. Üretilen kodu Keycloak'a yazın

### 3.4 Örnek Kullanıcılar

| Kullanıcı | Rol | E-posta |
|-----------|-----|---------|
| admin | admin | admin@mintstack.local |
| test | user | test@mintstack.local |

---

## 4. Yönetici İşlemleri

### 4.1 2FA'yı Tüm Kullanıcılar İçin Zorunlu Yapma

1. **Authentication** → **Required actions**
2. **Configure OTP** satırını bulun
3. **Set as default action** işaretleyin (yeni kullanıcılar için)
4. Mevcut kullanıcılar için: **Required** olarak işaretleyin
5. **Save**

**Alternatif**: Belirli gruplar için zorunlu yapmak üzere **Authentication** → **Flows** → **browser** içinde OTP Form'u **Required** yapın.

### 4.2 Kullanıcı 2FA'sını Sıfırlama

Kullanıcı cihazını kaybettiğinde veya 2FA'ya erişemediğinde:

1. **Users** → İlgili kullanıcıyı seçin
2. **Credentials** sekmesine gidin
3. **OTP** credential'ını bulun
4. **Remove** ile silin
5. Kullanıcı bir sonraki girişte 2FA'yı yeniden yapılandırabilir

### 4.3 Kullanıcıya 2FA Zorunluluğu Gönderme

1. **Users** → Kullanıcıyı seçin
2. **Required user actions** sekmesine gidin
3. **Configure OTP** ekleyin
4. Kullanıcı bir sonraki girişte 2FA kurulumu yapmak zorunda kalır

---

## 5. Sorun Giderme

### 5.1 "Invalid OTP" Hatası

| Olası Neden | Çözüm |
|-------------|-------|
| Saat senkronizasyonu | Telefon ve sunucu saatlerini kontrol edin; NTP kullanın |
| Yanlış kod | Yeni kod üretilene kadar bekleyin (30 sn) |
| Look-ahead penceresi | Keycloak'ta `otpPolicyLookAheadWindow` değerini artırın (örn. 2) |

### 5.2 QR Kod Görünmüyor

- Tarayıcı JavaScript engelini kaldırın
- Farklı tarayıcı deneyin (Chrome, Firefox)
- Manuel secret key ile kurulum yapın

### 5.3 "Required action not set" Hatası

- **Authentication** → **Required actions** → **Configure OTP** etkin mi kontrol edin
- Kullanıcıya **Required user actions** üzerinden **Configure OTP** ekleyin

### 5.4 Keycloak Admin Console'a Erişilemiyor

- Docker: `docker-compose ps keycloak` ile container çalışıyor mu kontrol edin
- Port: Keycloak `8180` portunda yayında (docker-compose: `8180:8080`)
- Log: `docker-compose logs -f keycloak` ile hata mesajlarını inceleyin

### 5.5 Realm Import Sonrası OTP Ayarları

Realm `realm-export.json` ile import edildiğinde OTP policy zaten tanımlıdır. Değişiklik yapmak için Admin Console üzerinden **Authentication** → **Policies** → **OTP Policy** kullanın.

---

## İlgili Dokümantasyon

- [Keycloak Server Administration Guide](https://www.keycloak.org/docs/latest/server_admin/)
- [Keycloak Authentication Flows](https://www.keycloak.org/docs/latest/server_admin/#_authentication-flows)
- [docs/SECURITY.md](./SECURITY.md) - Güvenlik checklist
- [docs/ARCHITECTURE.md](./ARCHITECTURE.md) - Sistem mimarisi
