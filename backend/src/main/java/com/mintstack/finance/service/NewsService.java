package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.NewsResponse;
import com.mintstack.finance.dto.response.NewsCategoryResponse;
import com.mintstack.finance.entity.News;
import com.mintstack.finance.entity.NewsCategory;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.NewsCategoryRepository;
import com.mintstack.finance.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsCategoryRepository categoryRepository;
    private final com.mintstack.finance.scheduler.NewsScheduler newsScheduler;

    @Cacheable(value = "news", key = "'latest'")
    @Transactional(readOnly = true)
    public List<NewsResponse> getLatestNews() {
        List<News> news = newsRepository.findTop5ByIsPublishedTrueOrderByPublishedAtDesc();
        return news.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<NewsResponse> getAllNews(Pageable pageable) {
        Page<News> news = newsRepository.findByIsPublishedTrueOrderByPublishedAtDesc(pageable);
        return news.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<NewsResponse> getNewsByCategory(String categorySlug, Pageable pageable) {
        Page<News> news = newsRepository.findByCategorySlugAndIsPublishedTrueOrderByPublishedAtDesc(categorySlug, pageable);
        return news.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public NewsResponse getNewsById(UUID id) {
        News news = newsRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Haber", "id", id));

        return mapToResponse(news);
    }

    @Transactional(readOnly = true)
    public Page<NewsResponse> searchNews(String query, Pageable pageable) {
        Page<News> news = newsRepository.searchByTitleOrSummary(query, pageable);
        return news.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<NewsResponse> getFeaturedNews() {
        List<News> news = newsRepository.findTop10ByIsFeaturedTrueOrderByPublishedAtDesc();
        return news.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NewsCategoryResponse> getCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
            .stream()
            .map(this::mapToCategoryResponse)
            .collect(Collectors.toList());
    }

    @org.springframework.cache.annotation.CacheEvict(value = "news", allEntries = true)
    public void fetchNewsManually() {
        log.info("Manual news fetch requested");
        try {
            newsScheduler.fetchNews();
        } catch (Exception e) {
            log.warn("Manual news fetch exception: {}", e.getMessage());
        }
    }

    @Transactional
    public void saveNews(News news) {
        boolean existsByUrl = news.getSourceUrl() != null && newsRepository.existsBySourceUrl(news.getSourceUrl());
        boolean existsByHash = news.getExternalHash() != null && newsRepository.existsByExternalHash(news.getExternalHash());
        if (!existsByUrl && !existsByHash) {
            newsRepository.save(news);
            log.debug("Saved news: {}", news.getTitle());
        }
    }

    @Transactional
    public void incrementViewCount(UUID newsId) {
        int updated = newsRepository.incrementViewCount(newsId);
        if (updated > 0) {
            log.debug("Incremented view count for news: {}", newsId);
        }
    }

    private NewsResponse mapToResponse(News news) {
        NewsCategory category = news.getCategory();
        
        return NewsResponse.builder()
            .id(news.getId())
            .title(news.getTitle())
            .summary(news.getSummary())
            .content(news.getContent())
            .sourceUrl(news.getSourceUrl())
            .sourceName(news.getSourceName())
            .imageUrl(news.getImageUrl())
            .categoryName(category != null ? category.getName() : null)
            .categorySlug(category != null ? category.getSlug() : null)
            .publishedAt(news.getPublishedAt())
            .isFeatured(news.getIsFeatured())
            .isSimulated(Boolean.TRUE.equals(news.getIsSimulated()))
            .llmSummary(news.getLlmSummary())
            .llmSentiment(news.getLlmSentiment())
            .llmKeywords(news.getLlmKeywords())
            .llmModel(news.getLlmModel())
            .llmEnrichedAt(news.getLlmEnrichedAt())
            .viewCount(news.getViewCount())
            .build();
    }

    private NewsCategoryResponse mapToCategoryResponse(NewsCategory category) {
        return NewsCategoryResponse.builder()
            .id(category.getId())
            .name(category.getName())
            .slug(category.getSlug())
            .description(category.getDescription())
            .displayOrder(category.getDisplayOrder())
            .isActive(category.getIsActive())
            .build();
    }
}
