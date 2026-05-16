package com.mintstack.finance.repository;

import com.mintstack.finance.entity.News;
import com.mintstack.finance.entity.NewsCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsRepository extends JpaRepository<News, UUID> {

    Page<News> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Page<News> findByIsPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    Page<News> findByCategoryAndIsPublishedTrueOrderByPublishedAtDesc(NewsCategory category, Pageable pageable);

    Page<News> findByCategoryIdOrderByPublishedAtDesc(UUID categoryId, Pageable pageable);

    Page<News> findByCategorySlugOrderByPublishedAtDesc(String slug, Pageable pageable);

    Page<News> findByCategorySlugAndIsPublishedTrueOrderByPublishedAtDesc(String slug, Pageable pageable);

    List<News> findTop5ByOrderByPublishedAtDesc();

    List<News> findTop5ByIsPublishedTrueOrderByPublishedAtDesc();

    List<News> findTop10ByIsFeaturedTrueOrderByPublishedAtDesc();

    @Query("SELECT n FROM News n WHERE n.isPublished = TRUE AND (" +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(n.summary) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY n.publishedAt DESC")
    Page<News> searchByTitleOrSummary(@Param("query") String query, Pageable pageable);

    boolean existsBySourceUrl(String sourceUrl);

    boolean existsByExternalHash(String externalHash);

    Optional<News> findBySourceUrl(String sourceUrl);

    Optional<News> findByExternalHash(String externalHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE News n SET n.viewCount = COALESCE(n.viewCount, 0) + 1 WHERE n.id = :id")
    int incrementViewCount(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE News n SET n.summary = n.title WHERE n.summary IS NULL OR TRIM(n.summary) = ''")
    int fillMissingSummaryWithTitle();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE News n SET n.content = COALESCE(n.summary, n.title) WHERE n.content IS NULL OR TRIM(n.content) = ''")
    int fillMissingContentWithSummary();

    List<News> findBySourceUrlIsNullAndSourceNameIn(List<String> sourceNames);

    void deleteByPublishedAtBefore(LocalDateTime date);

    void deleteBySourceName(String sourceName);

    void deleteBySourceNameStartingWith(String sourceNamePrefix);

    void deleteByIsSimulatedTrue();
}
