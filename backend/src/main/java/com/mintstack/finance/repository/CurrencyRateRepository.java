package com.mintstack.finance.repository;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.CurrencyRate.RateSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, UUID> {

    Optional<CurrencyRate> findTopByCurrencyCodeOrderByFetchedAtDesc(String currencyCode);

    Optional<CurrencyRate> findTopByCurrencyCodeAndSourceOrderByFetchedAtDesc(
            String currencyCode, RateSource source);

    List<CurrencyRate> findBySourceOrderByCurrencyCodeAsc(RateSource source);

    List<CurrencyRate> findBySource(RateSource source);

    List<CurrencyRate> findBySourceOrderByFetchedAtDesc(RateSource source);

    Optional<CurrencyRate> findByCurrencyCodeAndSource(String currencyCode, RateSource source);

    @Query("SELECT c FROM CurrencyRate c WHERE c.currencyCode = :code " +
           "AND c.source = :source AND c.rateDate < :rateDate " +
           "AND (c.sellingRate > :minimumRate OR c.buyingRate > :minimumRate) " +
           "ORDER BY c.rateDate DESC, c.fetchedAt DESC")
    List<CurrencyRate> findPreviousRatesByRateDate(
            @Param("code") String currencyCode,
            @Param("source") RateSource source,
            @Param("rateDate") LocalDateTime rateDate,
            @Param("minimumRate") BigDecimal minimumRate,
            Pageable pageable);

    @Query("SELECT c FROM CurrencyRate c WHERE c.currencyCode = :code " +
           "AND c.source = :source AND c.fetchedAt <= :at " +
           "AND (c.sellingRate > :minimumRate OR c.buyingRate > :minimumRate) " +
           "ORDER BY c.fetchedAt DESC")
    List<CurrencyRate> findLatestAtOrBefore(
            @Param("code") String currencyCode,
            @Param("source") RateSource source,
            @Param("at") LocalDateTime at,
            @Param("minimumRate") BigDecimal minimumRate,
            Pageable pageable);

    @Query("SELECT c FROM CurrencyRate c WHERE c.source = :source AND " +
           "c.fetchedAt = (SELECT MAX(c2.fetchedAt) FROM CurrencyRate c2 " +
           "WHERE c2.currencyCode = c.currencyCode AND c2.source = :source)")
    List<CurrencyRate> findLatestBySource(@Param("source") RateSource source);

    @Query("SELECT c FROM CurrencyRate c WHERE " +
           "c.fetchedAt = (SELECT MAX(c2.fetchedAt) FROM CurrencyRate c2 " +
           "WHERE c2.currencyCode = c.currencyCode)")
    List<CurrencyRate> findAllLatest();

    @Query("SELECT c FROM CurrencyRate c WHERE c.currencyCode = :code " +
           "AND c.source = :source AND c.fetchedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.fetchedAt ASC")
    List<CurrencyRate> findHistoryByCurrencyCode(
            @Param("code") String currencyCode,
            @Param("source") RateSource source,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    void deleteByFetchedAtBefore(LocalDateTime date);

    @Query("SELECT c FROM CurrencyRate c WHERE c.currencyCode = :code " +
           "AND c.fetchedAt < :beforeDate " +
           "ORDER BY c.fetchedAt DESC LIMIT 1")
    Optional<CurrencyRate> findPreviousRate(
            @Param("code") String currencyCode,
            @Param("beforeDate") LocalDateTime beforeDate);

    void deleteAll();
}
