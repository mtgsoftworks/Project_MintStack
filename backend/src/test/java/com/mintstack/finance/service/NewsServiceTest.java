package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.NewsResponse;
import com.mintstack.finance.entity.News;
import com.mintstack.finance.entity.NewsCategory;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.NewsCategoryRepository;
import com.mintstack.finance.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NewsService Tests")
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsCategoryRepository newsCategoryRepository;

    @InjectMocks
    private NewsService newsService;

    private NewsCategory economyCategory;
    private NewsCategory stockCategory;
    private News newsItem1;
    private News newsItem2;

    @BeforeEach
    void setUp() {
        economyCategory = NewsCategory.builder()
                .id(1L)
                .name("Ekonomi")
                .slug("ekonomi")
                .build();

        stockCategory = NewsCategory.builder()
                .id(2L)
                .name("Borsa")
                .slug("borsa")
                .build();

        newsItem1 = News.builder()
                .id(1L)
                .title("Merkez Bankası faiz kararı")
                .summary("TCMB politika faizi değiştirmedi")
                .content("Türkiye Cumhuriyet Merkez Bankası...")
                .category(economyCategory)
                .sourceUrl("https://example.com/news/1")
                .sourceName("Ekonomi Haberleri")
                .publishedAt(LocalDateTime.now().minusHours(2))
                .imageUrl("https://example.com/images/1.jpg")
                .build();

        newsItem2 = News.builder()
                .id(2L)
                .title("BIST 100 rekor kırdı")
                .summary("Borsa tarihi zirveye ulaştı")
                .content("Borsa İstanbul'da işlem gören hisseler...")
                .category(stockCategory)
                .sourceUrl("https://example.com/news/2")
                .sourceName("Borsa Haberleri")
                .publishedAt(LocalDateTime.now().minusHours(1))
                .imageUrl("https://example.com/images/2.jpg")
                .build();
    }

    @Nested
    @DisplayName("Get News Tests")
    class GetNewsTests {

        @Test
        @DisplayName("Should return paginated news")
        void getNews_shouldReturnPaginatedNews() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<News> page = new PageImpl<>(Arrays.asList(newsItem1, newsItem2), pageable, 2);
            when(newsRepository.findAllByOrderByPublishedAtDesc(pageable))
                    .thenReturn(page);

            // When
            Page<NewsResponse> result = newsService.getNews(pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return empty page when no news")
        void getNews_whenNoNews_shouldReturnEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<News> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(newsRepository.findAllByOrderByPublishedAtDesc(pageable))
                    .thenReturn(emptyPage);

            // When
            Page<NewsResponse> result = newsService.getNews(pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should return news by id")
        void getNewsById_shouldReturnNews() {
            // Given
            when(newsRepository.findById(1L))
                    .thenReturn(Optional.of(newsItem1));

            // When
            NewsResponse result = newsService.getNewsById(1L);

            // Then
            assertThat(result.getTitle()).isEqualTo("Merkez Bankası faiz kararı");
            assertThat(result.getCategoryName()).isEqualTo("Ekonomi");
        }

        @Test
        @DisplayName("Should throw exception when news not found")
        void getNewsById_whenNotFound_shouldThrowException() {
            // Given
            when(newsRepository.findById(999L))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> newsService.getNewsById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get News By Category Tests")
    class GetNewsByCategoryTests {

        @Test
        @DisplayName("Should return news by category slug")
        void getNewsByCategory_shouldReturnCategoryNews() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<News> page = new PageImpl<>(Arrays.asList(newsItem1), pageable, 1);
            
            when(newsCategoryRepository.findBySlug("ekonomi"))
                    .thenReturn(Optional.of(economyCategory));
            when(newsRepository.findByCategoryIdOrderByPublishedAtDesc(1L, pageable))
                    .thenReturn(page);

            // When
            Page<NewsResponse> result = newsService.getNewsByCategory("ekonomi", pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("Ekonomi");
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void getNewsByCategory_whenCategoryNotFound_shouldThrowException() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(newsCategoryRepository.findBySlug("invalid"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> newsService.getNewsByCategory("invalid", pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Search News Tests")
    class SearchNewsTests {

        @Test
        @DisplayName("Should search news by keyword")
        void searchNews_shouldReturnMatchingNews() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<News> page = new PageImpl<>(Arrays.asList(newsItem1), pageable, 1);
            
            when(newsRepository.searchByKeyword("faiz", pageable))
                    .thenReturn(page);

            // When
            Page<NewsResponse> result = newsService.searchNews("faiz", pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).contains("faiz");
        }

        @Test
        @DisplayName("Should return empty when no matching news")
        void searchNews_whenNoMatch_shouldReturnEmpty() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<News> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            
            when(newsRepository.searchByKeyword("xyz123", pageable))
                    .thenReturn(emptyPage);

            // When
            Page<NewsResponse> result = newsService.searchNews("xyz123", pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Category Tests")
    class CategoryTests {

        @Test
        @DisplayName("Should return all categories")
        void getCategories_shouldReturnAllCategories() {
            // Given
            when(newsCategoryRepository.findAll())
                    .thenReturn(Arrays.asList(economyCategory, stockCategory));

            // When
            var result = newsService.getCategories();

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return latest news per category")
        void getLatestNewsPerCategory_shouldReturnLatest() {
            // Given
            when(newsCategoryRepository.findAll())
                    .thenReturn(Arrays.asList(economyCategory, stockCategory));
            when(newsRepository.findTopByCategoryIdOrderByPublishedAtDesc(1L))
                    .thenReturn(Optional.of(newsItem1));
            when(newsRepository.findTopByCategoryIdOrderByPublishedAtDesc(2L))
                    .thenReturn(Optional.of(newsItem2));

            // When
            var result = newsService.getLatestNewsPerCategory();

            // Then
            assertThat(result).hasSize(2);
        }
    }
}
