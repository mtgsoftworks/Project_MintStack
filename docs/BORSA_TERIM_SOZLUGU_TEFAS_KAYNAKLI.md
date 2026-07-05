# Borsa Terim Sozlugu (TEFAS Kaynakli) - Ic Kullanim

## Kapsam ve Not
Bu dokuman sadece ekip ic kullanimi icindir. Uygulama son kullanici arayuzunde birebir gosterilmez.
Amaç: BIST, TEFAS, fon, emir, takas, turev ve oran terimlerini tek bir referans dokumanda toplamak.

## Birincil Kaynaklar
- TEFAS resmi: https://www.tefas.gov.tr/
- TEFAS fon veri endpointi: `POST /api/funds/fonGnlBlgSiraliGetir`
- Borsa Istanbul resmi: https://www.borsaistanbul.com/
- MKK/Takas altyapisi: https://www.mkk.com.tr/
- TCMB kurlar: https://www.tcmb.gov.tr/kurlar

## Sistem Baglantisi
- Migration seed: `backend/src/main/resources/db/migration/V20__glossary_runtime_tefas.sql`
- Public API: `GET /api/v1/glossary`, `GET /api/v1/glossary/{slug}`
- Admin API: `POST /api/v1/admin/glossary`, `PUT /api/v1/admin/glossary/{id}`, `DELETE /api/v1/admin/glossary/{id}`

## Terimler

### 1) Piyasa Temel Terimleri
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| Borsa | Borsa | Menkul kiymetlerin organize piyasada alis-satisi | Borsa Istanbul |
| BIST | Borsa | Borsa Istanbul kisaltmasi | Borsa Istanbul |
| Seans | Islem | Piyasanin acik oldugu islem zaman dilimi | Borsa Istanbul |
| Acilis Seansi | Islem | Acilis fiyatinin olustugu surec | Borsa Istanbul |
| Kapanis Seansi | Islem | Kapanis fiyatinin olustugu surec | Borsa Istanbul |
| Fiyat Adimi | Islem | Fiyatin artip azalabilecegi minimum kademe | Borsa Istanbul |
| Lot | Islem | Islemdeki standart miktar birimi | Borsa Istanbul |
| Likidite | Islem | Varligin hizli ve dusuk maliyetle nakde donusmesi | Borsa Istanbul |
| Spread | Islem | Alis-satis kotasyon farki | Borsa Istanbul |
| Derinlik | Islem | Emir kademelerinde bekleyen hacim dagilimi | Borsa Istanbul |
| Volatilite | Risk | Fiyat oynakligi seviyesi | Finans literaturu |
| Hacim | Islem | Belirli surede el degistiren toplam miktar | Borsa Istanbul |
| Piyasa Degeri | Sirket | Sirketin borsa degerlemesi (fiyat x pay sayisi) | Borsa Istanbul |
| Serbest Dolasim | Sirket | Piyasada fiilen islem gorebilen pay oran/miktari | Borsa Istanbul |
| AOFM | Islem | Agirlikli ortalama fiyat | Borsa Istanbul |
| Tavan Fiyat | Islem | Gunluk ust fiyat limiti | Borsa Istanbul |
| Taban Fiyat | Islem | Gunluk alt fiyat limiti | Borsa Istanbul |

### 2) Emir Turleri ve Islem Akisi
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| Piyasa Emri | Emir | O anki en iyi fiyattan gerceklesme istegi | Borsa Istanbul |
| Limit Emir | Emir | Belirlenen fiyat veya daha iyi fiyattan gerceklesme | Borsa Istanbul |
| Stop Emir | Emir | Tetik fiyat gorulunce aktiflenen emir | Borsa Istanbul |
| Stop-Limit | Emir | Tetikte limit emre donusen emir | Borsa Istanbul |
| Kalani Iptal Et (IOC) | Emir | Aninda karsilanan kisim gerceklesir, kalani iptal olur | Borsa Istanbul |
| Tum ya da Hic (FOK) | Emir | Tamami aninda gerceklesmezse iptal olur | Borsa Istanbul |
| Gunluk Emir | Emir | Seans sonunda gecerliligi biter | Borsa Istanbul |
| Iptale Kadar Gecerli (GTC) | Emir | Kullanici iptal edene kadar gecerli emir | Araci kurum uygulamalari |
| Eslesme | Emir | Alis ve satis emirlerinin fiyat/miktar bazli karsilasmasi | Borsa Istanbul |
| Kismen Gerceklesme | Emir | Emrin bir bolumunun dolmasi | Borsa Istanbul |
| Gerceklesen Miktar | Emir | Dolan lot/adet | Borsa Istanbul |
| Bekleyen Emir | Emir | Henuz eslesmemis aktif emir | Borsa Istanbul |
| Emir Defteri | Emir | Tum aktif emirlerin fiyat kademeleri | Borsa Istanbul |
| Emir Iptali | Emir | Aktif emrin geri cekilmesi | Borsa Istanbul |
| Emir Revizesi | Emir | Fiyat/miktar vb alanlarda degisiklik | Borsa Istanbul |

