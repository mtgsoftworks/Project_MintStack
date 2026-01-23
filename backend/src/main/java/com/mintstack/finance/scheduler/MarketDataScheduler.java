package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.repository.UserDataPreferenceRepository;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.PriceUpdateService;
import com.mintstack.finance.service.event.EventPublisher;
import com.mintstack.finance.service.external.AlphaVantageClient;
import com.mintstack.finance.service.external.FinnhubClient;
import com.mintstack.finance.service.external.TcmbApiClient;
import com.mintstack.finance.service.external.YahooFinanceClient;
import com.mintstack.finance.service.simulation.SimulationDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataScheduler {

    private final TcmbApiClient tcmbApiClient;
    private final YahooFinanceClient yahooFinanceClient;
    private final AlphaVantageClient alphaVantageClient;
    private final FinnhubClient finnhubClient;
    private final MarketDataService marketDataService;
    private final PriceUpdateService priceUpdateService;
    private final InstrumentRepository instrumentRepository;
    private final UserApiConfigRepository userApiConfigRepository;
    private final UserDataPreferenceRepository preferenceRepository;
    private final EventPublisher eventPublisher;
    private final SimulationDataService simulationDataService;

    private static final List<String> INITIAL_BIST_STOCKS = Arrays.asList(
            "THYAO", "GARAN", "AKBNK", "EREGL", "SISE",
            "KCHOL", "SAHOL", "TUPRS", "ASELS", "BIMAS",
            "TCELL", "PGSUS", "SASA", "TOASO", "FROTO"
    );

    private static final List<String> FOREX_PAIRS = Arrays.asList(
            "USD/TRY", "EUR/TRY", "GBP/TRY", "CHF/TRY", "JPY/TRY"
    );

    private static final List<String> INITIAL_CRYPTO = Arrays.asList(
            "BTC-USD", "ETH-USD", "BNB-USD", "SOL-USD", "XRP-USD"
    );

    /**
     * Fetch TCMB currency rates (9:00, 12:00, 15:00 weekdays)
     * Only runs if TCMB API is configured by user
     */
    @Scheduled(cron = "${app.scheduler.tcmb-rates-cron}")
    public void fetchTcmbRates() {
        // Simülasyon modu aktifse gerçek API çağrısı yapma
        if (simulationDataService.isSimulationEnabled()) {
            log.debug("Simulation mode active. Skipping TCMB rates fetch.");
            return;
        }
        
        UserApiConfig tcmbConfig = getActiveConfig(ApiProvider.TCMB);
        if (tcmbConfig == null) {
            log.debug("TCMB API not configured. Skipping currency rates fetch.");
            return;
        }
        
        log.info("Starting TCMB rates fetch job");
        try {
            List<CurrencyRate> rates = tcmbApiClient.fetchTodayRates();
            marketDataService.saveCurrencyRates(rates);
            
            // Broadcast currency updates via WebSocket and publish to Kafka
            for (CurrencyRate rate : rates) {
                priceUpdateService.broadcastCurrencyUpdate(
                    rate.getCurrencyCode(),
                    rate.getBuyingRate(),
                    rate.getSellingRate()
                );
                
                // Publish market data event to Kafka
                eventPublisher.publishMarketDataEvent(
                    rate.getCurrencyCode(),
                    "CURRENCY_RATE",
                    java.util.Map.of(
                        "buyingRate", rate.getBuyingRate(),
                        "sellingRate", rate.getSellingRate(),
                        "date", rate.getRateDate().toString()
                    )
                );
            }
            
            log.info("TCMB rates fetch completed: {} rates saved", rates.size());
        } catch (Exception e) {
            log.error("TCMB rates fetch failed", e);
        }
    }

    /**
     * Fetch instrument prices every 15 mins (market hours)
     */
    @Scheduled(cron = "${app.scheduler.stock-prices-cron}")
    public void fetchStockPrices() {
        // Simülasyon modu aktifse gerçek API çağrısı yapma
        if (simulationDataService.isSimulationEnabled()) {
            log.debug("Simulation mode active. Skipping stock prices fetch.");
            return;
        }
        
        log.info("Starting market prices fetch job");
        try {
            // Check Provider Configs
            UserApiConfig yahooConfig = getActiveConfig(ApiProvider.YAHOO_FINANCE);
            UserApiConfig alphaConfig = getActiveConfig(ApiProvider.ALPHA_VANTAGE);
            UserApiConfig finnhubConfig = getActiveConfig(ApiProvider.FINNHUB);
            EnumMap<DataType, ApiProvider> preferredProviders = resolvePreferredProviders();
            ApiProvider preferredBistProvider = preferredProviders.get(DataType.BIST_STOCKS);

            long instrumentCount = instrumentRepository.countRealInstruments();
            if (instrumentCount == 0) {
                log.info("No instruments found. Bootstrapping before price updates.");
                if (hasStockProviderForDataType(DataType.BIST_STOCKS, preferredBistProvider, yahooConfig, alphaConfig, finnhubConfig)) {
                    bootstrapStocksFromApi(preferredBistProvider, yahooConfig, alphaConfig, finnhubConfig);
                } else {
                    log.info("No suitable provider config found for BIST bootstrap. Skipping bootstrap.");
                }
                return;
            }

            updatePricesForType(Instrument.InstrumentType.STOCK, yahooConfig, alphaConfig, finnhubConfig, preferredProviders);
            updatePricesForType(Instrument.InstrumentType.BOND, yahooConfig, alphaConfig, finnhubConfig, preferredProviders);
            updatePricesForType(Instrument.InstrumentType.FUND, yahooConfig, alphaConfig, finnhubConfig, preferredProviders);
            updatePricesForType(Instrument.InstrumentType.VIOP, yahooConfig, alphaConfig, finnhubConfig, preferredProviders);
            updatePricesForType(Instrument.InstrumentType.CRYPTO, yahooConfig, alphaConfig, finnhubConfig, preferredProviders);

            log.info("Market prices fetch completed.");
        } catch (Exception e) {
            log.error("Market prices fetch failed", e);
        }
    }

    /**
     * Initial data load and periodic bootstrap check
     * Runs every 60 seconds - if DB is empty and API key exists, bootstraps stocks
     */
    @Scheduled(initialDelay = 5000, fixedDelay = 60000)
    public void initialDataLoad() {
        // Simülasyon modu aktifse gerçek API çağrısı yapma
        if (simulationDataService.isSimulationEnabled()) {
            log.debug("Simulation mode active. Skipping initial data load from external APIs.");
            return;
        }
        
        // 1. Load TCMB Rates (only if configured)
        UserApiConfig tcmbConfig = getActiveConfig(ApiProvider.TCMB);
        if (tcmbConfig != null) {
            try {
                List<CurrencyRate> rates = tcmbApiClient.fetchTodayRates();
                if (rates != null && !rates.isEmpty()) {
                    marketDataService.saveCurrencyRates(rates);
                    log.info("TCMB rates loaded: {} currencies", rates.size());
                }
            } catch (Exception e) {
                log.debug("TCMB load: {}", e.getMessage());
            }
        }

        // 2. Bootstrap Stocks from API if DB is empty
        long stockCount = instrumentRepository.countRealInstruments();
        if (stockCount == 0) {
            UserApiConfig alphaConfig = getActiveConfig(ApiProvider.ALPHA_VANTAGE);
            UserApiConfig yahooConfig = getActiveConfig(ApiProvider.YAHOO_FINANCE);
            UserApiConfig finnhubConfig = getActiveConfig(ApiProvider.FINNHUB);
            EnumMap<DataType, ApiProvider> preferredProviders = resolvePreferredProviders();
            ApiProvider preferredBistProvider = preferredProviders.get(DataType.BIST_STOCKS);

            if (hasStockProviderForDataType(DataType.BIST_STOCKS, preferredBistProvider, yahooConfig, alphaConfig, finnhubConfig)) {
                log.info("Database empty & API key found. Bootstrapping stocks from API...");
                bootstrapStocksFromApi(preferredBistProvider, yahooConfig, alphaConfig, finnhubConfig);
            } else {
                log.debug("Database empty but no API key configured yet. Waiting...");
            }
        }
    }

    private void bootstrapStocksFromApi(ApiProvider preferredBistProvider,
                                        UserApiConfig yahooConfig,
                                        UserApiConfig alphaConfig,
                                        UserApiConfig finnhubConfig) {
        if (!hasStockProviderForDataType(DataType.BIST_STOCKS, preferredBistProvider, yahooConfig, alphaConfig, finnhubConfig)) {
            log.warn("No active API configuration found for BIST bootstrap. Cannot bootstrap stocks.");
            return;
        }

        for (String symbol : INITIAL_BIST_STOCKS) {
            try {
                // Check if already exists (Real version)
                if (instrumentRepository.findBySymbolAndIsSimulated(symbol, false).isPresent()) continue;

                Instrument lookupInstrument = Instrument.builder()
                    .symbol(symbol)
                    .exchange("BIST")
                    .build();
                BigDecimal price = fetchInstrumentPrice(
                    lookupInstrument,
                    DataType.BIST_STOCKS,
                    preferredBistProvider,
                    yahooConfig,
                    alphaConfig,
                    finnhubConfig
                );

                if (price != null) {
                    Instrument stock = new Instrument();
                    stock.setId(UUID.randomUUID());
                    stock.setSymbol(symbol);
                    stock.setName(symbol + " (BIST)"); // Simplified naming
                    stock.setType(Instrument.InstrumentType.STOCK);
                    stock.setExchange("BIST");
                    stock.setCurrency("TRY");
                    stock.setIsActive(true);
                    stock.setCurrentPrice(price);
                    stock.setPreviousClose(price); // Initial state
                    
                    instrumentRepository.save(stock);
                    log.info("Bootstrapped stock: {} - Price: {}", symbol, price);
                } else {
                    log.warn("Could not fetch price for {}, skipping bootstrap.", symbol);
                }
                
                // FIX: Rate limit protection using non-blocking delay
                // The delay is handled by the API clients themselves via WebClient timeout
                // No blocking Thread.sleep needed here - API client rate limiting is sufficient

            } catch (Exception e) {
                log.error("Error bootstrapping stock {}", symbol, e);
            }
        }
    }

    private void saveDailyPriceHistory(Instrument instrument, BigDecimal price) {
        if (price == null) return;
        try {
            PriceHistory history = PriceHistory.builder()
                .instrument(instrument)
                .priceDate(LocalDate.now())
                .closePrice(price)
                .openPrice(price)
                .highPrice(price)
                .lowPrice(price)
                .build();
            marketDataService.savePriceHistory(history);
        } catch (Exception e) {
            log.warn("Failed to history for {}", instrument.getSymbol());
        }
    }

    private void updatePricesForType(Instrument.InstrumentType type,
                                     UserApiConfig yahooConfig,
                                     UserApiConfig alphaConfig,
                                     UserApiConfig finnhubConfig,
                                     EnumMap<DataType, ApiProvider> preferredProviders) {
        // Only fetch real instruments (isSimulated = false)
        List<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(type, false);
        if (instruments.isEmpty()) {
            log.info("No {} instruments found to update.", type);
            return;
        }

        int batchSize = 5; // Process 5 instruments at a time
        long delayBetweenRequests = 1000; // 1 second delay between batches (to respect API limits)

        for (int i = 0; i < instruments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, instruments.size());
            List<Instrument> batch = instruments.subList(i, end);

            log.info("Processing batch {}/{} ({} instruments)", (i / batchSize) + 1, (instruments.size() + batchSize - 1) / batchSize, batch.size());

            for (Instrument instrument : batch) {
                try {
                    DataType dataType = resolveDataTypeForInstrument(type, instrument);
                    ApiProvider preferredProvider = preferredProviders.get(dataType);
                    BigDecimal price = fetchInstrumentPrice(
                        instrument,
                        dataType,
                        preferredProvider,
                        yahooConfig,
                        alphaConfig,
                        finnhubConfig
                    );
                    if (price != null) {
                        BigDecimal previousClose = instrument.getCurrentPrice();
                        instrument.setPreviousClose(previousClose);
                        instrument.setCurrentPrice(price);
                        instrumentRepository.save(instrument);
                        saveDailyPriceHistory(instrument, price);

                        // Broadcast price update via WebSocket
                        BigDecimal change = previousClose != null ? price.subtract(previousClose) : BigDecimal.ZERO;
                        BigDecimal changePercent = previousClose != null && previousClose.compareTo(BigDecimal.ZERO) != 0
                            ? change.divide(previousClose, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                            : BigDecimal.ZERO;

                        priceUpdateService.broadcastStockUpdate(
                            instrument.getSymbol(),
                            price,
                            previousClose,
                            change,
                            changePercent
                        );
                    }
                } catch (Exception e) {
                    log.error("Failed to update price for {} ({}): {}", instrument.getSymbol(), type, e.getMessage());
                }
            }

            // Add delay between batches to respect API rate limits
            if (i + batchSize < instruments.size()) {
                try {
                    Thread.sleep(delayBetweenRequests);
                } catch (InterruptedException e) {
                    log.warn("Batch delay interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private BigDecimal fetchInstrumentPrice(Instrument instrument,
                                            DataType dataType,
                                            ApiProvider preferredProvider,
                                            UserApiConfig yahooConfig,
                                            UserApiConfig alphaConfig,
                                            UserApiConfig finnhubConfig) {
        if (preferredProvider != null) {
            return fetchPriceForProvider(preferredProvider, instrument, yahooConfig, alphaConfig, finnhubConfig);
        }

        BigDecimal price = fetchPriceForProvider(ApiProvider.YAHOO_FINANCE, instrument, yahooConfig, alphaConfig, finnhubConfig);
        if (price == null) {
            price = fetchPriceForProvider(ApiProvider.ALPHA_VANTAGE, instrument, yahooConfig, alphaConfig, finnhubConfig);
        }
        if (price == null) {
            price = fetchPriceForProvider(ApiProvider.FINNHUB, instrument, yahooConfig, alphaConfig, finnhubConfig);
        }
        if (price == null) {
            log.warn("No price fetched for {} using any provider for {}", instrument.getSymbol(), dataType);
        }
        return price;
    }

    private BigDecimal fetchPriceForProvider(ApiProvider provider,
                                             Instrument instrument,
                                             UserApiConfig yahooConfig,
                                             UserApiConfig alphaConfig,
                                             UserApiConfig finnhubConfig) {
        try {
            switch (provider) {
                case YAHOO_FINANCE -> {
                    if (yahooConfig == null) {
                        return null;
                    }
                    String apiKey = yahooConfig.getApiKey();
                    String baseUrl = yahooConfig.getBaseUrl();
                    return yahooFinanceClient.fetchStockPrice(instrument.getSymbol(), apiKey, baseUrl);
                }
                case ALPHA_VANTAGE -> {
                    if (alphaConfig == null) {
                        return null;
                    }
                    String querySymbol = buildAlphaSymbol(instrument.getSymbol());
                    return alphaVantageClient.fetchGlobalQuote(querySymbol);
                }
                case FINNHUB -> {
                    if (finnhubConfig == null) {
                        return null;
                    }
                    return finnhubClient.fetchStockQuote(instrument.getSymbol());
                }
                default -> {
                    return null;
                }
            }
        } catch (Exception e) {
            log.warn("{} fetch failed for {}: {}", provider, instrument.getSymbol(), e.getMessage());
            return null;
        }
    }

    private EnumMap<DataType, ApiProvider> resolvePreferredProviders() {
        EnumMap<DataType, ApiProvider> map = new EnumMap<>(DataType.class);
        for (DataType dataType : DataType.values()) {
            preferenceRepository.findFirstByDataTypeAndIsEnabledTrueOrderByUpdatedAtDesc(dataType)
                .ifPresent(pref -> map.put(dataType, pref.getProvider()));
        }
        return map;
    }

    private boolean hasStockProviderForDataType(DataType dataType,
                                                ApiProvider preferredProvider,
                                                UserApiConfig yahooConfig,
                                                UserApiConfig alphaConfig,
                                                UserApiConfig finnhubConfig) {
        if (preferredProvider != null) {
            return isProviderConfigured(preferredProvider, yahooConfig, alphaConfig, finnhubConfig);
        }
        return yahooConfig != null || alphaConfig != null || finnhubConfig != null;
    }

    private boolean isProviderConfigured(ApiProvider provider,
                                         UserApiConfig yahooConfig,
                                         UserApiConfig alphaConfig,
                                         UserApiConfig finnhubConfig) {
        return switch (provider) {
            case YAHOO_FINANCE -> yahooConfig != null;
            case ALPHA_VANTAGE -> alphaConfig != null;
            case FINNHUB -> finnhubConfig != null;
            default -> false;
        };
    }

    private DataType resolveDataTypeForInstrument(Instrument.InstrumentType type, Instrument instrument) {
        if (type == Instrument.InstrumentType.STOCK) {
            String exchange = instrument.getExchange();
            if (exchange != null && exchange.equalsIgnoreCase("BIST")) {
                return DataType.BIST_STOCKS;
            }
            return DataType.US_STOCKS;
        }
        if (type == Instrument.InstrumentType.CRYPTO) {
            return DataType.CRYPTO;
        }
        return DataType.BIST_STOCKS;
    }

    private String buildAlphaSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return symbol;
        }
        return symbol.contains(".") ? symbol : symbol + ".IS";
    }

    private UserApiConfig getActiveConfig(ApiProvider provider) {
        return userApiConfigRepository.findByProviderAndIsActiveTrue(provider)
                .stream().findFirst().orElse(null);
    }

    /**
     * Fetch non-TCMB forex rates (every 5 minutes)
     */
    @Scheduled(cron = "${app.scheduler.forex-rates-cron}")
    public void fetchNonTcmbForexRates() {
        if (simulationDataService.isSimulationEnabled()) {
            log.debug("Simulation mode active. Skipping non-TCMB forex rates fetch.");
            return;
        }

        UserApiConfig alphaConfig = getActiveConfig(ApiProvider.ALPHA_VANTAGE);
        UserApiConfig finnhubConfig = getActiveConfig(ApiProvider.FINNHUB);
        EnumMap<DataType, ApiProvider> preferredProviders = resolvePreferredProviders();
        ApiProvider preferredForexProvider = preferredProviders.get(DataType.CURRENCY_RATES);

        if (alphaConfig == null && finnhubConfig == null) {
            log.debug("No forex provider configured. Skipping non-TCMB forex rates fetch.");
            return;
        }

        log.info("Starting non-TCMB forex rates fetch job");
        int batchSize = 2;
        long delayBetweenRequests = 2000;

        for (int i = 0; i < FOREX_PAIRS.size(); i += batchSize) {
            int end = Math.min(i + batchSize, FOREX_PAIRS.size());
            List<String> batch = FOREX_PAIRS.subList(i, end);

            for (String pair : batch) {
                String[] currencies = pair.split("/");
                String from = currencies[0];
                String to = currencies[1];

                try {
                    CurrencyRate rate = fetchForexRate(from, to, preferredForexProvider, alphaConfig, finnhubConfig);
                    if (rate != null) {
                        marketDataService.saveCurrencyRates(List.of(rate));
                        priceUpdateService.broadcastCurrencyUpdate(
                            rate.getCurrencyCode(),
                            rate.getBuyingRate(),
                            rate.getSellingRate()
                        );
                        eventPublisher.publishMarketDataEvent(
                            rate.getCurrencyCode(),
                            "CURRENCY_RATE",
                            Map.of(
                                "buyingRate", rate.getBuyingRate(),
                                "sellingRate", rate.getSellingRate(),
                                "date", rate.getFetchedAt() != null ? rate.getFetchedAt().toString() : LocalDateTime.now().toString()
                            )
                        );
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch forex rate for {}: {}", pair, e.getMessage());
                }
            }

            if (i + batchSize < FOREX_PAIRS.size()) {
                try {
                    Thread.sleep(delayBetweenRequests);
                } catch (InterruptedException e) {
                    log.warn("Forex batch delay interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("Non-TCMB forex rates fetch completed.");
    }

    /**
     * Fetch crypto prices (every minute)
     */
    @Scheduled(cron = "${app.scheduler.crypto-prices-cron}")
    public void fetchCryptoPrices() {
        if (simulationDataService.isSimulationEnabled()) {
            log.debug("Simulation mode active. Skipping crypto prices fetch.");
            return;
        }

        UserApiConfig finnhubConfig = getActiveConfig(ApiProvider.FINNHUB);

        if (finnhubConfig == null) {
            log.debug("No crypto provider configured. Skipping crypto prices fetch.");
            return;
        }

        log.info("Starting crypto prices fetch job");
        List<Instrument> cryptoInstruments = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(Instrument.InstrumentType.CRYPTO, false);

        if (cryptoInstruments.isEmpty()) {
            log.info("No crypto instruments found. Bootstrapping crypto instruments.");
            bootstrapCryptoInstruments(finnhubConfig);
            return;
        }

        int batchSize = 3;
        long delayBetweenRequests = 1000;

        for (int i = 0; i < cryptoInstruments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, cryptoInstruments.size());
            List<Instrument> batch = cryptoInstruments.subList(i, end);

            for (Instrument instrument : batch) {
                try {
                    BigDecimal price = finnhubClient.fetchCryptoPrice(instrument.getSymbol());
                    if (price != null) {
                        BigDecimal previousClose = instrument.getCurrentPrice();
                        instrument.setPreviousClose(previousClose);
                        instrument.setCurrentPrice(price);
                        instrumentRepository.save(instrument);
                        saveDailyPriceHistory(instrument, price);

                        BigDecimal change = previousClose != null ? price.subtract(previousClose) : BigDecimal.ZERO;
                        BigDecimal changePercent = previousClose != null && previousClose.compareTo(BigDecimal.ZERO) != 0
                            ? change.divide(previousClose, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                            : BigDecimal.ZERO;

                        priceUpdateService.broadcastStockUpdate(
                            instrument.getSymbol(),
                            price,
                            previousClose,
                            change,
                            changePercent
                        );
                    }
                } catch (Exception e) {
                    log.error("Failed to update crypto price for {}: {}", instrument.getSymbol(), e.getMessage());
                }
            }

            if (i + batchSize < cryptoInstruments.size()) {
                try {
                    Thread.sleep(delayBetweenRequests);
                } catch (InterruptedException e) {
                    log.warn("Crypto batch delay interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("Crypto prices fetch completed.");
    }

    private void bootstrapCryptoInstruments(UserApiConfig finnhubConfig) {
        for (String symbol : INITIAL_CRYPTO) {
            try {
                if (instrumentRepository.findBySymbolAndIsSimulated(symbol, false).isPresent()) continue;

                BigDecimal price = finnhubClient.fetchCryptoPrice(symbol);
                if (price != null) {
                    Instrument crypto = new Instrument();
                    crypto.setId(UUID.randomUUID());
                    crypto.setSymbol(symbol);
                    crypto.setName(symbol);
                    crypto.setType(Instrument.InstrumentType.CRYPTO);
                    crypto.setExchange("CRYPTO");
                    crypto.setCurrency("USD");
                    crypto.setIsActive(true);
                    crypto.setCurrentPrice(price);
                    crypto.setPreviousClose(price);

                    instrumentRepository.save(crypto);
                    log.info("Bootstrapped crypto: {} - Price: {}", symbol, price);
                }
            } catch (Exception e) {
                log.error("Error bootstrapping crypto {}", symbol, e);
            }
        }
    }

    private CurrencyRate fetchForexRate(String fromCurrency, String toCurrency,
                                        ApiProvider preferredProvider,
                                        UserApiConfig alphaConfig,
                                        UserApiConfig finnhubConfig) {
        if (preferredProvider != null) {
            return fetchForexRateForProvider(preferredProvider, fromCurrency, toCurrency, alphaConfig, finnhubConfig);
        }

        CurrencyRate rate = fetchForexRateForProvider(ApiProvider.ALPHA_VANTAGE, fromCurrency, toCurrency, alphaConfig, finnhubConfig);
        if (rate == null) {
            rate = fetchForexRateForProvider(ApiProvider.FINNHUB, fromCurrency, toCurrency, alphaConfig, finnhubConfig);
        }
        return rate;
    }

    private CurrencyRate fetchForexRateForProvider(ApiProvider provider,
                                                    String fromCurrency,
                                                    String toCurrency,
                                                    UserApiConfig alphaConfig,
                                                    UserApiConfig finnhubConfig) {
        try {
            switch (provider) {
                case ALPHA_VANTAGE -> {
                    if (alphaConfig == null) return null;
                    return alphaVantageClient.fetchExchangeRate(fromCurrency, toCurrency);
                }
                case FINNHUB -> {
                    if (finnhubConfig == null) return null;
                    return finnhubClient.fetchForexRate(fromCurrency, toCurrency);
                }
                default -> {
                    return null;
                }
            }
        } catch (Exception e) {
            log.warn("{} forex fetch failed for {}/{}: {}", provider, fromCurrency, toCurrency, e.getMessage());
            return null;
        }
    }
}
