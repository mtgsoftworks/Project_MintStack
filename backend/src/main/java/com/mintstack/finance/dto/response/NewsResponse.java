package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsResponse {

    private UUID id;
    
    private String title;
    
    private String summary;
    
    private String content;
    
    private String sourceUrl;
    
    private String sourceName;
    
    private String imageUrl;
    
    private String categoryName;
    
    private String categorySlug;
    
    private LocalDateTime publishedAt;
    
    private Boolean isFeatured;
    
    private Long viewCount;
}
