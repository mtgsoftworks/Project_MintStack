package com.mintstack.finance.service.simulation;

import com.mintstack.finance.dto.simulation.MarketEvent;
import com.mintstack.finance.dto.simulation.NewsScenario;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.News;
import com.mintstack.finance.entity.NewsCategory;
import com.mintstack.finance.entity.SimulationConfig;
import com.mintstack.finance.entity.SimulationConfig.MarketTrend;
import com.mintstack.finance.entity.SimulationConfig.VolatilityLevel;
import com.mintstack.finance.repository.NewsCategoryRepository;
import com.mintstack.finance.repository.NewsRepository;
import com.mintstack.finance.repository.SimulationConfigRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.service.PriceCacheService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationDataService {
    private static final String SIMULATION_SOURCE_NAME = "Simulasyon";
    private static final String SIMULATION_SOURCE_URL_PREFIX = "https://mintstack.local/simulation-news";

    private final SimulationConfigRepository configRepository;
    private final NewsRepository newsRepository;
    private final NewsCategoryRepository newsCategoryRepository;
    private final UserApiConfigRepository userApiConfigRepository;
    private final PriceSimulationEngine priceEngine;
    private final PriceCacheService priceCacheService;
    private final MarketEventEngine marketEventEngine;
    private final NewsScenarioEngine newsScenarioEngine;
    private final SimulationMarketBootstrapService marketBootstrapService;
    private final SimulationPersistenceService persistenceService;

    private final Map<String, SimulatedStock> stockCache = new ConcurrentHashMap<>();
    private final Map<String, SimulatedStock> bondCache = new ConcurrentHashMap<>();
    private final Map<String, SimulatedStock> fundCache = new ConcurrentHashMap<>();
    private final Map<String, SimulatedStock> viopCache = new ConcurrentHashMap<>();
    private final Map<String, SimulatedCurrency> currencyCache = new ConcurrentHashMap<>();
    private final Map<String, SimulatedIndex> indexCache = new ConcurrentHashMap<>();
    private final Map<String, SimulatedCrypto> cryptoCache = new ConcurrentHashMap<>();
    private static final long MIN_SIMULATION_NEWS_INTERVAL_SECONDS = 60L;

    private LocalDate lastTradingDate = null;
    private LocalDateTime lastSimulationHeadlineAt = null;
    private final Random random = new Random();

    @PostConstruct
    public void initializeMarketData() {
        marketBootstrapService.initializeCaches(
            stockCache,
            bondCache,
            fundCache,
            viopCache,
            currencyCache,
            indexCache,
            cryptoCache
        );
        normalizeLegacyScenarioNews();
        ensureBaseMockNewsIfEmpty();
    }

    public boolean isSimulationEnabled() {
        return configRepository.getOrCreateDefault().getIsEnabled();
    }

    public SimulationConfig getConfig() {
        return configRepository.getOrCreateDefault();
    }

    @Transactional
    public SimulationConfig updateConfig(
        Boolean enabled,
        VolatilityLevel volatility,
        Integer updateInterval,
        MarketTrend trend,
        Boolean enableRandomEvents,
        Boolean enableMarketHours
    ) {
        if (Boolean.TRUE.equals(enabled)) {
            boolean hasActiveApi = !userApiConfigRepository
                .findByProviderAndIsActiveTrue(com.mintstack.finance.entity.UserApiConfig.ApiProvider.ALPHA_VANTAGE)
                .isEmpty()
                || !userApiConfigRepository
                .findByProviderAndIsActiveTrue(com.mintstack.finance.entity.UserApiConfig.ApiProvider.YAHOO_FINANCE)
                .isEmpty();

            if (hasActiveApi) {
                throw new IllegalStateException("Simulasyon modunu acmak icin once diger API baglantilarini kapatin.");
            }
        }

        SimulationConfig config = configRepository.getOrCreateDefault();

        if (enabled != null) {
            config.setIsEnabled(enabled);
        }
        if (volatility != null) {
            config.setVolatilityLevel(volatility);
        }
        if (updateInterval != null) {
            config.setUpdateIntervalSeconds(updateInterval);
        }
        if (trend != null) {
            config.setMarketTrend(trend);
        }
        if (enableRandomEvents != null) {
            config.setEnableRandomEvents(enableRandomEvents);
        }
        if (enableMarketHours != null) {
            config.setEnableMarketHours(enableMarketHours);
        }

        return configRepository.save(config);
    }

    public void simulateAllPrices() {
        SimulationConfig config = getConfig();
        if (!config.getIsEnabled()) {
            return;
        }

        marketEventEngine.decayEvents();

        Optional<MarketEvent> newEvent = marketEventEngine.checkForEvent(stockCache);
        if (newEvent.isPresent() && config.getEnableRandomEvents()) {
            log.info("Simulation market event triggered: {}", newEvent.get().getType());
        }

        if (config.getEnableRandomEvents() && random.nextDouble() < 0.005) {
            Optional<MarketEvent> randomEvent = marketEventEngine.generateRandomEvent();
            randomEvent.ifPresent((event) -> log.info("Random simulation event: {}", event.getType()));
        }

        if (marketEventEngine.isTradingHalted("XU100")) {
            return;
        }

        if (config.getEnableMarketHours()) {
            LocalTime now = LocalTime.now(ZoneId.of("Europe/Istanbul"));
            if (now.isBefore(LocalTime.of(10, 0)) || now.isAfter(LocalTime.of(18, 0))) {
                return;
            }
        }

        VolatilityLevel volatility = config.getVolatilityLevel();
        MarketTrend trend = config.getMarketTrend();
        boolean randomEvents = config.getEnableRandomEvents();
        int intervalSeconds = config.getUpdateIntervalSeconds();

        double eventMultiplier = priceEngine.simulateMarketEvent(randomEvents);

        Map<String, Double> newsImpacts = randomEvents ? simulateNewsImpact() : new HashMap<>();
        Map<String, Double> sectorMovements = simulateSectorCorrelation(volatility, trend, intervalSeconds);

        LocalDate today = LocalDate.now();
        boolean isNewDay = lastTradingDate == null || !lastTradingDate.equals(today);
        if (isNewDay) {
            resetDailyValues();
            lastTradingDate = today;
        }

        simulateStocks(volatility, trend, intervalSeconds, eventMultiplier, newsImpacts, sectorMovements);
        simulateBonds(volatility, intervalSeconds);
        simulateFunds(volatility, trend, intervalSeconds);
        simulateViop(volatility, trend, intervalSeconds, eventMultiplier);
        simulateCurrencies(volatility, intervalSeconds);
        simulateIndices(volatility, trend, intervalSeconds, eventMultiplier);
        simulateCryptos(volatility, trend, intervalSeconds, randomEvents);
        maybeGenerateSimulationHeadline(intervalSeconds);
    }

    private Map<String, Double> simulateNewsImpact() {
        Map<String, Double> symbolImpacts = new HashMap<>();

        if (random.nextDouble() < 0.05) {
            Optional<NewsScenario> scenario = newsScenarioEngine.generateRandomNews();
            scenario.ifPresent((value) -> applyNewsScenario(value, symbolImpacts));
        }

        return symbolImpacts;
    }

    private void applyNewsScenario(NewsScenario scenario, Map<String, Double> symbolImpacts) {
        saveScenarioNews(scenario);

        List<String> affectedSymbols = newsScenarioEngine.getAffectedSymbols(scenario);
        double priceImpact = newsScenarioEngine.calculatePriceImpact(scenario);

        for (String symbol : affectedSymbols) {
            symbolImpacts.put(symbol, priceImpact);

            SimulatedStock stock = stockCache.get(symbol);
            if (stock != null) {
                BigDecimal impactMultiplier = BigDecimal.valueOf(1.0 + priceImpact);
                BigDecimal newPrice = stock.getCurrentPrice().multiply(impactMultiplier).setScale(2, RoundingMode.HALF_UP);
                stock.updatePrice(newPrice);
            }

            SimulatedCrypto crypto = cryptoCache.get(symbol);
            if (crypto != null) {
                BigDecimal impactMultiplier = BigDecimal.valueOf(1.0 + priceImpact);
                BigDecimal newPrice = crypto.getCurrentPrice().multiply(impactMultiplier).setScale(2, RoundingMode.HALF_UP);
                crypto.updatePrice(newPrice);
            }
        }
    }

    private void saveScenarioNews(NewsScenario scenario) {
        try {
            List<NewsCategory> categories = newsCategoryRepository.findAll();
            if (categories.isEmpty()) {
                return;
            }

            NewsCategory category = categories.stream()
                .filter((item) -> item.getName().contains("Piyasa") || item.getName().contains("Ekonomi"))
                .findFirst()
                .orElse(categories.get(0));

            News news = News.builder()
                .title(scenario.getTitle())
                .content(scenario.getContent())
                .summary(scenario.getSummary())
                .sourceName(buildSimulationSourceName(scenario.getSource()))
                .sourceUrl(buildScenarioSourceUrl(scenario))
                .category(category)
                .publishedAt(LocalDateTime.now())
                .isPublished(true)
                .viewCount(0L)
                .build();

            newsRepository.save(news);
        } catch (Exception error) {
            log.warn("Scenario news kaydedilemedi: {}", error.getMessage());
        }
    }

    private void maybeGenerateSimulationHeadline(int updateIntervalSeconds) {
        LocalDateTime now = LocalDateTime.now();
        long minInterval = Math.max(MIN_SIMULATION_NEWS_INTERVAL_SECONDS, updateIntervalSeconds * 8L);

        if (lastSimulationHeadlineAt != null
            && now.isBefore(lastSimulationHeadlineAt.plusSeconds(minInterval))) {
            return;
        }

        SimulatedIndex bist = indexCache.get("XU100.IS");
        if (bist == null) {
            bist = indexCache.get("XU100");
        }
        if (bist == null) {
            return;
        }

        List<NewsCategory> categories = ensureDefaultNewsCategories();
        if (categories.isEmpty()) {
            return;
        }

        NewsCategory category = categories.stream()
            .filter((item) -> "piyasa".equalsIgnoreCase(item.getSlug()))
            .findFirst()
            .orElse(categories.get(0));

        BigDecimal price = bist.getCurrentValue().setScale(2, RoundingMode.HALF_UP);
        BigDecimal changePercent = bist.getChangePercent().setScale(2, RoundingMode.HALF_UP);
        String direction = changePercent.compareTo(BigDecimal.ZERO) >= 0 ? "yukselis" : "dusus";
        String timestamp = now.toLocalTime().withNano(0).toString();

        News autoNews = News.builder()
            .title(String.format("Simulasyon BIST 100 guncellemesi: %s", price))
            .summary(String.format("BIST 100 %% %s (%s) hareketi gosteriyor.", changePercent, direction))
            .content(String.format(
                "Simulasyon saat %s itibariyla BIST 100 %s seviyesinde. Gunluk degisim %%%s (%s).",
                timestamp,
                price,
                changePercent,
                direction
            ))
            .sourceName(SIMULATION_SOURCE_NAME)
            .sourceUrl(String.format("%s/xu100-%s", SIMULATION_SOURCE_URL_PREFIX, now))
            .category(category)
            .publishedAt(now)
            .isPublished(true)
            .isFeatured(false)
            .viewCount(0L)
            .build();

        try {
            newsRepository.save(autoNews);
            lastSimulationHeadlineAt = now;
        } catch (Exception error) {
            log.warn("Simulation headline kaydedilemedi: {}", error.getMessage());
        }
    }

    private Map<String, Double> simulateSectorCorrelation(VolatilityLevel volatility, MarketTrend trend, int intervalSeconds) {
        Map<String, Double> sectorMovements = new HashMap<>();

        for (String sector : new String[] { "BANKA", "HAVACILIK", "TEKNOLOJI", "OTOMOTIV", "METAL", "PETROL", "HOLDING" }) {
            double movement = priceEngine.simulateGBM(
                sector,
                BigDecimal.ONE,
                0.01,
                volatility,
                trend,
                intervalSeconds
            ).doubleValue() - 1.0;
            sectorMovements.put(sector, movement);
        }

        return sectorMovements;
    }

    private void simulateStocks(
        VolatilityLevel volatility,
        MarketTrend trend,
        int intervalSeconds,
        double eventMultiplier,
        Map<String, Double> newsImpacts,
        Map<String, Double> sectorMovements
    ) {
        for (Map.Entry<String, SimulatedStock> entry : stockCache.entrySet()) {
            String symbol = entry.getKey();
            SimulatedStock stock = entry.getValue();

            double sectorMovement = 0.0;
            if (stock.getSector() != null && sectorMovements.containsKey(stock.getSector())) {
                sectorMovement = sectorMovements.get(stock.getSector());
            }

            double newsImpact = 1.0;
            if (newsImpacts.containsKey(symbol)) {
                newsImpact = 1.0 + newsImpacts.get(symbol);
            }

            BigDecimal basePrice = priceEngine.simulateGBM(
                symbol,
                stock.getCurrentPrice(),
                stock.getBaseVolatility(),
                volatility,
                trend,
                intervalSeconds
            );

            BigDecimal newPrice = basePrice
                .multiply(BigDecimal.valueOf(1.0 + sectorMovement))
                .multiply(BigDecimal.valueOf(newsImpact))
                .multiply(BigDecimal.valueOf(eventMultiplier))
                .setScale(2, RoundingMode.HALF_UP);

            BigDecimal previousClose = stock.getCurrentPrice();
            stock.updatePrice(newPrice);

            persistenceService.saveAndBroadcastStock(symbol, stock, previousClose, newPrice);
        }
    }

    private void simulateBonds(VolatilityLevel volatility, int intervalSeconds) {
        for (Map.Entry<String, SimulatedStock> entry : bondCache.entrySet()) {
            String symbol = entry.getKey();
            SimulatedStock bond = entry.getValue();

            BigDecimal meanPrice = bond.getPreviousClose() != null ? bond.getPreviousClose() : bond.getCurrentPrice();
            BigDecimal newPrice = priceEngine.simulateMeanReversion(
                symbol,
                bond.getCurrentPrice(),
                meanPrice,
                bond.getBaseVolatility(),
                volatility,
                0.18,
                intervalSeconds
            ).setScale(6, RoundingMode.HALF_UP);

            BigDecimal previousClose = bond.getCurrentPrice();
            bond.updatePrice(newPrice);
            bond.updateVolume();

            persistenceService.saveInstrumentQuote(
                symbol,
                Instrument.InstrumentType.BOND,
                bond,
                previousClose,
                newPrice
            );
        }
    }

    private void simulateFunds(VolatilityLevel volatility, MarketTrend trend, int intervalSeconds) {
        for (Map.Entry<String, SimulatedStock> entry : fundCache.entrySet()) {
            String symbol = entry.getKey();
            SimulatedStock fund = entry.getValue();

            BigDecimal newPrice = priceEngine.simulateGBM(
                symbol,
                fund.getCurrentPrice(),
                fund.getBaseVolatility(),
                volatility,
                trend,
                intervalSeconds
            ).setScale(6, RoundingMode.HALF_UP);

            BigDecimal previousClose = fund.getCurrentPrice();
            fund.updatePrice(newPrice);
            fund.updateVolume();

            persistenceService.saveInstrumentQuote(
                symbol,
                Instrument.InstrumentType.FUND,
                fund,
                previousClose,
                newPrice
            );
        }
    }

    private void simulateViop(VolatilityLevel volatility, MarketTrend trend, int intervalSeconds, double eventMultiplier) {
        for (Map.Entry<String, SimulatedStock> entry : viopCache.entrySet()) {
            String symbol = entry.getKey();
            SimulatedStock contract = entry.getValue();

            BigDecimal basePrice = priceEngine.simulateGBM(
                symbol,
                contract.getCurrentPrice(),
                contract.getBaseVolatility(),
                volatility,
                trend,
                intervalSeconds
            );

            BigDecimal newPrice = basePrice
                .multiply(BigDecimal.valueOf(eventMultiplier))
                .setScale(6, RoundingMode.HALF_UP);

            BigDecimal previousClose = contract.getCurrentPrice();
            contract.updatePrice(newPrice);
            contract.updateVolume();

            persistenceService.saveInstrumentQuote(
                symbol,
                Instrument.InstrumentType.VIOP,
                contract,
                previousClose,
                newPrice
            );
        }
    }

    private void simulateCurrencies(VolatilityLevel volatility, int intervalSeconds) {
        for (Map.Entry<String, SimulatedCurrency> entry : currencyCache.entrySet()) {
            String code = entry.getKey();
            SimulatedCurrency currency = entry.getValue();

            if ("XAU".equals(code)) {
                BigDecimal newMid = priceEngine.simulateGBM(
                    code,
                    currency.getMidRate(),
                    currency.getBaseVolatility(),
                    volatility,
                    MarketTrend.NEUTRAL,
                    intervalSeconds
                );
                BigDecimal[] spread = priceEngine.simulateSpread(newMid, currency.getSpreadPercent());
                currency.updateRates(spread[0], spread[1]);
            } else {
                BigDecimal newMid = priceEngine.simulateMeanReversion(
                    code,
                    currency.getMidRate(),
                    currency.getMeanRate(),
                    currency.getBaseVolatility(),
                    volatility,
                    0.1,
                    intervalSeconds
                );
                BigDecimal[] spread = priceEngine.simulateSpread(newMid, currency.getSpreadPercent());
                currency.updateRates(spread[0], spread[1]);
            }

            persistenceService.saveCurrencyRate(code, currency);
        }
    }

    private void simulateIndices(VolatilityLevel volatility, MarketTrend trend, int intervalSeconds, double eventMultiplier) {
        for (Map.Entry<String, SimulatedIndex> entry : indexCache.entrySet()) {
            String symbol = entry.getKey();
            SimulatedIndex index = entry.getValue();

            BigDecimal newValue = priceEngine.simulateGBM(
                symbol,
                index.getCurrentValue(),
                index.getBaseVolatility(),
                volatility,
                trend,
                intervalSeconds
            );

            if (eventMultiplier != 1.0) {
                newValue = newValue.multiply(BigDecimal.valueOf(eventMultiplier)).setScale(2, RoundingMode.HALF_UP);
            }

            index.updateValue(newValue);
            persistenceService.saveAndBroadcastIndex(symbol, index, newValue);
        }
    }

    private void simulateCryptos(VolatilityLevel volatility, MarketTrend trend, int intervalSeconds, boolean randomEvents) {
        for (Map.Entry<String, SimulatedCrypto> entry : cryptoCache.entrySet()) {
            String symbol = entry.getKey();
            SimulatedCrypto crypto = entry.getValue();

            BigDecimal basePrice = priceEngine.simulateGBM(
                symbol,
                crypto.getCurrentPrice(),
                crypto.getBaseVolatility(),
                volatility,
                trend,
                intervalSeconds
            );

            double cryptoEventMultiplier = priceEngine.simulateCryptoEvent(randomEvents);
            BigDecimal newPrice = basePrice
                .multiply(BigDecimal.valueOf(cryptoEventMultiplier))
                .setScale(2, RoundingMode.HALF_UP);

            crypto.updatePrice(newPrice);
            crypto.updateMarketCap(newPrice);
            persistenceService.broadcastCryptoUpdate(symbol, newPrice, crypto);
        }
    }

    @Transactional
    public void resetSimulation() {
        priceCacheService.clearAllCache();
        marketEventEngine.clearAllEvents();

        persistenceService.deleteSimulationData();

        priceEngine.clearState();
        initializeMarketData();
        lastSimulationHeadlineAt = null;
    }

    @Transactional
    public Map<String, Object> deleteSimulationData() {
        return persistenceService.deleteSimulationData();
    }

    public Map<String, SimulatedStock> getStocks() {
        return Collections.unmodifiableMap(stockCache);
    }

    public Map<String, SimulatedStock> getBonds() {
        return Collections.unmodifiableMap(bondCache);
    }

    public Map<String, SimulatedStock> getFunds() {
        return Collections.unmodifiableMap(fundCache);
    }

    public Map<String, SimulatedStock> getViop() {
        return Collections.unmodifiableMap(viopCache);
    }

    public Map<String, SimulatedCurrency> getCurrencies() {
        return Collections.unmodifiableMap(currencyCache);
    }

    public Map<String, SimulatedIndex> getIndices() {
        return Collections.unmodifiableMap(indexCache);
    }

    public Map<String, SimulatedCrypto> getCryptos() {
        return Collections.unmodifiableMap(cryptoCache);
    }

    public SimulatedStock getStock(String symbol) {
        return stockCache.get(symbol);
    }

    public SimulatedStock getBond(String symbol) {
        return bondCache.get(symbol);
    }

    public SimulatedStock getFund(String symbol) {
        return fundCache.get(symbol);
    }

    public SimulatedStock getViopContract(String symbol) {
        return viopCache.get(symbol);
    }

    public SimulatedCurrency getCurrency(String code) {
        return currencyCache.get(code);
    }

    public SimulatedIndex getIndex(String symbol) {
        return indexCache.get(symbol);
    }

    public SimulatedCrypto getCrypto(String symbol) {
        return cryptoCache.get(symbol);
    }

    public MarketEventEngine getMarketEventEngine() {
        return marketEventEngine;
    }

    private void resetDailyValues() {
        for (SimulatedStock stock : stockCache.values()) {
            stock.setNewDayPreviousClose();
            stock.resetDailyOHLC(stock.getCurrentPrice());
        }

        for (SimulatedStock bond : bondCache.values()) {
            bond.setNewDayPreviousClose();
            bond.resetDailyOHLC(bond.getCurrentPrice());
        }

        for (SimulatedStock fund : fundCache.values()) {
            fund.setNewDayPreviousClose();
            fund.resetDailyOHLC(fund.getCurrentPrice());
        }

        for (SimulatedStock viop : viopCache.values()) {
            viop.setNewDayPreviousClose();
            viop.resetDailyOHLC(viop.getCurrentPrice());
        }

        for (SimulatedIndex index : indexCache.values()) {
            index.setNewDayPreviousClose();
        }

        for (SimulatedCrypto crypto : cryptoCache.values()) {
            crypto.setNewDayPreviousClose();
        }
    }

    private void ensureBaseMockNewsIfEmpty() {
        try {
            if (newsRepository.count() > 0) {
                return;
            }

            List<NewsCategory> categories = ensureDefaultNewsCategories();
            if (categories.isEmpty()) {
                return;
            }

            NewsCategory primaryCategory = categories.stream()
                .filter((item) -> "piyasa".equalsIgnoreCase(item.getSlug()))
                .findFirst()
                .orElse(categories.get(0));

            LocalDateTime now = LocalDateTime.now();
            List<News> baselineNews = List.of(
                buildMockNews(
                    primaryCategory,
                    "BIST 100 simulated opening stays positive",
                    "Simulation baseline news item for dashboard visibility.",
                    "BIST 100 started the session with moderate buying interest in simulation mode.",
                    "https://mintstack.local/simulation-news/bist100-opening",
                    now.minusMinutes(10),
                    true
                ),
                buildMockNews(
                    primaryCategory,
                    "Bond market remains stable in simulation mode",
                    "Government bond yields are moving in a narrow range.",
                    "Fixed income instruments continue to trade in a low-volatility range in simulation mode.",
                    "https://mintstack.local/simulation-news/bond-market-stable",
                    now.minusMinutes(25),
                    false
                ),
                buildMockNews(
                    primaryCategory,
                    "Fund inflows favor balanced portfolios",
                    "Balanced and mixed funds attract simulated demand.",
                    "Fund allocations in simulation mode show preference for diversified products.",
                    "https://mintstack.local/simulation-news/fund-inflows-balanced",
                    now.minusMinutes(40),
                    false
                ),
                buildMockNews(
                    primaryCategory,
                    "VIOP contracts see increased turnover",
                    "Index and FX contracts show higher intraday turnover.",
                    "Derivatives activity is elevated for benchmark contracts in simulation mode.",
                    "https://mintstack.local/simulation-news/viop-turnover-rise",
                    now.minusMinutes(55),
                    false
                ),
                buildMockNews(
                    primaryCategory,
                    "Currency basket remains range-bound",
                    "Major FX pairs keep a balanced trend in simulation mode.",
                    "USDTRY and EURTRY continue inside a controlled volatility band in simulated trading.",
                    "https://mintstack.local/simulation-news/currency-range-bound",
                    now.minusMinutes(70),
                    false
                )
            );

            newsRepository.saveAll(baselineNews);
            log.info("Seeded {} baseline simulation news items", baselineNews.size());
        } catch (Exception error) {
            log.warn("Baseline simulation news seed failed: {}", error.getMessage());
        }
    }

    private List<NewsCategory> ensureDefaultNewsCategories() {
        List<NewsCategory> categories = newsCategoryRepository.findAll();
        if (!categories.isEmpty()) {
            return categories;
        }

        List<NewsCategory> defaults = List.of(
            NewsCategory.builder()
                .name("Piyasa")
                .slug("piyasa")
                .description("Genel piyasa haberleri")
                .displayOrder(1)
                .isActive(true)
                .build(),
            NewsCategory.builder()
                .name("Ekonomi")
                .slug("ekonomi")
                .description("Makro ekonomi haberleri")
                .displayOrder(2)
                .isActive(true)
                .build(),
            NewsCategory.builder()
                .name("Sirket")
                .slug("sirket")
                .description("Sirket duyurulari")
                .displayOrder(3)
                .isActive(true)
                .build()
        );

        return newsCategoryRepository.saveAll(defaults);
    }

    private News buildMockNews(
        NewsCategory category,
        String title,
        String summary,
        String content,
        String sourceUrl,
        LocalDateTime publishedAt,
        boolean featured
    ) {
        return News.builder()
            .title(title)
            .summary(summary)
            .content(content)
            .sourceName(SIMULATION_SOURCE_NAME)
            .sourceUrl(sourceUrl)
            .category(category)
            .publishedAt(publishedAt)
            .isPublished(true)
            .isFeatured(featured)
            .viewCount(0L)
            .build();
    }

    private void normalizeLegacyScenarioNews() {
        try {
            List<News> legacyScenarioNews = newsRepository.findBySourceUrlIsNullAndSourceNameIn(NewsScenarioCatalog.SOURCES);
            if (legacyScenarioNews.isEmpty()) {
                return;
            }

            for (News news : legacyScenarioNews) {
                String originalSource = news.getSourceName();
                news.setSourceName(buildSimulationSourceName(originalSource));
                String suffix = news.getId() != null ? news.getId().toString() : String.valueOf(System.currentTimeMillis());
                news.setSourceUrl(String.format("%s/legacy-%s", SIMULATION_SOURCE_URL_PREFIX, suffix));
            }

            newsRepository.saveAll(legacyScenarioNews);
            log.info("Normalized {} legacy simulation news records", legacyScenarioNews.size());
        } catch (Exception error) {
            log.warn("Legacy simulation news normalization failed: {}", error.getMessage());
        }
    }

    private String buildSimulationSourceName(String externalSource) {
        if (externalSource == null || externalSource.isBlank()) {
            return SIMULATION_SOURCE_NAME;
        }
        return SIMULATION_SOURCE_NAME + " (" + externalSource.trim() + ")";
    }

    private String buildScenarioSourceUrl(NewsScenario scenario) {
        String newsType = scenario != null && scenario.getType() != null
            ? scenario.getType().name().toLowerCase()
            : "scenario";
        return String.format("%s/%s-%d", SIMULATION_SOURCE_URL_PREFIX, newsType, System.currentTimeMillis());
    }
}
