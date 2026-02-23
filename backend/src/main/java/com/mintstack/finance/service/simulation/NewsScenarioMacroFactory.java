package com.mintstack.finance.service.simulation;

import com.mintstack.finance.dto.simulation.NewsScenario;
import com.mintstack.finance.dto.simulation.NewsScenario.ImpactDirection;
import com.mintstack.finance.dto.simulation.NewsScenario.NewsType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
class NewsScenarioMacroFactory {

    private final Random random = new Random();

    NewsScenario generateMacroNews() {
        boolean positive = random.nextDouble() < 0.5;
        ImpactDirection direction = positive ? ImpactDirection.POSITIVE : ImpactDirection.NEGATIVE;
        double impact = 0.005 + random.nextDouble() * 0.02;
        if (!positive) {
            impact = -impact;
        }

        String title;
        String content;
        List<String> affectedSectors;

        int variant = random.nextInt(6);
        switch (variant) {
            case 0 -> {
                double rate = 40.0 + random.nextDouble() * 10;
                title = "TCMB Faiz Karari Aciklandi";
                content = String.format(
                    "TCMB politika faizini %%%.2f olarak belirledi. Piyasalar karari %s degerlendiriyor.",
                    rate,
                    positive ? "olumlu" : "ihtiyatli"
                );
                affectedSectors = List.of("BANKA", "HOLDING");
            }
            case 1 -> {
                double inflation = 35.0 + random.nextDouble() * 20;
                title = "TUİK Enflasyon Verileri";
                content = String.format("Aylik enflasyon %%%.2f olarak aciklandi. Beklentiler %s yonlu.", inflation, positive ? "asagi" : "yukari");
                affectedSectors = List.of("BANKA", "PERAKENDE", "GYO");
            }
            case 2 -> {
                title = "Fed Toplanti Tutanaklari";
                content = String.format("Fed tutanaklari yayinlandi. %s sinyaller piyasayi etkiliyor.", positive ? "Guvercin" : "Sahin");
                affectedSectors = List.of("HAVACILIK", "OTOMOTIV", "TEKNOLOJI");
            }
            case 3 -> {
                title = "ABD Tarife Kararlari";
                content = String.format("Yeni tarife kararlari piyasada %s etki olusturuyor.", positive ? "olumlu" : "olumsuz");
                affectedSectors = List.of("OTOMOTIV", "METAL", "KIMYA");
            }
            case 4 -> {
                double growth = -2.0 + random.nextDouble() * 6;
                title = "GSYH Buyume Verileri";
                content = String.format(
                    "GSYH buyume verisi %%%.2f. Ekonomik aktivite %s.",
                    growth,
                    growth > 0 ? "genisliyor" : "daraliyor"
                );
                direction = growth > 0 ? ImpactDirection.POSITIVE : ImpactDirection.NEGATIVE;
                impact = growth > 0 ? Math.abs(impact) : -Math.abs(impact);
                affectedSectors = List.of("HOLDING", "BANKA", "INSAAT");
            }
            default -> {
                double unemployment = 8.0 + random.nextDouble() * 5;
                title = "Issizlik Oranlari";
                content = String.format(
                    "Issizlik orani %%%.2f olarak raporlandi. %s piyasayi etkiliyor.",
                    unemployment,
                    positive ? "Dusus" : "Yukselis"
                );
                affectedSectors = List.of("PERAKENDE", "HOLDING");
            }
        }

        return buildScenario(title, content, NewsType.MACRO_NEWS, affectedSectors, impact, direction, 60 + random.nextInt(120));
    }

