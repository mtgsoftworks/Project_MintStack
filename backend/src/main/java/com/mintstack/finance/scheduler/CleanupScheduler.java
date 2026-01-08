package com.mintstack.finance.scheduler;

import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.NewsRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CleanupScheduler {

    private final CurrencyRateRepository currencyRateRepository;
    private final NewsRepository newsRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    // Retention periods
    private static final int CURRENCY_RATE_RETENTION_DAYS = 90;
    private static final int NEWS_RETENTION_DAYS = 180;
    private static final int PRICE_HISTORY_RETENTION_DAYS = 365;

    /**
     * Clean old data at 2:00 AM every day
     */
    @Scheduled(cron = "${app.scheduler.cleanup-cron}")
    @Transactional
    public void cleanupOldData() {
        log.info("Starting data cleanup job");
        
        try {
            // Clean old currency rates
            LocalDateTime currencyRateCutoff = LocalDateTime.now().minusDays(CURRENCY_RATE_RETENTION_DAYS);
            currencyRateRepository.deleteByFetchedAtBefore(currencyRateCutoff);
            log.info("Cleaned currency rates older than {}", currencyRateCutoff);
            
            // Clean old news
            LocalDateTime newsCutoff = LocalDateTime.now().minusDays(NEWS_RETENTION_DAYS);
            newsRepository.deleteByPublishedAtBefore(newsCutoff);
            log.info("Cleaned news older than {}", newsCutoff);
            
            // Clean old price history
            LocalDate priceHistoryCutoff = LocalDate.now().minusDays(PRICE_HISTORY_RETENTION_DAYS);
            priceHistoryRepository.deleteByPriceDateBefore(priceHistoryCutoff);
            log.info("Cleaned price history older than {}", priceHistoryCutoff);
            
            log.info("Data cleanup completed successfully");
        } catch (Exception e) {
            log.error("Data cleanup failed", e);
        }
    }
}
