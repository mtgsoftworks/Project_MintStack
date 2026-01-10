package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for WebSocket price update messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdateMessage {

    /**
     * Type of the update: CURRENCY, STOCK, BOND, FUND, VIOP
     */
    private String type;

    /**
     * Symbol or code of the instrument
     */
    private String symbol;

    /**
     * Current price
     */
    private BigDecimal price;

    /**
     * Previous price (for calculating change)
     */
    private BigDecimal previousPrice;

    /**
     * Additional data specific to the instrument type
     */
    private Map<String, Object> additionalData;

    /**
     * Timestamp of the update
     */
    private LocalDateTime timestamp;

    /**
     * Calculate price change
     */
    public BigDecimal getChange() {
        if (price != null && previousPrice != null) {
            return price.subtract(previousPrice);
        }
        return null;
    }

    /**
     * Calculate percentage change
     */
    public BigDecimal getChangePercent() {
        if (price != null && previousPrice != null && previousPrice.compareTo(BigDecimal.ZERO) != 0) {
            return price.subtract(previousPrice)
                    .divide(previousPrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return null;
    }
}