### 3) Takas, Saklama ve Operasyon
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| Takas | Operasyon | Islem sonrasi para ve menkul kiymet yukumluluklerinin yerine getirilmesi | Takasbank |
| Takas Tarihi | Operasyon | Islemden sonra nihai devir gunu | Takasbank |
| T+1 / T+2 | Operasyon | Islem gununu takip eden 1/2 is gunu takas dongusu | Takasbank |
| Saklama | Operasyon | Varliklarin MKK/Takas sisteminde korunmasi | MKK |
| Virman | Operasyon | Hesaplar arasi kiymet transferi | MKK |
| Nakit Bloke | Operasyon | Islem/teminat icin ayrilan nakit | Takasbank |
| Menkul Bloke | Operasyon | Teminat/taahhut amacli kilitlenen kiymet | Takasbank |
| Komisyon | Maliyet | Araci kurum islem ucreti | Araci kurum |
| BSMV | Maliyet | Finansal islem kaynakli vergi kalemi | Mevzuat |
| Islem Maliyeti | Maliyet | Komisyon+vergi+ucret toplam etkisi | Finans literaturu |
| Slipaj | Maliyet | Beklenen ve gerceklesen fiyat farki | Piyasa uygulamasi |
| Kurum Takas Limiti | Risk | Kurum bazli acik pozisyon/takas limiti | Araci kurum |

### 4) Endeks ve Gosterge Terimleri
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| Endeks | Endeks | Secili varlik sepetinin toplu performans gostergesi | Borsa Istanbul |
| BIST 100 | Endeks | BIST'te one cikan 100 paydan olusan temel endeks | Borsa Istanbul |
| BIST 30 | Endeks | Vadeli islemlerde de referans olan buyuk 30 pay endeksi | Borsa Istanbul |
| BIST Temettu | Endeks | Temettu odeme etkisini iceren endeks | Borsa Istanbul |
| Sektor Endeksi | Endeks | Banka, sanayi vb sektor bazli endeks | Borsa Istanbul |
| Agirlik | Endeks | Bir varligin endekse katkisini belirleyen pay | Borsa Istanbul |
| Serbest Dolasim Agirlikli | Endeks | Endekste serbest dolasima gore agirliklandirma | Borsa Istanbul |
| Baz Deger | Endeks | Endeksin referans baslangic degeri | Borsa Istanbul |
| Rebalans | Endeks | Endeks kapsam/agirlilik guncelleme donemi | Borsa Istanbul |

### 5) TEFAS ve Fon Terimleri
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| TEFAS | Fon | Turkiye Elektronik Fon Alim Satim Platformu | TEFAS |
| Fon Kodu | Fon | Fonun benzersiz islem kodu | TEFAS |
| Fon Unvani | Fon | Fonun resmi tam adi | TEFAS |
| Fon Fiyati | Fon | Pay basina birim fiyat | TEFAS |
| Fon Toplam Degeri | Fon | Fonun toplam portfoy degeri | TEFAS |
| Tedavuldeki Pay Sayisi | Fon | Dolasimdaki toplam fon payi | TEFAS |
| Kisi Sayisi | Fon | Fonda pay sahibi yatirimci sayisi | TEFAS |
| Portfoy Buyuklugu | Fon | Fon varliklarinin toplam buyuklugu | TEFAS |
| Borsa Bulten Fiyati | Fon | Borsa bulten referans fiyat alani | TEFAS |
| Fon Turu | Fon | Hisse, borclanma, para piyasasi, karma vb | TEFAS |
| Fon Kategori | Fon | Serbest, katilim, degisken vb kategori | TEFAS |
| BYF | Fon | Borsa Yatirim Fonu (ETF) | TEFAS/BIST |
| Yatirim Fonu | Fon | Portfoyu profesyonelce yonetilen ortak havuz | SPK/TEFAS |
| Katilim Fon | Fon | Faizsiz finans prensibine uygun fon | TEFAS |
| Para Piyasasi Fonu | Fon | Kisa vadeli dusuk riskli araclara yatirim yapan fon | TEFAS |
| Borclanma Araclari Fonu | Fon | Tahvil/bono agirlikli fon | TEFAS |
| Hisse Senedi Yogun Fon | Fon | Hisse payi yuksek fon sinifi | TEFAS |
| Fon Isletim Gideri | Fon | Fon yonetim ve operasyon giderleri | KAP/IZAHNAME |
| Toplam Gider Orani | Fon | Fon toplam maliyet oran gostergesi | KAP/IZAHNAME |
| Risk Degeri (1-7) | Fon | Fonun standart risk skoru | TEFAS/KID |
| Alis Valoru | Fon | Alis talimati sonrasi paya donus gunu | TEFAS |
| Satis Valoru | Fon | Satis talimati sonrasi nakde donus gunu | TEFAS |
| Fon Dagilim Raporu | Fon | Fon varlik kompozisyonu | KAP/TEFAS |
| Izahname | Fon | Fonun hukuki ve operasyonel temel dokumani | KAP |

