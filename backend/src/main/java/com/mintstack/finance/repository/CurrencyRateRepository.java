package com.mintstack.finance.repository;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.CurrencyRate.RateSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query("SELECT c FROM CurrencyRate c WHERE c.source = :source AND " +
           "c.fetchedAt = (SELECT MAX(c2.fetchedAt) FROM CurrencyRate c2 " +
           "WHERE c2.currencyCode = c.currencyCode AND c2.source = :source)")
    List<CurrencyRate> findLatestBySource(@Param("source") RateSource source);

    @Query("SELECT c FROM CurrencyRate c WHERE c.currencyCode = :code " +
           "AND c.fetchedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.fetchedAt ASC")
    List<CurrencyRate> findHistoryByCurrencyCode(
            @Param("code") String currencyCode,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    void deleteByFetchedAtBefore(LocalDateTime date);
}
