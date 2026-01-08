package com.mintstack.finance.repository;

import com.mintstack.finance.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    List<Portfolio> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Portfolio> findByIdAndUserId(UUID id, UUID userId);

    Optional<Portfolio> findByUserIdAndIsDefaultTrue(UUID userId);

    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.items WHERE p.id = :id AND p.user.id = :userId")
    Optional<Portfolio> findByIdAndUserIdWithItems(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.items WHERE p.user.id = :userId")
    List<Portfolio> findByUserIdWithItems(@Param("userId") UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    long countByUserId(UUID userId);
}