### 6) Tahvil, Bono ve Faiz Terimleri
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| Tahvil | Sabit Getiri | 1 yildan uzun vadeli borclanma araci | Hazine/SPK |
| Bono | Sabit Getiri | 1 yildan kisa vadeli borclanma araci | Hazine/SPK |
| Kupon | Sabit Getiri | Donemsel faiz odemesi | Sabit getiri literaturu |
| Kupon Orani | Sabit Getiri | Nominale gore kupon faiz orani | Sabit getiri literaturu |
| Vade Sonu | Sabit Getiri | Anaparanin geri odendigi tarih | Sabit getiri literaturu |
| Getiri (Yield) | Sabit Getiri | Yatirimin beklenen faiz/geri donus orani | Sabit getiri literaturu |
| Bilesik Faiz | Sabit Getiri | Faizin faiz uretmesiyle hesaplanan toplam getiri | Finans literaturu |
| Durasyon | Sabit Getiri | Faiz degisimine duyarlilik olcusu | Sabit getiri literaturu |
| Konveksite | Sabit Getiri | Durasyonun ikinci derece duzeltmesi | Sabit getiri literaturu |

### 7) VIOP ve Turev Terimleri
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| VIOP | Turev | Vadeli Islem ve Opsiyon Piyasasi | Borsa Istanbul |
| Vadeli Islem Sozlesmesi | Turev | Belirli tarihte belirli fiyattan alim-satim taahhudu | Borsa Istanbul |
| Opsiyon Sozlesmesi | Turev | Hak verir, yukumluluk vermez (alim/satim hakki) | Borsa Istanbul |
| Kullanim Fiyati | Turev | Opsiyonda hakkin kullanildigi fiyat | Borsa Istanbul |
| Vade Tarihi | Turev | Sozlesmenin sona erdigi tarih | Borsa Istanbul |
| Uzun Pozisyon | Turev | Fiyatin yukselecegi beklentisiyle alinmis pozisyon | Turev literaturu |
| Kisa Pozisyon | Turev | Fiyatin dusecegi beklentisiyle alinmis pozisyon | Turev literaturu |
| Baslangic Teminati | Turev | Pozisyon acarken yatirilmasi gereken minimum teminat | Takasbank |
| Surdurme Teminati | Turev | Pozisyonu acik tutmak icin gereken asgari teminat | Takasbank |
| Teminat Tamamlama Cagrisi | Turev | Teminat seviyesi dusunce yapilan ekleme talebi | Takasbank |
| Uzlasma Fiyati | Turev | Gun sonu degerleme/kar-zarar hesap fiyati | Borsa Istanbul |
| Mark-to-Market | Turev | Pozisyonun gunluk piyasa degeriyle yeniden hesaplanmasi | Turev literaturu |
| Kaldirac | Turev | Kucuk teminatla buyuk pozisyon acabilme etkisi | Turev literaturu |

### 8) Sirket Analizi ve Carpansal Terimler
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| PD/DD | Carpansal | Piyasa Degeri / Defter Degeri orani | Finansal analiz |
| F/K | Carpansal | Fiyat / Kazanc orani | Finansal analiz |
| FD/FAVOK | Carpansal | Firma Degeri / FAVOK orani | Finansal analiz |
| Net Kar Marji | Karlilik | Net kar / satislar | Finansal analiz |
| Brut Kar Marji | Karlilik | Brut kar / satislar | Finansal analiz |
| Ozsermaye Karliligi (ROE) | Karlilik | Net kar / ozkaynak | Finansal analiz |
| Aktif Karlilik (ROA) | Karlilik | Net kar / toplam aktif | Finansal analiz |
| Cari Oran | Likidite | Donen varlik / kisa vadeli borc | Finansal analiz |
| Likidite Orani | Likidite | Nakit benzeri varliklar / kisa vadeli borc | Finansal analiz |
| Net Borc/FAVOK | Borcluluk | Borc yukunun operasyonel kara orani | Finansal analiz |
| Beta | Risk | Hissenin piyasa hareketine gore goreli riski | Finansal analiz |

