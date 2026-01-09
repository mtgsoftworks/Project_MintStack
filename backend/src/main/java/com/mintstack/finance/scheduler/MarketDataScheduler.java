package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.external.TcmbApiClient;
import com.mintstack.finance.service.external.YahooFinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.mintstack.finance.entity.UserApiConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataScheduler {

    private final TcmbApiClient tcmbApiClient;
    private final YahooFinanceClient yahooFinanceClient;
    private final MarketDataService marketDataService;
    private final InstrumentRepository instrumentRepository;

    /**
     * Fetch TCMB currency rates at 9:00, 12:00, 15:00 on weekdays
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
     * Fetch stock prices every 15 minutes during market hours (9:00-18:00) on weekdays
     * Also saves daily price history
     */
    @Scheduled(cron = "${app.scheduler.stock-prices-cron}")
    public void fetchStockPrices() {
        log.info("Starting stock prices fetch job");
        try {
            List<Instrument> stocks = instrumentRepository.findByTypeAndIsActiveTrue(Instrument.InstrumentType.STOCK);
            
            UserApiConfig config = marketDataService.getActiveYahooConfig();
            String apiKey = config != null ? config.getApiKey() : null;
            String baseUrl = config != null ? config.getBaseUrl() : null;
            
            for (Instrument stock : stocks) {
                try {
                    BigDecimal price = yahooFinanceClient.fetchStockPrice(stock.getSymbol(), apiKey, baseUrl);
                    
                    // Update current price
                    stock.setPreviousClose(stock.getCurrentPrice());
                    stock.setCurrentPrice(price);
                    instrumentRepository.save(stock);
                    
                    // Save to price history (one entry per day)
                    saveDailyPriceHistory(stock, price);
                    
                } catch (Exception e) {
                    log.error("Failed to update price for {}: {}", stock.getSymbol(), e.getMessage());
                }
            }
            
            log.info("Stock prices fetch completed: {} stocks updated", stocks.size());
        } catch (Exception e) {
            log.error("Stock prices fetch failed", e);
        }
    }

    /**
     * Initial data load on application startup
     */
    @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    public void initialDataLoad() {
        log.info("Starting initial data load");
        
        // Fetch initial TCMB rates
        try {
            List<CurrencyRate> rates = tcmbApiClient.fetchTodayRates();
            marketDataService.saveCurrencyRates(rates);
            log.info("Initial TCMB rates loaded: {} rates", rates.size());
        } catch (Exception e) {
            log.warn("Initial TCMB rates load failed: {}", e.getMessage());
        }
        
        // Fetch initial stock prices AND historical data
        try {
            List<Instrument> stocks = instrumentRepository.findByTypeAndIsActiveTrue(Instrument.InstrumentType.STOCK);
            
            UserApiConfig config = marketDataService.getActiveYahooConfig();
            String apiKey = config != null ? config.getApiKey() : null;
            String baseUrl = config != null ? config.getBaseUrl() : null;
            
            for (Instrument stock : stocks) {
                try {
                    // Fetch current price
                    BigDecimal price = yahooFinanceClient.fetchStockPrice(stock.getSymbol(), apiKey, baseUrl);
                    stock.setPreviousClose(stock.getCurrentPrice());
                    stock.setCurrentPrice(price);
                    instrumentRepository.save(stock);
                    
                    // Fetch and save historical data (last 180 days)
                    fetchAndSaveHistoricalData(stock.getSymbol(), apiKey, baseUrl);
                    
                } catch (Exception e) {
                    log.warn("Failed to load data for {}: {}", stock.getSymbol(), e.getMessage());
                }
            }
            
            log.info("Initial stock prices and history loaded: {} stocks", stocks.size());
        } catch (Exception e) {
            log.warn("Initial stock prices load failed: {}", e.getMessage());
        }
        
        // Fetch initial bond/fund/viop prices
        /* Mock data disabled by user request
        try {
            loadMockInstrumentPrices();
            log.info("Initial instrument prices loaded");
        } catch (Exception e) {
            log.warn("Initial instrument prices load failed: {}", e.getMessage());
        }
        */
    }
    
    /**
     * Fetch and save historical price data for a symbol
     */
    private void fetchAndSaveHistoricalData(String symbol, String apiKey, String baseUrl) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(180);
            
            List<PriceHistory> history = yahooFinanceClient.fetchHistoricalData(symbol, startDate, endDate, apiKey, baseUrl);
            
            for (PriceHistory ph : history) {
                marketDataService.savePriceHistory(ph);
            }
            
            log.info("Saved {} historical price records for {}", history.size(), symbol);
        } catch (Exception e) {
            log.warn("Failed to fetch historical data for {}: {}", symbol, e.getMessage());
        }
    }
    
    /**
     * Save daily price history entry
     */
    private void saveDailyPriceHistory(Instrument instrument, BigDecimal price) {
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
            log.warn("Failed to save price history for {}: {}", instrument.getSymbol(), e.getMessage());
        }
    }
    
    /**
     * Load mock prices for bonds, funds, and VIOP (real APIs require subscription)
     * In production, replace with real API calls
     */
    private void loadMockInstrumentPrices() {
        // Bonds
        updateInstrumentPrice("TBOND-2Y", new java.math.BigDecimal("98.50"), new java.math.BigDecimal("0.25"));
        updateInstrumentPrice("TBOND-5Y", new java.math.BigDecimal("95.30"), new java.math.BigDecimal("-0.15"));
        updateInstrumentPrice("TBOND-10Y", new java.math.BigDecimal("92.10"), new java.math.BigDecimal("-0.08"));
        updateInstrumentPrice("EUROBOND-30", new java.math.BigDecimal("87.25"), new java.math.BigDecimal("0.12"));
        
        // Funds
        updateInstrumentPrice("AFT", new java.math.BigDecimal("125.45"), new java.math.BigDecimal("1.25"));
        updateInstrumentPrice("IPB", new java.math.BigDecimal("98.70"), new java.math.BigDecimal("0.85"));
        updateInstrumentPrice("YAF", new java.math.BigDecimal("215.30"), new java.math.BigDecimal("0.45"));
        updateInstrumentPrice("TI2", new java.math.BigDecimal("112.80"), new java.math.BigDecimal("-0.32"));
        updateInstrumentPrice("GAF", new java.math.BigDecimal("198.60"), new java.math.BigDecimal("0.55"));
        
        // VIOP
        updateInstrumentPrice("F_XU030", new java.math.BigDecimal("9850.00"), new java.math.BigDecimal("1.15"));
        updateInstrumentPrice("F_USDTRY", new java.math.BigDecimal("43.25"), new java.math.BigDecimal("0.18"));
        updateInstrumentPrice("F_EURTRY", new java.math.BigDecimal("50.45"), new java.math.BigDecimal("0.22"));
        updateInstrumentPrice("F_GOLDTRY", new java.math.BigDecimal("2850.00"), new java.math.BigDecimal("0.35"));
    }
    
    private void updateInstrumentPrice(String symbol, java.math.BigDecimal price, java.math.BigDecimal mockChangePercent) {
        instrumentRepository.findBySymbol(symbol).ifPresent(instrument -> {
            // Calculate previousClose based on mockChangePercent to get correct calculated change
            java.math.BigDecimal prevClose = price.divide(
                java.math.BigDecimal.ONE.add(mockChangePercent.divide(new java.math.BigDecimal("100"), 6, java.math.RoundingMode.HALF_UP)),
                6, java.math.RoundingMode.HALF_UP
            );
            instrument.setPreviousClose(prevClose);
            instrument.setCurrentPrice(price);
            instrumentRepository.save(instrument);
        });
    }
}
