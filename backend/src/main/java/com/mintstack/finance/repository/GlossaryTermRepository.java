package com.mintstack.finance.repository;

import com.mintstack.finance.entity.GlossaryTerm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, UUID> {

    Optional<GlossaryTerm> findBySlugAndLocaleAndIsActiveTrue(String slug, String locale);

    Optional<GlossaryTerm> findBySlugAndLocale(String slug, String locale);

    @Query("""
        SELECT g FROM GlossaryTerm g
        WHERE g.isActive = true
          AND (:localeEnabled = false OR g.locale = :locale)
          AND (:categoryEnabled = false OR LOWER(g.category) = LOWER(:category))
          AND (
            :queryEnabled = false OR
            LOWER(g.term) LIKE CONCAT('%', LOWER(:query), '%') OR
            LOWER(g.definition) LIKE CONCAT('%', LOWER(:query), '%') OR
            LOWER(COALESCE(g.aliases, '')) LIKE CONCAT('%', LOWER(:query), '%')
          )
        ORDER BY g.sortOrder ASC, g.term ASC
        """)
    Page<GlossaryTerm> searchActive(
        @Param("query") String query,
        @Param("queryEnabled") boolean queryEnabled,
        @Param("category") String category,
        @Param("categoryEnabled") boolean categoryEnabled,
        @Param("locale") String locale,
        @Param("localeEnabled") boolean localeEnabled,
        Pageable pageable
    );
}
