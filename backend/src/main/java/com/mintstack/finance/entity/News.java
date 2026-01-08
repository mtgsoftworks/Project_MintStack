package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "news", indexes = {
    @Index(name = "idx_news_category", columnList = "category_id"),
    @Index(name = "idx_news_published_at", columnList = "published_at"),
    @Index(name = "idx_news_source", columnList = "source_name")
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

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;
}
