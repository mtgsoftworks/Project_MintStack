package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.external.AlphaVantageClient;
import com.mintstack.finance.service.external.TcmbApiClient;
import com.mintstack.finance.service.external.YahooFinanceClient;
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
    private final InstrumentRepository instrumentRepository;
    private final UserApiConfigRepository userApiConfigRepository;

    private static final List<String> INITIAL_BIST_STOCKS = Arrays.asList(
            "THYAO", "GARAN", "AKBNK", "EREGL", "SISE",
            "KCHOL", "SAHOL", "TUPRS", "ASELS", "BIMAS",
            "TCELL", "PGSUS", "SASA", "TOASO", "FROTO"
    );

    /**
     * Fetch TCMB currency rates (9:00, 12:00, 15:00 weekdays)
     */
    @Scheduled(cron = "${app.scheduler.tcmb-rates-cron}")
    public void fetchTcmbRates() {
        log.info("Starting TCMB rates fetch job");
        try {
            List<CurrencyRate> rates = tcmbApiClient.fetchTodayRates();
            marketDataService.saveCurrencyRates(rates);
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
        log.info("Starting market prices fetch job");
        try {
            // Check Provider Configs
            UserApiConfig yahooConfig = getActiveConfig(ApiProvider.YAHOO_FINANCE);
            UserApiConfig alphaConfig = getActiveConfig(ApiProvider.ALPHA_VANTAGE);

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
        // 1. Load TCMB Rates (always try)
        try {
            List<CurrencyRate> rates = tcmbApiClient.fetchTodayRates();
            if (rates != null && !rates.isEmpty()) {
                marketDataService.saveCurrencyRates(rates);
            }
        } catch (Exception e) {
            log.debug("TCMB load: {}", e.getMessage());
        }

        // 2. Bootstrap Stocks from API if DB is empty
        long stockCount = instrumentRepository.count();
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
                // Check if already exists
                if (instrumentRepository.findBySymbol(symbol).isPresent()) continue;

                BigDecimal price = null;
                String name = symbol; // Default name if not fetched

                // Try fetching to validate and get price
                if (yahooConfig != null) {
                    try {
                        price = yahooFinanceClient.fetchStockPrice(symbol, yahooConfig.getApiKey(), yahooConfig.getBaseUrl());
                        // Yahoo might return name in a more complex response, simplifying here
                    } catch (Exception ignored) {}
                }

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
                
                // Rate limit protection
                Thread.sleep(1000); 

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
        List<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrue(type);
        if (instruments.isEmpty()) {
            log.info("No {} instruments found to update.", type);
            return;
        }

        for (Instrument instrument : instruments) {
            try {
                BigDecimal price = fetchInstrumentPrice(instrument, yahooConfig, alphaConfig);
                if (price != null) {
                    instrument.setPreviousClose(instrument.getCurrentPrice());
                    instrument.setCurrentPrice(price);
                    instrumentRepository.save(instrument);
                    saveDailyPriceHistory(instrument, price);
                }
            } catch (Exception e) {
                log.error("Failed to update price for {} ({}): {}", instrument.getSymbol(), type, e.getMessage());
            }
        }
    }

    private BigDecimal fetchInstrumentPrice(Instrument instrument, UserApiConfig yahooConfig, UserApiConfig alphaConfig) {
        BigDecimal price = null;

        if (yahooConfig != null) {
            try {
                price = yahooFinanceClient.fetchStockPrice(instrument.getSymbol(), yahooConfig.getApiKey(), yahooConfig.getBaseUrl());
            } catch (Exception e) {
                log.warn("Yahoo fetch failed for {} ({}), trying failover...", instrument.getSymbol(), instrument.getType());
            }
        }

        if (price == null && alphaConfig != null) {
            String querySymbol = buildAlphaSymbol(instrument.getSymbol());
            price = alphaVantageClient.fetchGlobalQuote(querySymbol, alphaConfig.getApiKey());
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
