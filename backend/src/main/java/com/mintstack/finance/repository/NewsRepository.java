package com.mintstack.finance.repository;

import com.mintstack.finance.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NewsRepository extends JpaRepository<News, UUID> {

    Page<News> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Page<News> findByCategoryIdOrderByPublishedAtDesc(UUID categoryId, Pageable pageable);

    Page<News> findByCategorySlugOrderByPublishedAtDesc(String slug, Pageable pageable);

    List<News> findTop5ByOrderByPublishedAtDesc();

    List<News> findTop10ByIsFeaturedTrueOrderByPublishedAtDesc();

    @Query("SELECT n FROM News n WHERE " +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(n.summary) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY n.publishedAt DESC")
    Page<News> searchByTitleOrSummary(@Param("query") String query, Pageable pageable);

    boolean existsBySourceUrl(String sourceUrl);

    void deleteByPublishedAtBefore(LocalDateTime date);
}
