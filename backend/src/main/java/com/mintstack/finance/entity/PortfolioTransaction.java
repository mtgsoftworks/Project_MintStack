package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Portfolio transaction history entry.
 */
@Entity
@Table(name = "portfolio_transactions", indexes = {
    @Index(name = "idx_portfolio_transactions_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_portfolio_transactions_instrument", columnList = "instrument_id"),
    @Index(name = "idx_portfolio_transactions_date", columnList = "transaction_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "quantity", precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity;

    @Column(name = "price", precision = 18, scale = 6, nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    @Builder.Default
    private OrderType orderType = OrderType.MARKET;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.FILLED;

    @Column(name = "commission_amount", precision = 18, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "filled_quantity", precision = 18, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(name = "average_fill_price", precision = 18, scale = 6)
    private BigDecimal averageFillPrice;

    @Column(name = "filled_at")
    private java.time.LocalDateTime filledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "realized_profit_loss", precision = 18, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal realizedProfitLoss = BigDecimal.ZERO;

    @Column(name = "limit_price", precision = 18, scale = 6)
    private BigDecimal limitPrice;

    @Column(name = "stop_price", precision = 18, scale = 6)
    private BigDecimal stopPrice;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "notes", length = 500)
    private String notes;

    public enum TransactionType {
        BUY,
        SELL
    }

    public enum OrderType {
        MARKET,
        LIMIT,
        STOP
    }

    public enum OrderStatus {
        PENDING,
        PARTIALLY_FILLED,
        FILLED,
        CANCELED,
        REJECTED
    }
}
