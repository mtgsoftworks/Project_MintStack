package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "simulation_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationConfig extends BaseEntity {

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "volatility_level", nullable = false)
    @Builder.Default
    private VolatilityLevel volatilityLevel = VolatilityLevel.MEDIUM;

    @Column(name = "update_interval_seconds", nullable = false)
    @Builder.Default
    private Integer updateIntervalSeconds = 5;

    @Column(name = "market_trend")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MarketTrend marketTrend = MarketTrend.NEUTRAL;

    @Column(name = "enable_random_events")
    @Builder.Default
    private Boolean enableRandomEvents = true;

    @Column(name = "enable_market_hours")
    @Builder.Default
    private Boolean enableMarketHours = false;

    public enum VolatilityLevel {
        LOW(0.005),      // %0.5 günlük volatilite
        MEDIUM(0.015),   // %1.5 günlük volatilite
        HIGH(0.035),     // %3.5 günlük volatilite
        EXTREME(0.060);  // %6 günlük volatilite (kriz modu)

        private final double dailyVolatility;

        VolatilityLevel(double dailyVolatility) {
            this.dailyVolatility = dailyVolatility;
        }

        public double getDailyVolatility() {
            return dailyVolatility;
        }
    }

    public enum MarketTrend {
        BULLISH(0.0003),   // Pozitif drift
        NEUTRAL(0.0),      // Nötr
        BEARISH(-0.0003);  // Negatif drift

        private final double drift;

        MarketTrend(double drift) {
            this.drift = drift;
        }

        public double getDrift() {
            return drift;
        }
    }
}
