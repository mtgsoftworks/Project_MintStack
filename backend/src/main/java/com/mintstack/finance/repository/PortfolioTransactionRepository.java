package com.mintstack.finance.repository;

import com.mintstack.finance.entity.PortfolioTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, UUID> {

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
}
