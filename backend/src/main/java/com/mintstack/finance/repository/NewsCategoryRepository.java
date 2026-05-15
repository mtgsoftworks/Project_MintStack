package com.mintstack.finance.repository;

import com.mintstack.finance.entity.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsCategoryRepository extends JpaRepository<NewsCategory, UUID> {

    Optional<NewsCategory> findBySlug(String slug);

    List<NewsCategory> findByIsActiveTrueOrderByDisplayOrderAsc();

    @Query("""
        SELECT DISTINCT c
        FROM NewsCategory c
        JOIN c.newsList n
        WHERE c.isActive = TRUE
          AND n.isPublished = TRUE
        ORDER BY c.displayOrder ASC
        """)
    List<NewsCategory> findActiveCategoriesWithPublishedNewsOrderByDisplayOrderAsc();

    boolean existsBySlug(String slug);

    boolean existsByName(String name);
}
