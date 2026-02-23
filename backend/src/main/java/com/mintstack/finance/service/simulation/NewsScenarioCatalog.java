package com.mintstack.finance.service.simulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class NewsScenarioCatalog {

    static final List<String> SOURCES = List.of(
        "Reuters", "Bloomberg", "Anadolu Ajansi", "DHA", "IHA",
        "Financial Times", "Wall Street Journal", "CNBC", "Bloomberg HT"
    );

    static final Map<String, List<String>> SECTOR_SYMBOLS;
    static final Map<String, String[]> SECTOR_POSITIVE_REASONS;
    static final Map<String, String[]> SECTOR_NEGATIVE_REASONS;
    static final Map<String, String> SYMBOL_TO_COMPANY;
    static final Map<String, String> CRYPTO_SYMBOLS;

    static final String[] QUARTERS = {"1. ceyrek", "2. ceyrek", "3. ceyrek", "4. ceyrek", "yillik"};

    static final String[] CRYPTO_POSITIVE_REASONS = {
        "kurumsal yatirim haberleri", "ETF onay beklentisi", "proje guncellemesi",
        "yeni ortaklik duyurusu", "mainnet lansmani", "staking odullerinde artis",
        "regulasyon acikliginin artmasi", "DeFi TVL artisi", "whale alimlari"
    };

    static final String[] CRYPTO_NEGATIVE_REASONS = {
        "regulasyon endiseleri", "exchange hack haberleri", "whale satislari",
        "proje gecikmeleri", "guvenlik acigi tespiti", "protokol sorunlari",
        "makroekonomik belirsizlik", "likidite sorunlari", "halving sonrasi satis baskisi"
    };

    static {
        Map<String, List<String>> sectorSymbols = new HashMap<>();
        sectorSymbols.put("BANKA", List.of("GARAN", "AKBNK", "YKBNK", "HALKB", "VAKBN", "ISCTR"));
        sectorSymbols.put("HAVACILIK", List.of("THYAO", "PGSUS", "TAVHL"));
        sectorSymbols.put("TEKNOLOJI", List.of("ASELS", "TCELL"));
        sectorSymbols.put("OTOMOTIV", List.of("TOASO", "FROTO"));
        sectorSymbols.put("METAL", List.of("EREGL"));
        sectorSymbols.put("PETROL", List.of("TUPRS", "PETKM"));
        sectorSymbols.put("KIMYA", List.of("SISE", "SASA"));
        sectorSymbols.put("HOLDING", List.of("KCHOL", "SAHOL"));
        sectorSymbols.put("MADEN", List.of("KOZAL"));
        sectorSymbols.put("PERAKENDE", List.of("BIMAS"));
        sectorSymbols.put("TELEKOM", List.of("TCELL", "TTKOM"));
        sectorSymbols.put("GYO", List.of("EKGYO"));
        sectorSymbols.put("INSAAT", List.of("ENKAI", "TAVHL"));
        SECTOR_SYMBOLS = Collections.unmodifiableMap(sectorSymbols);

        Map<String, String[]> positiveReasons = new HashMap<>();
        positiveReasons.put("BANKA", new String[]{"sermaye yeterlilik oranlari beklentileri asti", "kredi buyumesi beklentileri asti", "dijital donusum yatirimlari meyvesini verdi"});
        positiveReasons.put("HAVACILIK", new String[]{"yolcu sayisi rekor kirdi", "yeni hatlar acilacagi duyuruldu", "kur farki gelirleri artti"});
        positiveReasons.put("TEKNOLOJI", new String[]{"yeni savunma sanayii sozlesmesi imzalandi", "ihracat rakamlari beklentileri asti", "Ar-Ge yatirimlari meyvesini verdi"});
        positiveReasons.put("OTOMOTIV", new String[]{"uretim rakamlari rekor kirdi", "yeni model lansmani yapildi", "ihracat beklentileri guclendi"});
        positiveReasons.put("METAL", new String[]{"celik fiyatlari yukselis trendine girdi", "kapasite kullanim orani artti", "yeni ihracat pazarlari acildi"});
        positiveReasons.put("PETROL", new String[]{"rafine marjlari artti", "yeni uretim tesisi devreye alindi", "enerji talebi artti"});
        positiveReasons.put("KIMYA", new String[]{"ham madde maliyetleri azaldi", "yeni urun lansmani yapildi", "kapasite genisletme yatirimlari tamamlandi"});
        positiveReasons.put("HOLDING", new String[]{"portfoy sirketlerinden olumlu sonuc beklentisi olustu", "yeni yatirim kararlari alindi", "temettu dagitiminin artacagi belirtildi"});
        positiveReasons.put("MADEN", new String[]{"altin fiyatlari rekor seviyeye ulasti", "yeni maden sahasi kesfi yapildi", "uretim miktari artti"});
        positiveReasons.put("PERAKENDE", new String[]{"satis rakamlari beklentileri asti", "yeni magaza acilislari yapildi", "market payi artti"});
        positiveReasons.put("TELEKOM", new String[]{"abone sayisi artti", "5G yatirimlari hizlandi", "fiber altyapi yatirimlari meyvesini verdi"});
        positiveReasons.put("GYO", new String[]{"konut satislari artti", "yeni proje lansmani yapildi", "kira gelirleri beklentileri asti"});
        positiveReasons.put("INSAAT", new String[]{"yeni ihale kazanildi", "proje portfoyu genisledi", "yurt disi proje beklentileri artti"});
        SECTOR_POSITIVE_REASONS = Collections.unmodifiableMap(positiveReasons);

        Map<String, String[]> negativeReasons = new HashMap<>();
        negativeReasons.put("BANKA", new String[]{"takip kredileri artti", "KKB oranlari yukseliste", "kar marjlari daraldi"});
        negativeReasons.put("HAVACILIK", new String[]{"yakit maliyetleri artti", "ucus iptalleri yasandi", "dolar kuru riski artti"});
        negativeReasons.put("TEKNOLOJI", new String[]{"ihale beklentileri ertelendi", "ihracat kisitlamalari etkili oldu", "Ar-Ge giderleri artti"});
        negativeReasons.put("OTOMOTIV", new String[]{"cip tedarik sorunu yasandi", "uretim kesintisi beklentisi olustu", "ic pazar talebi azaldi"});
        negativeReasons.put("METAL", new String[]{"enerji maliyetleri artti", "kuresel celik talebi azaldi", "uretim kapasitesi dusuruldu"});
        negativeReasons.put("PETROL", new String[]{"rafine marjlari daraldi", "hammadde tedarik sorunlari yasandi", "enerji talebi azaldi"});
        negativeReasons.put("KIMYA", new String[]{"ham madde maliyetleri artti", "cevre regulasyonlari ek maliyet olusturdu", "kapasite kullanimi azaldi"});
        negativeReasons.put("HOLDING", new String[]{"portfoy sirketlerinden olumsuz sonuc beklentisi olustu", "yatirim deger kayiplari raporlandi", "temettu azaltilmasi gundeme geldi"});
        negativeReasons.put("MADEN", new String[]{"altin fiyatlari dusus trendine girdi", "uretim maliyetleri artti", "cevre regulasyonlari kisitlar getirdi"});
        negativeReasons.put("PERAKENDE", new String[]{"satis rakamlari beklenti altinda kaldi", "magaza kapanislari yapildi", "market payi azaldi"});
        negativeReasons.put("TELEKOM", new String[]{"abone kayiplari yasandi", "altyapi maliyetleri artti", "rekabet baskisi artti"});
        negativeReasons.put("GYO", new String[]{"konut satislari azaldi", "proje iptalleri yasandi", "kira gelirleri beklenti altinda kaldi"});
        negativeReasons.put("INSAAT", new String[]{"ihale kaybedildi", "proje gecikmeleri yasandi", "maliyet artis beklentisi olustu"});
        SECTOR_NEGATIVE_REASONS = Collections.unmodifiableMap(negativeReasons);

        Map<String, String> symbolToCompany = new HashMap<>();
        symbolToCompany.put("GARAN", "Garanti BBVA");
        symbolToCompany.put("AKBNK", "Akbank");
        symbolToCompany.put("THYAO", "Turk Hava Yollari");
        symbolToCompany.put("PGSUS", "Pegasus");
        symbolToCompany.put("TOASO", "Tofas Otomobil");
        symbolToCompany.put("FROTO", "Ford Otosan");
        symbolToCompany.put("ASELS", "Aselsan");
        symbolToCompany.put("TCELL", "Turkcell");
        symbolToCompany.put("EREGL", "Eregli Demir Celik");
        symbolToCompany.put("SISE", "Sisecam");
        symbolToCompany.put("KCHOL", "Koc Holding");
        symbolToCompany.put("SAHOL", "Sabanci Holding");
        symbolToCompany.put("TUPRS", "Tupras");
        symbolToCompany.put("BIMAS", "BIM Magazalar");
        symbolToCompany.put("KOZAL", "Koza Altin");
        symbolToCompany.put("YKBNK", "Yapi Kredi");
        symbolToCompany.put("HALKB", "Halkbank");
        symbolToCompany.put("VAKBN", "Vakifbank");
        symbolToCompany.put("ISCTR", "Is Bankasi C");
        symbolToCompany.put("TAVHL", "TAV Havalimanlari");
        symbolToCompany.put("SASA", "SASA Polyester");
        symbolToCompany.put("PETKM", "Petkim");
        symbolToCompany.put("TTKOM", "Turk Telekom");
        symbolToCompany.put("EKGYO", "Emlak Konut GYO");
        symbolToCompany.put("ENKAI", "Enka Insaat");
        SYMBOL_TO_COMPANY = Collections.unmodifiableMap(symbolToCompany);

        Map<String, String> cryptoSymbols = new HashMap<>();
        cryptoSymbols.put("Bitcoin", "BTC-USD");
        cryptoSymbols.put("Ethereum", "ETH-USD");
        cryptoSymbols.put("Binance Coin", "BNB-USD");
        cryptoSymbols.put("Solana", "SOL-USD");
        cryptoSymbols.put("XRP", "XRP-USD");
        cryptoSymbols.put("Dogecoin", "DOGE-USD");
        cryptoSymbols.put("Cardano", "ADA-USD");
        cryptoSymbols.put("Avalanche", "AVAX-USD");
        cryptoSymbols.put("Polkadot", "DOT-USD");
        cryptoSymbols.put("Chainlink", "LINK-USD");
        CRYPTO_SYMBOLS = Collections.unmodifiableMap(cryptoSymbols);
    }

    private NewsScenarioCatalog() {
    }
}
