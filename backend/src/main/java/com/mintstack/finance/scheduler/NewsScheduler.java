package com.mintstack.finance.scheduler;

import com.mintstack.finance.config.NewsFeedProperties;
import com.mintstack.finance.entity.News;
import com.mintstack.finance.repository.NewsRepository;
import com.mintstack.finance.service.NewsEnrichmentService;
import com.mintstack.finance.service.external.RssNewsClient;
import com.mintstack.finance.service.simulation.SimulationDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class NewsScheduler {

    private final RssNewsClient rssNewsClient;
    private final NewsRepository newsRepository;
    private final NewsEnrichmentService newsEnrichmentService;
    private final SimulationDataService simulationDataService;
    private final NewsFeedProperties newsFeedProperties;

    @Scheduled(cron = "${app.scheduler.news-fetch-cron}")
    public void fetchNews() {
        if (simulationDataService.isSimulationEnabled() && !newsFeedProperties.isFetchWhenSimulationEnabled()) {
            log.debug("Simulation mode active and real news fetch disabled. Skipping news fetch.");
            return;
        }

        log.info("Starting news fetch job");
        try {
            List<News> newsList = rssNewsClient.fetchAllNews();
            int saved = 0;
            int enriched = 0;
            for (News news : newsList) {
                if (shouldSave(news)) {
                    News prepared = newsEnrichmentService.enrichIfEnabled(news);
                    if (prepared.getLlmEnrichedAt() != null) {
                        enriched++;
                    }
                    newsRepository.save(prepared);
                    saved++;
                }
            }
            log.info("News fetch completed: {} new articles saved out of {} fetched (llmEnriched={})", saved, newsList.size(), enriched);
        } catch (Exception e) {
            log.error("News fetch failed", e);
        }
    }

    @Scheduled(initialDelay = 15000, fixedDelay = Long.MAX_VALUE)
    public void initialNewsLoad() {
        if (simulationDataService.isSimulationEnabled() && !newsFeedProperties.isFetchWhenSimulationEnabled()) {
            log.debug("Simulation mode active and real news fetch disabled. Skipping initial news load.");
            return;
        }

        log.info("Starting initial news load");
        try {
            List<News> newsList = rssNewsClient.fetchAllNews();
            int saved = 0;
            int enriched = 0;
            for (News news : newsList) {
                if (shouldSave(news)) {
                    News prepared = newsEnrichmentService.enrichIfEnabled(news);
                    if (prepared.getLlmEnrichedAt() != null) {
                        enriched++;
                    }
                    newsRepository.save(prepared);
                    saved++;
                }
            }
            log.info("Initial news load completed: {} articles saved (llmEnriched={})", saved, enriched);
        } catch (Exception e) {
            log.warn("Initial news load failed: {}", e.getMessage());
        }
    }

    private boolean shouldSave(News news) {
        boolean existsByUrl = StringUtils.hasText(news.getSourceUrl()) && newsRepository.existsBySourceUrl(news.getSourceUrl());
        boolean existsByHash = StringUtils.hasText(news.getExternalHash()) && newsRepository.existsByExternalHash(news.getExternalHash());
        return !existsByUrl && !existsByHash;
    }
}
