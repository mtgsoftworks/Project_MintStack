package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "portfolio_items", indexes = {
    @Index(name = "idx_portfolio_items_portfolio", columnList = "portfolio_id"),
    @Index(name = "idx_portfolio_items_instrument", columnList = "instrument_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "quantity", precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity;

    @Column(name = "purchase_price", precision = 18, scale = 6, nullable = false)
    private BigDecimal purchasePrice;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "notes", length = 500)
    private String notes;

    public BigDecimal getTotalCost() {
        return quantity.multiply(purchasePrice);
    }

    public BigDecimal getCurrentValue() {
        if (instrument.getCurrentPrice() == null) {
            return getTotalCost();
        }
        return quantity.multiply(instrument.getCurrentPrice());
    }

    public BigDecimal getProfitLoss() {
        return getCurrentValue().subtract(getTotalCost());
    }

    public BigDecimal getProfitLossPercent() {
        BigDecimal totalCost = getTotalCost();
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getProfitLoss()
            .divide(totalCost, 6, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}
