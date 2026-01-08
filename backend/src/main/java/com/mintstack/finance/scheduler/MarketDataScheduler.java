package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.external.TcmbApiClient;
import com.mintstack.finance.service.external.YahooFinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
     */
    @Scheduled(cron = "${app.scheduler.stock-prices-cron}")
    public void fetchStockPrices() {
        log.info("Starting stock prices fetch job");
        try {
            List<String> stockSymbols = instrumentRepository
                .findByTypeAndIsActiveTrue(Instrument.InstrumentType.STOCK)
                .stream()
                .map(Instrument::getSymbol)
                .collect(Collectors.toList());
            
            yahooFinanceClient.updateStockPrices(stockSymbols);
            log.info("Stock prices fetch completed: {} stocks updated", stockSymbols.size());
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
        try {
            // Fetch initial TCMB rates
            List<CurrencyRate> rates = tcmbApiClient.fetchTodayRates();
            marketDataService.saveCurrencyRates(rates);
            log.info("Initial TCMB rates loaded: {} rates", rates.size());
        } catch (Exception e) {
            log.warn("Initial TCMB rates load failed: {}", e.getMessage());
        }
    }
}
