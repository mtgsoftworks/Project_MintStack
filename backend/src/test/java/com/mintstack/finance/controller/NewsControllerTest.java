package com.mintstack.finance.controller;

import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.config.SecurityConfig;
import com.mintstack.finance.dto.response.NewsCategoryResponse;
import com.mintstack.finance.dto.response.NewsResponse;
import com.mintstack.finance.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NewsController.class)
@Import({CorsProperties.class, SecurityConfig.class})

class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsService newsService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void getAllNews_ShouldReturnNews() throws Exception {
        // Given
        NewsResponse news = NewsResponse.builder()
            .id(UUID.randomUUID())
            .title("Test Haber")
            .summary("Test özet")
            .publishedAt(LocalDateTime.now())
            .categoryName("Ekonomi")
            .build();

        Page<NewsResponse> newsPage = new PageImpl<>(List.of(news), PageRequest.of(0, 10), 1);
        when(newsService.getAllNews(any())).thenReturn(newsPage);

        // When & Then
        mockMvc.perform(get("/api/v1/news"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].title").value("Test Haber"));
    }

    @Test
    void getNewsById_ShouldReturnNews() throws Exception {
        // Given
        UUID newsId = UUID.randomUUID();
        NewsResponse news = NewsResponse.builder()
            .id(newsId)
            .title("Test Haber")
            .summary("Test özet")
            .content("Test içerik")
            .publishedAt(LocalDateTime.now())
            .categoryName("Ekonomi")
            .build();

        when(newsService.getNewsById(newsId)).thenReturn(news);

        // When & Then
        mockMvc.perform(get("/api/v1/news/" + newsId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("Test Haber"));
    }

    @Test
    void getNewsByCategory_ShouldReturnFilteredNews() throws Exception {
        // Given
        String categorySlug = "ekonomi";
        NewsResponse news = NewsResponse.builder()
            .id(UUID.randomUUID())
            .title("Ekonomi Haberi")
            .summary("Ekonomi özet")
            .publishedAt(LocalDateTime.now())
            .categoryName("Ekonomi")
            .build();

        Page<NewsResponse> newsPage = new PageImpl<>(List.of(news), PageRequest.of(0, 10), 1);
        when(newsService.getNewsByCategory(any(String.class), any())).thenReturn(newsPage);

        // When & Then
        mockMvc.perform(get("/api/v1/news/category/" + categorySlug))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void searchNews_ShouldReturnMatchingNews() throws Exception {
        // Given
        NewsResponse news = NewsResponse.builder()
            .id(UUID.randomUUID())
            .title("Döviz Haberi")
            .summary("Döviz özet")
            .publishedAt(LocalDateTime.now())
            .categoryName("Ekonomi")
            .build();

        Page<NewsResponse> newsPage = new PageImpl<>(List.of(news), PageRequest.of(0, 10), 1);
        when(newsService.searchNews(any(String.class), any())).thenReturn(newsPage);

        // When & Then
        mockMvc.perform(get("/api/v1/news/search").param("query", "döviz"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getCategories_ShouldReturnCategoryDtos() throws Exception {
        NewsCategoryResponse category = NewsCategoryResponse.builder()
            .id(UUID.randomUUID())
            .name("Ekonomi")
            .slug("ekonomi")
            .description("Ekonomi haberleri")
            .displayOrder(1)
            .isActive(true)
            .build();

        when(newsService.getCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/news/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].slug").value("ekonomi"));
    }
}
