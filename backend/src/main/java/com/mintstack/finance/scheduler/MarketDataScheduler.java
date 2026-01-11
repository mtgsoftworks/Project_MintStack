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
     * Fetch stock prices every 15 mins (market hours)
     */
    @Scheduled(cron = "${app.scheduler.stock-prices-cron}")
    public void fetchStockPrices() {
        log.info("Starting stock prices fetch job");
        try {
            List<Instrument> stocks = instrumentRepository.findByTypeAndIsActiveTrue(Instrument.InstrumentType.STOCK);
            if (stocks.isEmpty()) {
                log.info("No stocks found to update.");
                return;
            }

            // Check Provider Configs
            UserApiConfig yahooConfig = getActiveConfig(ApiProvider.YAHOO_FINANCE);
            UserApiConfig alphaConfig = getActiveConfig(ApiProvider.ALPHA_VANTAGE);

            for (Instrument stock : stocks) {
                try {
                    BigDecimal price = null;
                    
                    // Try Yahoo First
                    if (yahooConfig != null) {
                        try {
                            price = yahooFinanceClient.fetchStockPrice(stock.getSymbol(), yahooConfig.getApiKey(), yahooConfig.getBaseUrl());
                        } catch (Exception e) {
                            log.warn("Yahoo fetch failed for {}, trying failover...", stock.getSymbol());
                        }
                    }

                    // Fallback to Alpha Vantage
                    if (price == null && alphaConfig != null) {
                        // Append .IS for BIST stocks if needed, Alpha Vantage usually needs suffix for non-US
                        // Assuming BIST stocks, we try adding .IS or .TR depending on symbol
                        String querySymbol = stock.getSymbol().endsWith(".IS") ? stock.getSymbol() : stock.getSymbol() + ".IS";
                        price = alphaVantageClient.fetchGlobalQuote(querySymbol, alphaConfig.getApiKey());
                    }

                    if (price != null) {
                        stock.setPreviousClose(stock.getCurrentPrice()); // Shift current to previous
                        stock.setCurrentPrice(price);
                        instrumentRepository.save(stock);
                        saveDailyPriceHistory(stock, price);
                    }

                } catch (Exception e) {
                    log.error("Failed to update price for {}: {}", stock.getSymbol(), e.getMessage());
                }
            }
            log.info("Stock prices fetch completed.");
        } catch (Exception e) {
            log.error("Stock prices fetch failed", e);
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
                        price = alphaVantageClient.fetchGlobalQuote(symbol + ".IS", alphaConfig.getApiKey());
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

    private UserApiConfig getActiveConfig(ApiProvider provider) {
        return userApiConfigRepository.findByProviderAndIsActiveTrue(provider)
                .stream().findFirst().orElse(null);
    }
}
