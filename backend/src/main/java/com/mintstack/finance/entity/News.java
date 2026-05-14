package com.mintstack.finance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "news", indexes = {
    @Index(name = "idx_news_category", columnList = "category_id"),
    @Index(name = "idx_news_published_at", columnList = "published_at"),
    @Index(name = "idx_news_source", columnList = "source_name"),
    @Index(name = "idx_news_simulated", columnList = "is_simulated")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private NewsCategory category;

    @NotBlank(message = "Haber başlığı boş olamaz")
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "summary", length = 1000)
    private String summary;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "external_hash", length = 64)
    private String externalHash;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "is_published")
    @Builder.Default
    private Boolean isPublished = true;

    @Column(name = "is_simulated")
    @Builder.Default
    private Boolean isSimulated = false;

    @Column(name = "llm_summary", length = 1000)
    private String llmSummary;

    @Column(name = "llm_sentiment", length = 32)
    private String llmSentiment;

    @Column(name = "llm_keywords", length = 500)
    private String llmKeywords;

    @Column(name = "llm_model", length = 120)
    private String llmModel;

    @Column(name = "llm_enriched_at")
    private LocalDateTime llmEnrichedAt;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;
}
