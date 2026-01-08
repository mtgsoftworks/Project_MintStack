package com.mintstack.finance.repository;

import com.mintstack.finance.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, UUID> {

    List<PortfolioItem> findByPortfolioId(UUID portfolioId);

    Optional<PortfolioItem> findByIdAndPortfolioId(UUID id, UUID portfolioId);

    @Query("SELECT pi FROM PortfolioItem pi JOIN FETCH pi.instrument WHERE pi.portfolio.id = :portfolioId")
    List<PortfolioItem> findByPortfolioIdWithInstruments(@Param("portfolioId") UUID portfolioId);

    @Query("SELECT pi FROM PortfolioItem pi WHERE pi.portfolio.user.id = :userId")
    List<PortfolioItem> findAllByUserId(@Param("userId") UUID userId);

    boolean existsByPortfolioIdAndInstrumentId(UUID portfolioId, UUID instrumentId);

    void deleteByPortfolioId(UUID portfolioId);
}
