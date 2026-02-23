package com.mintstack.finance.service.simulation;

import com.mintstack.finance.dto.simulation.MarketEvent;
import com.mintstack.finance.dto.simulation.NewsScenario;
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
    private final Map<String, SimulatedCurrency> currencyCache = new ConcurrentHashMap<>();
    private final Map<String, SimulatedIndex> indexCache = new ConcurrentHashMap<>();
    private final Map<String, SimulatedCrypto> cryptoCache = new ConcurrentHashMap<>();

    private LocalDate lastTradingDate = null;
    private final Random random = new Random();

    @PostConstruct
    public void initializeMarketData() {
        marketBootstrapService.initializeCaches(stockCache, currencyCache, indexCache, cryptoCache);
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
        simulateCurrencies(volatility, intervalSeconds);
        simulateIndices(volatility, trend, intervalSeconds, eventMultiplier);
        simulateCryptos(volatility, trend, intervalSeconds, randomEvents);
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
                .sourceName(scenario.getSource())
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
    }

    @Transactional
    public Map<String, Object> deleteSimulationData() {
        return persistenceService.deleteSimulationData();
    }

    public Map<String, SimulatedStock> getStocks() {
        return Collections.unmodifiableMap(stockCache);
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

        for (SimulatedIndex index : indexCache.values()) {
            index.setNewDayPreviousClose();
        }

        for (SimulatedCrypto crypto : cryptoCache.values()) {
            crypto.setNewDayPreviousClose();
        }
    }
}
