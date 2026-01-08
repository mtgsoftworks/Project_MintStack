package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.News;
import com.mintstack.finance.repository.NewsRepository;
import com.mintstack.finance.service.external.RssNewsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class NewsScheduler {

    private final RssNewsClient rssNewsClient;
    private final NewsRepository newsRepository;

    /**
     * Fetch news from RSS feeds every 30 minutes
     */
    @Scheduled(cron = "${app.scheduler.news-fetch-cron}")
    public void fetchNews() {
        log.info("Starting news fetch job");
        try {
            List<News> newsList = rssNewsClient.fetchAllNews();
            
            int saved = 0;
            for (News news : newsList) {
                // Check if news already exists by source URL
                if (news.getSourceUrl() != null && 
                    !newsRepository.existsBySourceUrl(news.getSourceUrl())) {
                    newsRepository.save(news);
                    saved++;
                }
            }
            
            log.info("News fetch completed: {} new articles saved out of {} fetched", saved, newsList.size());
        } catch (Exception e) {
            log.error("News fetch failed", e);
        }
    }

    /**
     * Initial news load on application startup
     */
    @Scheduled(initialDelay = 15000, fixedDelay = Long.MAX_VALUE)
    public void initialNewsLoad() {
        log.info("Starting initial news load");
        try {
            List<News> newsList = rssNewsClient.fetchAllNews();
            
            int saved = 0;
            for (News news : newsList) {
                if (news.getSourceUrl() != null && 
                    !newsRepository.existsBySourceUrl(news.getSourceUrl())) {
                    newsRepository.save(news);
                    saved++;
                }
            }
            
            log.info("Initial news load completed: {} articles saved", saved);
        } catch (Exception e) {
            log.warn("Initial news load failed: {}", e.getMessage());
        }
    }

    /**
     * Clean up old news (older than 30 days)
     */
    @Scheduled(cron = "${app.scheduler.cleanup-cron}")
    public void cleanupOldNews() {
        log.info("Starting news cleanup job");
        try {
            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(30);
            newsRepository.deleteByPublishedAtBefore(cutoffDate);
            log.info("News cleanup completed");
        } catch (Exception e) {
            log.error("News cleanup failed", e);
        }
    }
}