    NewsScenario generateGeopoliticalNews() {
        boolean positive = random.nextDouble() < 0.4;
        ImpactDirection direction = positive ? ImpactDirection.POSITIVE : ImpactDirection.NEGATIVE;
        double impact = 0.01 + random.nextDouble() * 0.04;
        if (!positive) {
            impact = -impact;
        }

        String title;
        String content;
        List<String> affectedSectors;

        int variant = random.nextInt(5);
        switch (variant) {
            case 0 -> {
                title = "Bolgesel Jeopolitik Gerilimler";
                content = "Bolgesel gelismeler enerji fiyatlari ve tedarik zincirleri uzerinde baski olusturuyor.";
                affectedSectors = List.of("PETROL", "HAVACILIK", "METAL");
            }
            case 1 -> {
                title = "Ticaret Anlasmasi Muzakereleri";
                content = "Ticaret anlasmasi muzakerelerinde ilerleme kaydedildi, ihracat odakli sektorlerde beklenti olustu.";
                affectedSectors = List.of("OTOMOTIV", "KIMYA", "TEKNOLOJI");
            }
            case 2 -> {
                title = "AB-Turkiye Iliskileri";
                content = "AB ile iliskilerde yeni gelismeler ticaret hacmini etkileyebilir.";
                affectedSectors = List.of("OTOMOTIV", "TEKSTIL", "METAL");
            }
            case 3 -> {
                title = "Kuresel Tedarik Zinciri Sorunlari";
                content = "Uluslararasi tedarik zincirindeki aksama lojistik maliyetlerini etkiliyor.";
                affectedSectors = List.of("OTOMOTIV", "TEKNOLOJI", "KIMYA");
            }
            default -> {
                title = "Doviz Piyasasi Dalgalanmasi";
                content = "Kuresel para birimlerinde oynaklik artisinin sermaye akislarini etkiledigi gozlendi.";
                affectedSectors = List.of("BANKA", "HAVACILIK", "GYO");
            }
        }

        return buildScenario(title, content, NewsType.GEOPOLITICAL, affectedSectors, impact, direction, 90 + random.nextInt(180));
    }

    NewsScenario generateCentralBankNews() {
        boolean positive = random.nextDouble() < 0.5;
        ImpactDirection direction = positive ? ImpactDirection.POSITIVE : ImpactDirection.NEGATIVE;
        double impact = 0.01 + random.nextDouble() * 0.025;
        if (!positive) {
            impact = -impact;
        }

        String title;
        String content;
        List<String> affectedSectors;

        int variant = random.nextInt(5);
        switch (variant) {
            case 0 -> {
                double rate = 42.5 + random.nextDouble() * 10;
                title = "TCMB Faiz Karari";
                content = String.format("TCMB politika faizini %%%.2f olarak belirledi. Beklentiler %s.", rate, positive ? "asildi" : "karsilanmadi");
                affectedSectors = List.of("BANKA", "HOLDING", "GYO");
            }
            case 1 -> {
                title = "TCMB Rezerv Verileri";
                content = String.format("Merkez Bankasi rezervlerinde %s goruluyor.", positive ? "artis" : "azalis");
                affectedSectors = List.of("BANKA", "HAVACILIK");
            }
            case 2 -> {
                title = "Fed Faiz Karari";
                content = String.format("Fed faiz karari sonrasi %s sinyaller fiyatlaniyor.", positive ? "guvercin" : "sahin");
                affectedSectors = List.of("HAVACILIK", "OTOMOTIV", "TEKNOLOJI");
            }
            case 3 -> {
                title = "ECB Para Politikasi";
                content = String.format("ECB para politikasi kararinin euro bolgesi etkisi %s.", positive ? "olumlu" : "ihtiyatli");
                affectedSectors = List.of("OTOMOTIV", "HAVACILIK", "TEKNOLOJI");
            }
            default -> {
                title = "TCMB Enflasyon Raporu";
                content = String.format("Yil sonu enflasyon tahminleri %s revize edildi.", positive ? "asagi" : "yukari");
                affectedSectors = List.of("BANKA", "PERAKENDE", "GYO");
            }
        }

        return buildScenario(title, content, NewsType.CENTRAL_BANK, affectedSectors, impact, direction, 60 + random.nextInt(120));
    }

    private NewsScenario buildScenario(
        String title,
        String content,
        NewsType type,
        List<String> affectedSectors,
        double impact,
        ImpactDirection direction,
        int duration
    ) {
        List<String> affectedSymbols = getSymbolsForSectors(affectedSectors);
        String source = NewsScenarioCatalog.SOURCES.get(random.nextInt(NewsScenarioCatalog.SOURCES.size()));

        return NewsScenario.builder()
            .title(title)
            .content(content)
            .summary(content)
            .type(type)
            .source(source)
            .affectedSectors(affectedSectors)
            .affectedSymbols(affectedSymbols)
            .impactPercent(impact)
            .direction(direction)
            .durationMinutes(duration)
            .timestamp(LocalDateTime.now())
            .build();
    }

    private List<String> getSymbolsForSectors(List<String> sectors) {
        List<String> symbols = new ArrayList<>();
        for (String sector : sectors) {
            List<String> sectorSymbols = NewsScenarioCatalog.SECTOR_SYMBOLS.get(sector);
            if (sectorSymbols != null) {
                symbols.addAll(sectorSymbols);
            }
        }
        return symbols;
    }
}
