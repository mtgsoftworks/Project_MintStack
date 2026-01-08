package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "price_history", indexes = {
    @Index(name = "idx_price_history_instrument", columnList = "instrument_id"),
    @Index(name = "idx_price_history_date", columnList = "price_date"),
    @Index(name = "idx_price_history_instrument_date", columnList = "instrument_id, price_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "open_price", precision = 18, scale = 6)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 18, scale = 6)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 18, scale = 6)
    private BigDecimal lowPrice;

    @Column(name = "close_price", precision = 18, scale = 6, nullable = false)
    private BigDecimal closePrice;

    @Column(name = "adj_close", precision = 18, scale = 6)
    private BigDecimal adjustedClose;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;
}
