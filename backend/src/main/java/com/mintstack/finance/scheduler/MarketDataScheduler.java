package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.PriceUpdateService;
import com.mintstack.finance.service.event.EventPublisher;
import com.mintstack.finance.service.external.AlphaVantageClient;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataScheduler {

    private final TcmbApiClient tcmbApiClient;
    private final YahooFinanceClient yahooFinanceClient;
    private final AlphaVantageClient alphaVantageClient;
    private final MarketDataService marketDataService;
    private final PriceUpdateService priceUpdateService;
    private final InstrumentRepository instrumentRepository;
    private final UserApiConfigRepository userApiConfigRepository;
    private final EventPublisher eventPublisher;
    private final SimulationDataService simulationDataService;

    private static final List<String> INITIAL_BIST_STOCKS = Arrays.asList(
            "THYAO", "GARAN", "AKBNK", "EREGL", "SISE",
            "KCHOL", "SAHOL", "TUPRS", "ASELS", "BIMAS",
            "TCELL", "PGSUS", "SASA", "TOASO", "FROTO"
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

            long instrumentCount = instrumentRepository.countRealInstruments();
            if (instrumentCount == 0) {
                log.info("No instruments found. Bootstrapping before price updates.");
                if (yahooConfig != null || alphaConfig != null) {
                    bootstrapStocksFromApi();
                } else {
                    log.info("No Yahoo/Alpha config found. Skipping bootstrap.");
                }
                return;
            }

            updatePricesForType(Instrument.InstrumentType.STOCK, yahooConfig, alphaConfig);
            updatePricesForType(Instrument.InstrumentType.BOND, yahooConfig, alphaConfig);
            updatePricesForType(Instrument.InstrumentType.FUND, yahooConfig, alphaConfig);
            updatePricesForType(Instrument.InstrumentType.VIOP, yahooConfig, alphaConfig);

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
            
            if (alphaConfig != null || yahooConfig != null) {
                log.info("Database empty & API key found. Bootstrapping stocks from API...");
                bootstrapStocksFromApi();
            } else {
                log.debug("Database empty but no API key configured yet. Waiting...");
            }
        }
    }

    private void bootstrapStocksFromApi() {
        UserApiConfig alphaConfig = getActiveConfig(ApiProvider.ALPHA_VANTAGE);
        UserApiConfig yahooConfig = getActiveConfig(ApiProvider.YAHOO_FINANCE);

        if (alphaConfig == null && yahooConfig == null) {
            log.warn("No active API configuration found (Yahoo/AlphaVantage). Cannot bootstrap stocks.");
            return;
        }

        for (String symbol : INITIAL_BIST_STOCKS) {
            try {
                // Check if already exists (Real version)
                if (instrumentRepository.findBySymbolAndIsSimulated(symbol, false).isPresent()) continue;

                BigDecimal price = null;
                String name = symbol; // Default name if not fetched

                // Try Yahoo Finance (works with or without API key via direct fallback)
                try {
                    String apiKey = yahooConfig != null ? yahooConfig.getApiKey() : null;
                    String baseUrl = yahooConfig != null ? yahooConfig.getBaseUrl() : null;
                    price = yahooFinanceClient.fetchStockPrice(symbol, apiKey, baseUrl);
                } catch (Exception e) {
                    log.debug("Yahoo fetch failed for bootstrap {}: {}", symbol, e.getMessage());
                }

                // Fallback to Alpha Vantage
                if (price == null && alphaConfig != null) {
                    try {
                        price = alphaVantageClient.fetchGlobalQuote(buildAlphaSymbol(symbol), alphaConfig.getApiKey());
                    } catch (Exception ignored) {}
                }

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

    private void updatePricesForType(Instrument.InstrumentType type, UserApiConfig yahooConfig, UserApiConfig alphaConfig) {
        // Only fetch real instruments (isSimulated = false)
        List<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(type, false);
        if (instruments.isEmpty()) {
            log.info("No {} instruments found to update.", type);
            return;
        }

        for (Instrument instrument : instruments) {
            try {
                BigDecimal price = fetchInstrumentPrice(instrument, yahooConfig, alphaConfig);
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
    }

    private BigDecimal fetchInstrumentPrice(Instrument instrument, UserApiConfig yahooConfig, UserApiConfig alphaConfig) {
        BigDecimal price = null;

        // Try Yahoo Finance (works with or without API key via direct fallback)
        try {
            String apiKey = yahooConfig != null ? yahooConfig.getApiKey() : null;
            String baseUrl = yahooConfig != null ? yahooConfig.getBaseUrl() : null;
            price = yahooFinanceClient.fetchStockPrice(instrument.getSymbol(), apiKey, baseUrl);
        } catch (Exception e) {
            log.warn("Yahoo fetch failed for {} ({}), trying Alpha Vantage...", instrument.getSymbol(), instrument.getType());
        }

        // Fallback to Alpha Vantage if Yahoo failed and config exists
        if (price == null && alphaConfig != null) {
            try {
                String querySymbol = buildAlphaSymbol(instrument.getSymbol());
                price = alphaVantageClient.fetchGlobalQuote(querySymbol, alphaConfig.getApiKey());
            } catch (Exception e) {
                log.warn("Alpha Vantage also failed for {}", instrument.getSymbol());
            }
        }

        return price;
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
}
