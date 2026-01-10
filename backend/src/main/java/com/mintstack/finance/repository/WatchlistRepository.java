package com.mintstack.finance.repository;

import com.mintstack.finance.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {

    List<Watchlist> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Watchlist> findByIdAndUserId(UUID id, UUID userId);

    Optional<Watchlist> findByUserIdAndIsDefaultTrue(UUID userId);

    @Query("SELECT w FROM Watchlist w LEFT JOIN FETCH w.items i LEFT JOIN FETCH i.instrument WHERE w.id = :id AND w.user.id = :userId")
    Optional<Watchlist> findByIdAndUserIdWithItems(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByUserIdAndName(UUID userId, String name);

    long countByUserId(UUID userId);
}
