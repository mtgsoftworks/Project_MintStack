# Keycloak 2FA ve Remember Me Kurulumu

## 1. 2FA (OTP) Aktiflestirme

1. Keycloak admin paneline girin.
2. `mintstack-finance` realm'ini secin.
3. `Authentication > Policies > OTP Policy`
   - OTP Type: `totp`
   - Digits: `6`
   - Period: `30`
4. `Authentication > Required Actions`
   - `Configure OTP` -> Enabled + Default Action
5. `Authentication > Flows > Browser`
   - `OTP Form` adimini `REQUIRED` yapin.

## 2. Remember Me Ayari

1. `Realm Settings > Sessions`
   - `Remember Me` aktif edin.
   - SSO idle/max surelerini politika ile uyumlu tanimlayin.
2. `Clients > finance-frontend > Settings`
   - Standard Flow aktif olsun.

## 3. Dogrulama

- Test kullanicisiyla login olun.
- OTP kurulumu zorunlu ekraninin geldigini kontrol edin.
- Remember Me secenegiyle tekrar giriste davranisi test edin.

## 4. Sorun Giderme

- OTP cihaz kaybi durumunda kullanicinin OTP credential'i admin panelden sifirlanabilir.
- Saat farki problemi varsa sunucu ve cihaz NTP senkronunu kontrol edin.
