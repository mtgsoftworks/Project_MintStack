package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "instruments", indexes = {
    @Index(name = "idx_instruments_symbol", columnList = "symbol"),
    @Index(name = "idx_instruments_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrument extends BaseEntity {

    @Column(name = "symbol", unique = true, nullable = false, length = 20)
    private String symbol;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InstrumentType type;

    @Column(name = "exchange", length = 50)
    private String exchange;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "TRY";

    @Column(name = "current_price", precision = 18, scale = 6)
    private BigDecimal currentPrice;

    @Column(name = "previous_close", precision = 18, scale = 6)
    private BigDecimal previousClose;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PriceHistory> priceHistory = new ArrayList<>();

    public BigDecimal getChangePercent() {
        if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(previousClose)
            .divide(previousClose, 6, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    public enum InstrumentType {
        CURRENCY,
        STOCK,
        BOND,
        FUND,
        VIOP,
        COMMODITY
    }
}
