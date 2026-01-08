package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.NewsResponse;
import com.mintstack.finance.entity.News;
import com.mintstack.finance.entity.NewsCategory;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.NewsCategoryRepository;
import com.mintstack.finance.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsCategoryRepository newsCategoryRepository;

    @InjectMocks
    private NewsService newsService;

    private NewsCategory testCategory;
    private News testNews;

    @BeforeEach
    void setUp() {
        testCategory = NewsCategory.builder()
            .name("Ekonomi")
            .slug("ekonomi")
            .description("Ekonomi haberleri")
            .build();
        testCategory.setId(UUID.randomUUID());

        testNews = News.builder()
            .title("Test Haber")
            .summary("Test haber özeti")
            .content("Test haber içeriği")
            .sourceUrl("https://example.com/news/1")
            .imageUrl("https://example.com/image.jpg")
            .category(testCategory)
            .publishedAt(LocalDateTime.now())
            .build();
        testNews.setId(UUID.randomUUID());
    }

    @Test
    void getAllNews_ShouldReturnPaginatedNews() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));
        
        when(newsRepository.findAllByOrderByPublishedAtDesc(pageable))
            .thenReturn(newsPage);

        // When
        Page<NewsResponse> result = newsService.getAllNews(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Haber");
    }

    @Test
    void getNewsById_ShouldReturnNews_WhenExists() {
        // Given
        UUID newsId = testNews.getId();
        when(newsRepository.findById(newsId)).thenReturn(Optional.of(testNews));

        // When
        NewsResponse result = newsService.getNewsById(newsId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Haber");
    }

    @Test
    void getNewsById_ShouldThrowException_WhenNotFound() {
        // Given
        UUID newsId = UUID.randomUUID();
        when(newsRepository.findById(newsId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> newsService.getNewsById(newsId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getNewsByCategory_ShouldReturnFilteredNews() {
        // Given
        String categorySlug = "ekonomi";
        Pageable pageable = PageRequest.of(0, 10);
        Page<News> newsPage = new PageImpl<>(List.of(testNews));

        when(newsRepository.findByCategorySlugOrderByPublishedAtDesc(categorySlug, pageable))
            .thenReturn(newsPage);

        // When
        Page<NewsResponse> result = newsService.getNewsByCategory(categorySlug, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void service_ShouldBeInjected() {
        assertThat(newsService).isNotNull();
    }
}
