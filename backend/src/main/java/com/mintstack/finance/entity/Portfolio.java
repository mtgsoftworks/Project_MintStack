package com.mintstack.finance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolios", indexes = {
    @Index(name = "idx_portfolios_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Portföy adı boş olamaz")
    @Size(max = 100, message = "Portföy adı en fazla 100 karakter olabilir")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "initial_cash_balance", precision = 18, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal initialCashBalance = new BigDecimal("100000.000000");

    @Column(name = "cash_balance", precision = 18, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal cashBalance = new BigDecimal("100000.000000");

    @Column(name = "commission_rate", precision = 8, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("0.001000");

    @Column(name = "minimum_commission_amount", precision = 18, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal minimumCommissionAmount = new BigDecimal("1.000000");

    @Column(name = "commission_tax_rate", precision = 8, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal commissionTaxRate = new BigDecimal("0.050000");

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PortfolioItem> items = new ArrayList<>();

    public BigDecimal getPositionValue() {
        return items.stream()
            .map(PortfolioItem::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalValue() {
        return getPositionValue().add(cashBalance != null ? cashBalance : BigDecimal.ZERO);
    }

    public BigDecimal getTotalCost() {
        return items.stream()
            .map(PortfolioItem::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getNetAssetValue() {
        return getPositionValue().add(cashBalance != null ? cashBalance : BigDecimal.ZERO);
    }

    public BigDecimal getTotalProfitLoss() {
        return getTotalValue().subtract(getTotalCost());
    }

    public BigDecimal getProfitLossPercent() {
        BigDecimal totalCost = getTotalCost();
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getTotalProfitLoss()
            .divide(totalCost, 6, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}