### 9) Teknik Analiz Terimleri
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| RSI | Teknik | Goreli guc endeksi, asiri alim/satim gostergesi | Teknik analiz |
| MACD | Teknik | Hareketli ortalamalar yakinlasma-iraksama gostergesi | Teknik analiz |
| SMA | Teknik | Basit hareketli ortalama | Teknik analiz |
| EMA | Teknik | Ussel hareketli ortalama | Teknik analiz |
| Bollinger Bantlari | Teknik | Fiyatin oynaklik bandlari | Teknik analiz |
| ATR | Teknik | Ortalama gercek aralik, volatilite olcusu | Teknik analiz |
| Destek | Teknik | Dususte tepki alinmasi beklenen seviye | Teknik analiz |
| Direnc | Teknik | Yukseliste satis baskisi beklenen seviye | Teknik analiz |
| Kirilim | Teknik | Destek/direnc seviyesinin asilarak yeni bolgeye gecis | Teknik analiz |
| Hacim Onayi | Teknik | Fiyat hareketinin hacimle desteklenmesi | Teknik analiz |

### 10) Portfoy, Performans ve Risk Terimleri
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| Portfoy Dagilimi | Portfoy | Varliklarin siniflara gore agirliklari | Portfoy yonetimi |
| Cesitlendirme | Portfoy | Farkli varliklarla risk azaltimi | Portfoy yonetimi |
| Korelasyon | Portfoy | Iki varligin birlikte hareket olcusu | Istatistik |
| Maksimum Dusus (Max Drawdown) | Risk | Zirveden en derin gerileme | Portfoy analizi |
| Sharpe Orani | Risk/Getiri | Birim risk basina ek getiri | Portfoy analizi |
| Sortino Orani | Risk/Getiri | Asagi yonlu risk odakli getiri olcumu | Portfoy analizi |
| VaR | Risk | Belirli guven duzeyinde beklenen en kotu kayip siniri | Risk yonetimi |
| Stres Testi | Risk | Uc senaryolarda portfoy dayaniklilik olcumu | Risk yonetimi |
| Rebalans | Portfoy | Hedef agirliklara geri donmek icin portfoy duzenleme | Portfoy yonetimi |
| Nakit Orani | Portfoy | Portfoyde nakit benzeri varlik payi | Portfoy yonetimi |

### 11) Doviz, Emtia ve Makro Terimler
| Terim | Kategori | Tanim | Kaynak |
|---|---|---|---|
| Doviz Kuru | Doviz | Iki para birimi arasindaki degisim orani | TCMB |
| Alis Kuru | Doviz | Kurumun dovizi aldigi fiyat | TCMB |
| Satis Kuru | Doviz | Kurumun dovizi sattigi fiyat | TCMB |
| Efektif Kur | Doviz | Nakit islem odakli kur tipi | TCMB |
| Capraz Kur | Doviz | Iki para biriminin ucuncu para ile hesabi | Doviz literaturu |
| CDS | Makro Risk | Ulke kredi risk primi gostergesi | Piyasa verisi |
| Politika Faizi | Makro | Merkez bankasi temel faiz orani | TCMB |
| Enflasyon | Makro | Genel fiyat seviyesindeki artis hizi | TUIK/TCMB |
| Resesyon | Makro | Ekonomik aktivitede belirgin daralma donemi | Makroekonomi |
| PMI | Makro | Satin alma yoneticileri endeksi | Makroekonomi |

## Eksik Gorulen/Kritik Ek Terimler (Oncelikli)
Bu liste sozlukte alan aciklarini kapatmak icin onceliklidir:
- Rucan hakki
- Bedelli/bedelsiz sermaye artirimi
- Temettu verimi
- Aciga satis
- Kredili islem
- Volatilite bazli devre kesici
- Piyasa yapici
- Arbitraj
- Repo/Ters repo
- Eurobond

## Guncelleme Kurali
1. Yeni terim ekleme sirasi: TEFAS -> Borsa Istanbul -> TCMB -> MKK/Takasbank -> MintStack domain.
2. Her terimde `source_name` ve `source_url` zorunludur.
3. Uretime almadan once admin glossary API ile staging dogrulamasi yapilir.
4. Seed degisikligi gerekiyorsa migration dosyasi ile kalici hale getirilir.
