package com.mintstack.finance.scheduler;

import com.mintstack.finance.config.NewsFeedProperties;
import com.mintstack.finance.entity.News;
import com.mintstack.finance.repository.NewsRepository;
import com.mintstack.finance.service.external.RssNewsClient;
import com.mintstack.finance.service.simulation.SimulationDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsSchedulerTest {

    @Mock
    private RssNewsClient rssNewsClient;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private SimulationDataService simulationDataService;

    @Mock
    private NewsFeedProperties newsFeedProperties;

    private NewsScheduler newsScheduler;

    @BeforeEach
    void setUp() {
        newsScheduler = new NewsScheduler(
            rssNewsClient,
            newsRepository,
            simulationDataService,
            newsFeedProperties
        );
    }

    @Test
    void fetchNews_ShouldSkip_WhenSimulationEnabledAndFetchDisabled() {
        when(simulationDataService.isSimulationEnabled()).thenReturn(true);
        when(newsFeedProperties.isFetchWhenSimulationEnabled()).thenReturn(false);

        newsScheduler.fetchNews();

        verify(rssNewsClient, never()).fetchAllNews();
        verify(newsRepository, never()).save(any(News.class));
    }

    @Test
    void fetchNews_ShouldSaveOnlyUniqueNews_BySourceUrlAndHash() {
        when(simulationDataService.isSimulationEnabled()).thenReturn(false);

        News existingByUrl = News.builder()
            .title("Existing URL")
            .sourceUrl("https://example.com/news/1")
            .externalHash("hash-url")
            .publishedAt(LocalDateTime.now())
            .build();

        News existingByHash = News.builder()
            .title("Existing Hash")
            .sourceUrl("https://example.com/news/2")
            .externalHash("hash-dup")
            .publishedAt(LocalDateTime.now())
            .build();

        News unique = News.builder()
            .title("Unique")
            .sourceUrl("https://example.com/news/3")
            .externalHash("hash-unique")
            .publishedAt(LocalDateTime.now())
            .build();

        when(rssNewsClient.fetchAllNews()).thenReturn(List.of(existingByUrl, existingByHash, unique));
        when(newsRepository.existsBySourceUrl("https://example.com/news/1")).thenReturn(true);
        when(newsRepository.existsByExternalHash("hash-url")).thenReturn(false);
        when(newsRepository.existsBySourceUrl("https://example.com/news/2")).thenReturn(false);
        when(newsRepository.existsByExternalHash("hash-dup")).thenReturn(true);
        when(newsRepository.existsBySourceUrl("https://example.com/news/3")).thenReturn(false);
        when(newsRepository.existsByExternalHash("hash-unique")).thenReturn(false);

        newsScheduler.fetchNews();

        verify(newsRepository).save(unique);
        verify(newsRepository, never()).save(existingByUrl);
        verify(newsRepository, never()).save(existingByHash);
    }
}
