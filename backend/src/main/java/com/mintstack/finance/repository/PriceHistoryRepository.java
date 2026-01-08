package com.mintstack.finance.repository;

import com.mintstack.finance.entity.PriceHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    List<PriceHistory> findByInstrumentIdOrderByPriceDateDesc(UUID instrumentId);

    List<PriceHistory> findByInstrumentIdOrderByPriceDateDesc(UUID instrumentId, Pageable pageable);

    List<PriceHistory> findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
            UUID instrumentId, LocalDate startDate, LocalDate endDate);

    Optional<PriceHistory> findTopByInstrumentIdOrderByPriceDateDesc(UUID instrumentId);

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.instrument.symbol = :symbol " +
           "AND ph.priceDate BETWEEN :startDate AND :endDate ORDER BY ph.priceDate ASC")
    List<PriceHistory> findBySymbolAndDateRange(
            @Param("symbol") String symbol,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.instrument.symbol = :symbol " +
           "ORDER BY ph.priceDate DESC")
    List<PriceHistory> findLatestBySymbol(@Param("symbol") String symbol, Pageable pageable);

    boolean existsByInstrumentIdAndPriceDate(UUID instrumentId, LocalDate priceDate);

    void deleteByPriceDateBefore(LocalDate date);
}
