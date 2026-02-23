package com.mintstack.finance.service.simulation;

import com.mintstack.finance.dto.simulation.NewsScenario;
import com.mintstack.finance.dto.simulation.NewsScenario.ImpactDirection;
import com.mintstack.finance.dto.simulation.NewsScenario.NewsType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScenarioEngine {

    private final NewsScenarioMacroFactory macroFactory;
    private final Random random = new Random();

    public Optional<NewsScenario> generateRandomNews() {
        List<NewsType> availableTypes = List.of(NewsType.values());
        NewsType type = availableTypes.get(random.nextInt(availableTypes.size()));

        return switch (type) {
            case SECTOR_NEWS -> Optional.of(generateSectorNews(getRandomSector()));
            case COMPANY_NEWS -> Optional.of(generateCompanyNews(getRandomSymbol()));
            case MACRO_NEWS -> Optional.of(generateMacroNews());
            case GEOPOLITICAL -> Optional.of(generateGeopoliticalNews());
            case CRYPTO_NEWS -> Optional.of(generateCryptoNews(getRandomCrypto()));
            case EARNINGS -> Optional.of(generateEarningsNews(getRandomSymbol()));
            case CENTRAL_BANK -> Optional.of(generateCentralBankNews());
        };
    }

    public NewsScenario generateSectorNews(String sector) {
        boolean positive = random.nextDouble() < 0.5;
        ImpactDirection direction = positive ? ImpactDirection.POSITIVE : ImpactDirection.NEGATIVE;
        double impact = 0.01 + random.nextDouble() * 0.03;
        if (!positive) {
            impact = -impact;
        }

        String[] reasons = positive
            ? NewsScenarioCatalog.SECTOR_POSITIVE_REASONS.get(sector)
            : NewsScenarioCatalog.SECTOR_NEGATIVE_REASONS.get(sector);
        String reason = reasons != null ? reasons[random.nextInt(reasons.length)] : "gelisme yasandi";

        String title = String.format("%s Sektorunde %s Gelisme", sector, positive ? "Pozitif" : "Negatif");
        String content = String.format(
            "%s sektorunde %s. %s sektor hisselerinde %%%.2f oraninda %s hareket bekleniyor.",
            sector,
            reason,
            sector,
            Math.abs(impact * 100),
            positive ? "yukari" : "asagi"
        );

        List<String> affectedSymbols = NewsScenarioCatalog.SECTOR_SYMBOLS.getOrDefault(sector, List.of());
        String source = NewsScenarioCatalog.SOURCES.get(random.nextInt(NewsScenarioCatalog.SOURCES.size()));

        return NewsScenario.builder()
            .title(title)
            .content(content)
            .summary(content)
            .type(NewsType.SECTOR_NEWS)
            .source(source)
            .affectedSectors(List.of(sector))
            .affectedSymbols(affectedSymbols)
            .impactPercent(impact)
            .direction(direction)
            .durationMinutes(30 + random.nextInt(60))
            .timestamp(LocalDateTime.now())
            .build();
    }

    public NewsScenario generateCompanyNews(String symbol) {
        String companyName = NewsScenarioCatalog.SYMBOL_TO_COMPANY.getOrDefault(symbol, symbol);
        boolean positive = random.nextDouble() < 0.5;
        ImpactDirection direction = positive ? ImpactDirection.POSITIVE : ImpactDirection.NEGATIVE;
        double impact = 0.015 + random.nextDouble() * 0.035;
        if (!positive) {
            impact = -impact;
        }

        String title;
        String content;
        if (positive) {
            String[] positiveEvents = {
                String.format("%s yeni yatirim kararlari aldigini duyurdu", companyName),
                String.format("%s uluslararasi pazarlara acilma plani acikladi", companyName),
                String.format("%s hissesine AL tavsiyesi verildi", companyName),
                String.format("%s yonetiminden olumlu rehberlik aciklandi", companyName),
                String.format("%s yeni is birligi anlasmasi imzaladi", companyName)
            };
            title = String.format("%s: Olumlu Gelisme", companyName);
            content = positiveEvents[random.nextInt(positiveEvents.length)]
                + String.format(". Hissede %%%.2f yukselis bekleniyor.", Math.abs(impact * 100));
        } else {
            String[] negativeEvents = {
                String.format("%s yonetiminden beklenti altinda yonlendirme aciklandi", companyName),
                String.format("%s hissesine SAT tavsiyesi verildi", companyName),
                String.format("%s uretim sorunlari yasadigi raporlandi", companyName),
                String.format("%s yonetim degisikligi haberleri tartisiliyor", companyName),
                String.format("%s yasal sureclerle ilgili belirsizlik var", companyName)
            };
            title = String.format("%s: Dikkat Ceken Haber", companyName);
            content = negativeEvents[random.nextInt(negativeEvents.length)]
                + String.format(". Hissede %%%.2f dusus riski bulunuyor.", Math.abs(impact * 100));
        }

        String source = NewsScenarioCatalog.SOURCES.get(random.nextInt(NewsScenarioCatalog.SOURCES.size()));

        return NewsScenario.builder()
            .title(title)
            .content(content)
            .summary(content)
            .type(NewsType.COMPANY_NEWS)
            .source(source)
            .affectedSectors(getSectorForSymbol(symbol))
            .affectedSymbols(List.of(symbol))
            .impactPercent(impact)
            .direction(direction)
            .durationMinutes(15 + random.nextInt(45))
            .timestamp(LocalDateTime.now())
            .build();
    }

    public NewsScenario generateMacroNews() {
        return macroFactory.generateMacroNews();
    }

    public NewsScenario generateGeopoliticalNews() {
        return macroFactory.generateGeopoliticalNews();
    }

    public NewsScenario generateCryptoNews(String cryptoSymbol) {
        String cryptoName = NewsScenarioCatalog.CRYPTO_SYMBOLS.entrySet().stream()
            .filter((entry) -> entry.getValue().equals(cryptoSymbol))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(cryptoSymbol);

        boolean positive = random.nextDouble() < 0.5;
        ImpactDirection direction = positive ? ImpactDirection.POSITIVE : ImpactDirection.NEGATIVE;
        double impact = 0.02 + random.nextDouble() * 0.05;
        if (!positive) {
            impact = -impact;
        }

        String reason = positive
            ? NewsScenarioCatalog.CRYPTO_POSITIVE_REASONS[random.nextInt(NewsScenarioCatalog.CRYPTO_POSITIVE_REASONS.length)]
            : NewsScenarioCatalog.CRYPTO_NEGATIVE_REASONS[random.nextInt(NewsScenarioCatalog.CRYPTO_NEGATIVE_REASONS.length)];

        String title = String.format("%s: %s Haber Piyasayi Etkiledi", cryptoName, positive ? "Pozitif" : "Negatif");
        String content = String.format(
            "%s ile ilgili %s. %s fiyatinda %%%.2f oraninda %s hareket goruluyor.",
            cryptoName,
            reason,
            cryptoName,
            Math.abs(impact * 100),
            positive ? "yukselis" : "dusus"
        );

        String source = positive ? "CoinDesk" : (random.nextBoolean() ? "CoinDesk" : "CoinTelegraph");

        return NewsScenario.builder()
            .title(title)
            .content(content)
            .summary(content)
            .type(NewsType.CRYPTO_NEWS)
            .source(source)
            .affectedSectors(List.of())
            .affectedSymbols(List.of(cryptoSymbol))
            .impactPercent(impact)
            .direction(direction)
            .durationMinutes(15 + random.nextInt(30))
            .timestamp(LocalDateTime.now())
            .build();
    }

    public NewsScenario generateEarningsNews(String symbol) {
        String companyName = NewsScenarioCatalog.SYMBOL_TO_COMPANY.getOrDefault(symbol, symbol);
        boolean positive = random.nextDouble() < 0.5;
        ImpactDirection direction = positive ? ImpactDirection.POSITIVE : ImpactDirection.NEGATIVE;
        double impact = 0.02 + random.nextDouble() * 0.04;
        if (!positive) {
            impact = -impact;
        }

        String quarter = NewsScenarioCatalog.QUARTERS[random.nextInt(NewsScenarioCatalog.QUARTERS.length)];
        double earnings = 1.0 + random.nextDouble() * 5;
        double expected = earnings * (positive ? 0.9 : 1.1);

        String title = String.format("%s %s Karini Acikladi", companyName, quarter);
        String content = String.format(
            "%s, %s finansal sonuclarini acikladi. Net kar %%%.2f, beklenti %%%.2f. Sonuclar beklentinin %s.",
            companyName,
            quarter,
            earnings,
            expected,
            positive ? "uzerinde" : "altinda"
        );

        String source = NewsScenarioCatalog.SOURCES.get(random.nextInt(NewsScenarioCatalog.SOURCES.size()));

        return NewsScenario.builder()
            .title(title)
            .content(content)
            .summary(content)
            .type(NewsType.EARNINGS)
            .source(source)
            .affectedSectors(getSectorForSymbol(symbol))
            .affectedSymbols(List.of(symbol))
            .impactPercent(impact)
            .direction(direction)
            .durationMinutes(30 + random.nextInt(60))
            .timestamp(LocalDateTime.now())
            .build();
    }

    public NewsScenario generateCentralBankNews() {
        return macroFactory.generateCentralBankNews();
    }

    public List<String> getAffectedSymbols(NewsScenario scenario) {
        if (scenario.getAffectedSymbols() != null && !scenario.getAffectedSymbols().isEmpty()) {
            return scenario.getAffectedSymbols();
        }

        if (scenario.getAffectedSectors() != null && !scenario.getAffectedSectors().isEmpty()) {
            return getSymbolsForSectors(scenario.getAffectedSectors());
        }

        return List.of();
    }

    public double calculatePriceImpact(NewsScenario scenario) {
        if (scenario.getDirection() == ImpactDirection.NEUTRAL) {
            return 0.0;
        }

        double baseImpact = Math.abs(scenario.getImpactPercent());
        double volatilityBonus = random.nextDouble() * 0.005;

        return scenario.getDirection() == ImpactDirection.POSITIVE
            ? baseImpact + volatilityBonus
            : -(baseImpact + volatilityBonus);
    }

    private String getRandomSector() {
        List<String> sectors = new ArrayList<>(NewsScenarioCatalog.SECTOR_SYMBOLS.keySet());
        return sectors.get(random.nextInt(sectors.size()));
    }

    private String getRandomSymbol() {
        List<String> symbols = new ArrayList<>(NewsScenarioCatalog.SYMBOL_TO_COMPANY.keySet());
        return symbols.get(random.nextInt(symbols.size()));
    }

    private String getRandomCrypto() {
        List<String> cryptoSymbols = new ArrayList<>(NewsScenarioCatalog.CRYPTO_SYMBOLS.values());
        return cryptoSymbols.get(random.nextInt(cryptoSymbols.size()));
    }

    private List<String> getSectorForSymbol(String symbol) {
        for (Map.Entry<String, List<String>> entry : NewsScenarioCatalog.SECTOR_SYMBOLS.entrySet()) {
            if (entry.getValue().contains(symbol)) {
                return List.of(entry.getKey());
            }
        }
        return List.of();
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
