package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.NewsResponse;
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

    @Cacheable(value = "news", key = "'latest'")
    @Transactional(readOnly = true)
    public List<NewsResponse> getLatestNews() {
        List<News> news = newsRepository.findTop5ByOrderByPublishedAtDesc();
        return news.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<NewsResponse> getAllNews(Pageable pageable) {
        Page<News> news = newsRepository.findAllByOrderByPublishedAtDesc(pageable);
        return news.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<NewsResponse> getNewsByCategory(String categorySlug, Pageable pageable) {
        Page<News> news = newsRepository.findByCategorySlugOrderByPublishedAtDesc(categorySlug, pageable);
        return news.map(this::mapToResponse);
    }

    @Transactional
    public NewsResponse getNewsById(UUID id) {
        News news = newsRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Haber", "id", id));
        
        // Increment view count
        news.setViewCount(news.getViewCount() + 1);
        newsRepository.save(news);
        
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
    public List<NewsCategory> getCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional
    public void saveNews(News news) {
        if (!newsRepository.existsBySourceUrl(news.getSourceUrl())) {
            newsRepository.save(news);
            log.debug("Saved news: {}", news.getTitle());
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
            .viewCount(news.getViewCount())
            .build();
    }
}
