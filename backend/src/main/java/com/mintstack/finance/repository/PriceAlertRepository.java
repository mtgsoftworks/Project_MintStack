package com.mintstack.finance.repository;

import com.mintstack.finance.entity.PriceAlert;
import com.mintstack.finance.entity.PriceAlert.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, UUID> {

    List<PriceAlert> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<PriceAlert> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<PriceAlert> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT a FROM PriceAlert a WHERE a.isActive = true AND a.isTriggered = false")
    List<PriceAlert> findActiveAlerts();

    @Query("SELECT a FROM PriceAlert a JOIN FETCH a.instrument WHERE a.instrument.id = :instrumentId AND a.isActive = true")
    List<PriceAlert> findActiveAlertsByInstrumentId(@Param("instrumentId") UUID instrumentId);

    @Query("SELECT a FROM PriceAlert a JOIN FETCH a.instrument WHERE a.instrument.symbol = :symbol AND a.isActive = true")
    List<PriceAlert> findActiveAlertsBySymbol(@Param("symbol") String symbol);

    @Query("SELECT a FROM PriceAlert a WHERE a.isTriggered = true AND a.notificationSent = false")
    List<PriceAlert> findTriggeredAlertsNotNotified();

    long countByUserIdAndIsActiveTrue(UUID userId);

    List<PriceAlert> findByUserIdAndAlertType(UUID userId, AlertType alertType);
}
