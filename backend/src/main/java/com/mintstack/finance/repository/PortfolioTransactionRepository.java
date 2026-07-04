package com.mintstack.finance.repository;

import com.mintstack.finance.entity.PortfolioTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, UUID> {

    boolean existsByPortfolioId(UUID portfolioId);

    void deleteByPortfolioId(UUID portfolioId);

    @Query(
        value = "SELECT pt FROM PortfolioTransaction pt " +
            "JOIN pt.instrument instrument " +
            "WHERE pt.portfolio.id = :portfolioId AND pt.portfolio.user.id = :userId " +
            "ORDER BY pt.transactionDate DESC, pt.createdAt DESC",
        countQuery = "SELECT COUNT(pt) FROM PortfolioTransaction pt " +
            "WHERE pt.portfolio.id = :portfolioId AND pt.portfolio.user.id = :userId"
    )
    Page<PortfolioTransaction> findByPortfolioIdAndUserId(
        @Param("portfolioId") UUID portfolioId,
        @Param("userId") UUID userId,
        Pageable pageable
    );

    @Query(
        value = "SELECT pt FROM PortfolioTransaction pt " +
            "JOIN pt.instrument instrument " +
            "WHERE pt.portfolio.id = :portfolioId AND pt.portfolio.user.id = :userId " +
            "AND pt.orderStatus = :orderStatus " +
            "ORDER BY pt.transactionDate DESC, pt.createdAt DESC",
        countQuery = "SELECT COUNT(pt) FROM PortfolioTransaction pt " +
            "WHERE pt.portfolio.id = :portfolioId AND pt.portfolio.user.id = :userId " +
            "AND pt.orderStatus = :orderStatus"
    )
    Page<PortfolioTransaction> findByPortfolioIdAndUserIdAndOrderStatus(
        @Param("portfolioId") UUID portfolioId,
        @Param("userId") UUID userId,
        @Param("orderStatus") PortfolioTransaction.OrderStatus orderStatus,
        Pageable pageable
    );

    Optional<PortfolioTransaction> findByIdAndPortfolioId(UUID id, UUID portfolioId);

    List<PortfolioTransaction> findByPortfolioIdAndOrderStatusInOrderByCreatedAtAsc(
        UUID portfolioId,
        List<PortfolioTransaction.OrderStatus> statuses
    );

    @Query("SELECT COALESCE(SUM(pt.realizedProfitLoss), 0) FROM PortfolioTransaction pt " +
        "WHERE pt.portfolio.id = :portfolioId AND pt.orderStatus = com.mintstack.finance.entity.PortfolioTransaction.OrderStatus.FILLED")
    BigDecimal sumRealizedProfitLossByPortfolioId(@Param("portfolioId") UUID portfolioId);

    @Query("SELECT COALESCE(SUM(pt.realizedProfitLoss), 0) FROM PortfolioTransaction pt " +
        "WHERE pt.portfolio.user.id = :userId AND pt.orderStatus = com.mintstack.finance.entity.PortfolioTransaction.OrderStatus.FILLED")
    BigDecimal sumRealizedProfitLossByUserId(@Param("userId") UUID userId);
}
