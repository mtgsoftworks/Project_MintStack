package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Price alert entity for user notifications
 */
@Entity
@Table(name = "price_alerts", indexes = {
    @Index(name = "idx_alerts_user_id", columnList = "user_id"),
    @Index(name = "idx_alerts_instrument", columnList = "instrument_id"),
    @Index(name = "idx_alerts_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlert extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    @Column(name = "target_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal targetValue;

    @Column(name = "current_value_at_creation", precision = 18, scale = 4)
    private BigDecimal currentValueAtCreation;

    @Column(name = "is_triggered")
    @Builder.Default
    private Boolean isTriggered = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "triggered_value", precision = 18, scale = 4)
    private BigDecimal triggeredValue;

    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "notes")
    private String notes;

    /**
     * Alert types
     */
    public enum AlertType {
        PRICE_ABOVE,      // Trigger when price goes above target
        PRICE_BELOW,      // Trigger when price goes below target
        PERCENT_UP,       // Trigger when price increases by X%
        PERCENT_DOWN      // Trigger when price decreases by X%
    }

    /**
     * Check if alert should be triggered based on current price
     */
    public boolean shouldTrigger(BigDecimal currentPrice) {
        if (!isActive || isTriggered) {
            return false;
        }

        return switch (alertType) {
            case PRICE_ABOVE -> currentPrice.compareTo(targetValue) >= 0;
            case PRICE_BELOW -> currentPrice.compareTo(targetValue) <= 0;
            case PERCENT_UP -> {
                if (currentValueAtCreation == null || currentValueAtCreation.compareTo(BigDecimal.ZERO) == 0) {
                    yield false;
                }
                BigDecimal percentChange = currentPrice.subtract(currentValueAtCreation)
                        .divide(currentValueAtCreation, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                yield percentChange.compareTo(targetValue) >= 0;
            }
            case PERCENT_DOWN -> {
                if (currentValueAtCreation == null || currentValueAtCreation.compareTo(BigDecimal.ZERO) == 0) {
                    yield false;
                }
                BigDecimal percentChange = currentValueAtCreation.subtract(currentPrice)
                        .divide(currentValueAtCreation, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                yield percentChange.compareTo(targetValue) >= 0;
            }
        };
    }

    /**
     * Mark alert as triggered
     */
    public void trigger(BigDecimal currentPrice) {
        this.isTriggered = true;
        this.triggeredAt = LocalDateTime.now();
        this.triggeredValue = currentPrice;
        this.isActive = false;
    }
}
