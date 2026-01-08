package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "currency_rates", indexes = {
    @Index(name = "idx_currency_rates_code", columnList = "currency_code"),
    @Index(name = "idx_currency_rates_source", columnList = "source"),
    @Index(name = "idx_currency_rates_fetched_at", columnList = "fetched_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyRate extends BaseEntity {

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "currency_name")
    private String currencyName;

    @Column(name = "buying_rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal buyingRate;

    @Column(name = "selling_rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal sellingRate;

    @Column(name = "effective_buying_rate", precision = 18, scale = 6)
    private BigDecimal effectiveBuyingRate;

    @Column(name = "effective_selling_rate", precision = 18, scale = 6)
    private BigDecimal effectiveSellingRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private RateSource source;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "rate_date")
    private LocalDateTime rateDate;

    public BigDecimal getAverageRate() {
        return buyingRate.add(sellingRate).divide(new BigDecimal("2"), 6, java.math.RoundingMode.HALF_UP);
    }

    public enum RateSource {
        TCMB,
        YAHOO_FINANCE,
        ALPHA_VANTAGE,
        BANK_API,
        MANUAL
    }
}
