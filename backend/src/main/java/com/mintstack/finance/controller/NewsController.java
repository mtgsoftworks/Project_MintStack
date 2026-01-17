package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.NewsResponse;
import com.mintstack.finance.dto.response.PaginationInfo;
import com.mintstack.finance.entity.NewsCategory;
import com.mintstack.finance.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "Haber API'leri")
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    @Operation(summary = "Haberleri listele")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getNews(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<NewsResponse> news;
        
        if (search != null && !search.isEmpty()) {
            news = newsService.searchNews(search, pageable);
        } else if (category != null && !category.isEmpty()) {
            news = newsService.getNewsByCategory(category, pageable);
        } else {
            news = newsService.getAllNews(pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success(news.getContent(), PaginationInfo.from(news)));
    }

    @GetMapping("/search")
    @Operation(summary = "Haberlerde ara")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> searchNews(
            @RequestParam(required = false, name = "query") String query,
            @RequestParam(required = false, name = "q") String q,
            @PageableDefault(size = 10) Pageable pageable) {

        String searchTerm = (query != null && !query.isBlank()) ? query : q;
        Page<NewsResponse> news = (searchTerm == null || searchTerm.isBlank())
            ? newsService.getAllNews(pageable)
            : newsService.searchNews(searchTerm, pageable);

        return ResponseEntity.ok(ApiResponse.success(news.getContent(), PaginationInfo.from(news)));
    }

    @GetMapping("/category/{slug}")
    @Operation(summary = "Kategoriye göre haberleri getir")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getNewsByCategory(
            @PathVariable String slug,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<NewsResponse> news = newsService.getNewsByCategory(slug, pageable);
        return ResponseEntity.ok(ApiResponse.success(news.getContent(), PaginationInfo.from(news)));
    }

    @GetMapping("/latest")
    @Operation(summary = "Son haberleri getir (5 adet)")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getLatestNews() {
        List<NewsResponse> news = newsService.getLatestNews();
        return ResponseEntity.ok(ApiResponse.success(news));
    }

    @GetMapping("/featured")
    @Operation(summary = "Öne çıkan haberleri getir")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getFeaturedNews() {
        List<NewsResponse> news = newsService.getFeaturedNews();
        return ResponseEntity.ok(ApiResponse.success(news));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Haber detayını getir")
    public ResponseEntity<ApiResponse<NewsResponse>> getNewsById(@PathVariable UUID id) {
        NewsResponse news = newsService.getNewsById(id);
        return ResponseEntity.ok(ApiResponse.success(news));
    }

    @GetMapping("/categories")
    @Operation(summary = "Haber kategorilerini getir")
    public ResponseEntity<ApiResponse<List<NewsCategory>>> getCategories() {
        List<NewsCategory> categories = newsService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
