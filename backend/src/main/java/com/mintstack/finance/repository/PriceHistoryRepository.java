package com.mintstack.finance.repository;

import com.mintstack.finance.entity.PriceHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    @Query(value = """
        SELECT id, instrument_id, open_price, high_price, low_price,
               close_price, adj_close, volume, price_date
        FROM (
            SELECT ph.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY ph.instrument_id
                       ORDER BY ph.price_date DESC
                   ) AS row_number
            FROM price_history ph
            WHERE ph.instrument_id IN (:instrumentIds)
        ) ranked
        WHERE ranked.row_number <= :historyLimit
        ORDER BY instrument_id, price_date DESC
        """, nativeQuery = true)
    List<PriceHistory> findRecentByInstrumentIds(
            @Param("instrumentIds") List<UUID> instrumentIds,
            @Param("historyLimit") int historyLimit);

    @Query(value = """
        SELECT DISTINCT ON (instrument_id)
               id, instrument_id, open_price, high_price, low_price,
               close_price, adj_close, volume, price_date
        FROM price_history
        WHERE instrument_id IN (:instrumentIds)
          AND price_date <= :priceDate
        ORDER BY instrument_id, price_date DESC
        """, nativeQuery = true)
    List<PriceHistory> findLatestAtOrBeforeByInstrumentIds(
            @Param("instrumentIds") List<UUID> instrumentIds,
            @Param("priceDate") LocalDate priceDate);

    @Query("""
        SELECT ph.instrument.id AS instrumentId,
               MAX(COALESCE(ph.highPrice, ph.closePrice, ph.openPrice)) AS week52High,
               MIN(COALESCE(ph.lowPrice, ph.closePrice, ph.openPrice)) AS week52Low
        FROM PriceHistory ph
        WHERE ph.instrument.id IN :instrumentIds
          AND ph.priceDate BETWEEN :startDate AND :endDate
        GROUP BY ph.instrument.id
        """)
    List<PriceRangeView> findPriceRangesByInstrumentIds(
            @Param("instrumentIds") List<UUID> instrumentIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<PriceHistory> findByInstrumentIdOrderByPriceDateDesc(UUID instrumentId);

    List<PriceHistory> findByInstrumentIdOrderByPriceDateDesc(UUID instrumentId, Pageable pageable);

    List<PriceHistory> findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
            UUID instrumentId, LocalDate startDate, LocalDate endDate);

    Optional<PriceHistory> findTopByInstrumentIdOrderByPriceDateDesc(UUID instrumentId);

    List<PriceHistory> findByInstrumentIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
            UUID instrumentId, LocalDate priceDate, Pageable pageable);

    List<PriceHistory> findByInstrumentIdAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
            UUID instrumentId, LocalDate priceDate, Pageable pageable);

    List<PriceHistory> findByInstrumentIdOrderByPriceDateAsc(
            UUID instrumentId, Pageable pageable);

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.instrument.symbol = :symbol " +
           "AND ph.priceDate BETWEEN :startDate AND :endDate ORDER BY ph.priceDate ASC")
    List<PriceHistory> findBySymbolAndDateRange(
            @Param("symbol") String symbol,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ph FROM PriceHistory ph WHERE ph.instrument.symbol = :symbol " +
           "ORDER BY ph.priceDate DESC")
    List<PriceHistory> findLatestBySymbol(@Param("symbol") String symbol, Pageable pageable);

    Optional<PriceHistory> findByInstrumentIdAndPriceDate(UUID instrumentId, LocalDate priceDate);

    boolean existsByInstrumentIdAndPriceDate(UUID instrumentId, LocalDate priceDate);

    boolean existsByInstrumentType(com.mintstack.finance.entity.Instrument.InstrumentType type);

    void deleteByPriceDateBefore(LocalDate date);

    interface PriceRangeView {
        UUID getInstrumentId();
        BigDecimal getWeek52High();
        BigDecimal getWeek52Low();
    }
}
