package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.NewsResponse;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.service.NewsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsController.class)
@ActiveProfiles("test")
@DisplayName("NewsController Tests")
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    private NewsResponse createEconomyNews() {
        return NewsResponse.builder()
                .id(1L)
                .title("Merkez Bankası faiz kararı")
                .summary("TCMB politika faizi değiştirmedi")
                .content("Türkiye Cumhuriyet Merkez Bankası bugün yapılan toplantıda...")
                .categoryId(1L)
                .categoryName("Ekonomi")
                .categorySlug("ekonomi")
                .sourceUrl("https://example.com/news/1")
                .sourceName("Ekonomi Haberleri")
                .imageUrl("https://example.com/images/1.jpg")
                .publishedAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    private NewsResponse createStockNews() {
        return NewsResponse.builder()
                .id(2L)
                .title("BIST 100 rekor kırdı")
                .summary("Borsa tarihi zirveye ulaştı")
                .content("Borsa İstanbul'da işlem gören hisseler bugün...")
                .categoryId(2L)
                .categoryName("Borsa")
                .categorySlug("borsa")
                .sourceUrl("https://example.com/news/2")
                .sourceName("Borsa Haberleri")
                .imageUrl("https://example.com/images/2.jpg")
                .publishedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Nested
    @DisplayName("Public News Endpoints")
    class PublicNewsEndpoints {

        @Test
        @DisplayName("GET /api/v1/news - Should return news list (public)")
        void getNews_shouldReturnNews() throws Exception {
            // Given
            Page<NewsResponse> page = new PageImpl<>(
                    Arrays.asList(createStockNews(), createEconomyNews()),
                    PageRequest.of(0, 20),
                    2
            );
            when(newsService.getNews(any()))
                    .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/news")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[0].title").value("BIST 100 rekor kırdı"));
        }

        @Test
        @DisplayName("GET /api/v1/news - Should return empty page when no news")
        void getNews_whenNoNews_shouldReturnEmptyPage() throws Exception {
            // Given
            Page<NewsResponse> emptyPage = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(0, 20),
                    0
            );
            when(newsService.getNews(any()))
                    .thenReturn(emptyPage);

            // When/Then
            mockMvc.perform(get("/api/v1/news")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("GET /api/v1/news/{id} - Should return news by id (public)")
        void getNewsById_shouldReturnNews() throws Exception {
            // Given
            when(newsService.getNewsById(1L))
                    .thenReturn(createEconomyNews());

            // When/Then
            mockMvc.perform(get("/api/v1/news/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.title").value("Merkez Bankası faiz kararı"))
                    .andExpect(jsonPath("$.data.categoryName").value("Ekonomi"));
        }

        @Test
        @DisplayName("GET /api/v1/news/{id} - Should return 404 when news not found")
        void getNewsById_whenNotFound_shouldReturn404() throws Exception {
            // Given
            when(newsService.getNewsById(999L))
                    .thenThrow(new ResourceNotFoundException("Haber", "id", "999"));

            // When/Then
            mockMvc.perform(get("/api/v1/news/999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("News By Category Tests")
    class NewsByCategoryTests {

        @Test
        @DisplayName("GET /api/v1/news/category/{slug} - Should return news by category")
        void getNewsByCategory_shouldReturnCategoryNews() throws Exception {
            // Given
            Page<NewsResponse> page = new PageImpl<>(
                    Arrays.asList(createEconomyNews()),
                    PageRequest.of(0, 20),
                    1
            );
            when(newsService.getNewsByCategory(eq("ekonomi"), any()))
                    .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/news/category/ekonomi")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].categorySlug").value("ekonomi"));
        }

        @Test
        @DisplayName("GET /api/v1/news/category/{slug} - Should return 404 for invalid category")
        void getNewsByCategory_whenInvalidCategory_shouldReturn404() throws Exception {
            // Given
            when(newsService.getNewsByCategory(eq("invalid"), any()))
                    .thenThrow(new ResourceNotFoundException("Kategori", "slug", "invalid"));

            // When/Then
            mockMvc.perform(get("/api/v1/news/category/invalid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Search News Tests")
    class SearchNewsTests {

        @Test
        @DisplayName("GET /api/v1/news/search - Should search news by keyword")
        void searchNews_shouldReturnMatchingNews() throws Exception {
            // Given
            Page<NewsResponse> page = new PageImpl<>(
                    Arrays.asList(createEconomyNews()),
                    PageRequest.of(0, 20),
                    1
            );
            when(newsService.searchNews(eq("faiz"), any()))
                    .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/news/search")
                            .param("q", "faiz")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].title").value(containsString("faiz")));
        }

        @Test
        @DisplayName("GET /api/v1/news/search - Should return empty for no matches")
        void searchNews_whenNoMatches_shouldReturnEmpty() throws Exception {
            // Given
            Page<NewsResponse> emptyPage = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(0, 20),
                    0
            );
            when(newsService.searchNews(eq("xyz123"), any()))
                    .thenReturn(emptyPage);

            // When/Then
            mockMvc.perform(get("/api/v1/news/search")
                            .param("q", "xyz123")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("GET /api/v1/news/search - Should return 400 when query is missing")
        void searchNews_whenQueryMissing_shouldReturn400() throws Exception {
            mockMvc.perform(get("/api/v1/news/search")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Pagination Tests")
    class PaginationTests {

        @Test
        @DisplayName("GET /api/v1/news - Should support pagination parameters")
        void getNews_shouldSupportPagination() throws Exception {
            // Given
            Page<NewsResponse> page = new PageImpl<>(
                    Arrays.asList(createEconomyNews()),
                    PageRequest.of(1, 5),
                    11
            );
            when(newsService.getNews(any()))
                    .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/news")
                            .param("page", "1")
                            .param("size", "5")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pageable.pageNumber").value(1))
                    .andExpect(jsonPath("$.data.pageable.pageSize").value(5))
                    .andExpect(jsonPath("$.data.totalElements").value(11))
                    .andExpect(jsonPath("$.data.totalPages").value(3));
        }
    }

    @Nested
    @DisplayName("Category List Tests")
    class CategoryListTests {

        @Test
        @DisplayName("GET /api/v1/news/categories - Should return all categories")
        void getCategories_shouldReturnCategories() throws Exception {
            // Given
            var categories = Arrays.asList(
                    new NewsService.CategoryDTO(1L, "Ekonomi", "ekonomi"),
                    new NewsService.CategoryDTO(2L, "Borsa", "borsa"),
                    new NewsService.CategoryDTO(3L, "Dünya", "dunya")
            );
            when(newsService.getCategories())
                    .thenReturn(categories);

            // When/Then
            mockMvc.perform(get("/api/v1/news/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)));
        }
    }

    @Nested
    @DisplayName("Latest News Tests")
    class LatestNewsTests {

        @Test
        @DisplayName("GET /api/v1/news/latest - Should return latest news per category")
        void getLatestNews_shouldReturnLatest() throws Exception {
            // Given
            var latestNews = Arrays.asList(createEconomyNews(), createStockNews());
            when(newsService.getLatestNewsPerCategory())
                    .thenReturn(latestNews);

            // When/Then
            mockMvc.perform(get("/api/v1/news/latest")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("Authenticated News Endpoints")
    class AuthenticatedNewsEndpoints {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/v1/news/bookmarks - Should return bookmarked news")
        void getBookmarks_shouldReturnBookmarks() throws Exception {
            // This endpoint might require authentication
            // Given
            when(newsService.getBookmarkedNews(any(), any()))
                    .thenReturn(new PageImpl<>(Arrays.asList(createEconomyNews())));

            // When/Then
            mockMvc.perform(get("/api/v1/news/bookmarks")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }
}
