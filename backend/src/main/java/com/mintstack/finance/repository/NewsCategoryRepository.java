package com.mintstack.finance.repository;

import com.mintstack.finance.entity.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NewsCategoryRepository extends JpaRepository<NewsCategory, UUID> {

    Optional<NewsCategory> findBySlug(String slug);

    List<NewsCategory> findByIsActiveTrueOrderByDisplayOrderAsc();

    boolean existsBySlug(String slug);

    boolean existsByName(String name);
}
