# Keycloak 2FA ve "Beni Hatırla" Kurulumu

## 1. 2FA (OTP) Aktifleştirme

1. Keycloak Admin Konsoluna giriş yapın.
2. `mintstack-finance` realm'ini seçin.
3. **Authentication** → **Policies** → **OTP Policy**:
   - OTP Type: `totp`, Algorithm: `SHA1`, Digits: `6`, Period: `30` -> **Save**
4. **Authentication** → **Required Actions**:
   - `Configure OTP` -> **Enabled** ve **Default Action** işaretleyin.
5. **Authentication** → **Flows** → **Browser**:
   - `OTP Form` ekleyin ve **REQUIRED** yapın.

*Kullanıcılar ilk girişte Google Authenticator ile eşleşmek zorundadır.*

## 2. "Beni Hatırla" (Remember Me)

1. **Realm Settings** → **Sessions**:
   - SSO Session Idle / Max sürelerini belirleyin (Örn: 30 / 90 gün).
   - **Remember Me** -> **ON**
2. **Clients** → `finance-frontend` → **Settings**:
   - Standard Flow / Direct Access Grants -> **ON**

## Sorun Giderme

- **Kullanıcı Kilitlendiyse:** Admin konsolundan kullanıcının "Credentials" sekmesinden OTP kaydını silebilirsiniz, tekrar kurması istenir.
