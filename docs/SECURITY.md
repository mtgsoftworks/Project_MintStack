# Guvenlik Rehberi

## 1. Secret Yonetimi

- Secret degerlerini asla repoya commit etmeyin.
- `.env.example` dosyasini kopyalayip `.env` olusturun.
- Tum varsayilan sifreleri degistirin.

Guclu secret uretimi:

```bash
openssl rand -base64 24
openssl rand -base64 32
```

## 2. Uretim Guvenlik Kontrol Listesi

1. Keycloak admin sifresi ve client secret'lar degistirildi.
2. OpenSearch admin sifresi guclu ve unique.
3. Tls/https aktif.
4. Internal portlar disariya kapali.
5. 2FA aktif (`docs/KEYCLOAK_2FA_SETUP.md`).
6. Backup rutini otomatik.
7. Loglarda hassas veri maskeleme kontrol edildi.

## 3. Uygulama Seviyesi Guvenlik

- OAuth2/JWT ile kimlik dogrulama
- Rol bazli endpoint yetkilendirme (`USER`, `ADMIN`)
- Rate limiting ile kotuye kullanim korumasi
- Gateway uzerinden tek giris noktasi

## 4. Operasyonel Guvenlik

- Image guncellemeleri duzenli yapilmali.
- Secret rotasyonu periyodik olmali.
- Alarm esikleri (CPU, memory, hata oranlari) aktif tutulmali.
- Anahtar olaylar icin audit log izleme acik olmali.

## 5. Sik Hatalar

- Varsayilan sifrelerle canli ortama cikmak.
- Disariya acik DB/Redis portu birakmak.
- SSL dogrulamasini kalici olarak kapatmak.
