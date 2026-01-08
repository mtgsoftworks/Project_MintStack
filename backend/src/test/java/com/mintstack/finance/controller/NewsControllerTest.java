package com.mintstack.finance.controller;

import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.dto.response.NewsResponse;
import com.mintstack.finance.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsController.class)
@Import(CorsProperties.class)
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

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
            .andExpect(jsonPath("$.data.content[0].title").value("Test Haber"));
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
        mockMvc.perform(get("/api/v1/news/search").param("keyword", "döviz"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
