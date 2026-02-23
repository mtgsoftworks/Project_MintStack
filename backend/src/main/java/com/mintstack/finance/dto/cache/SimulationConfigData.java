package com.mintstack.finance.dto.cache;

import com.mintstack.finance.entity.SimulationConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationConfigData implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean isEnabled;
    private SimulationConfig.VolatilityLevel volatilityLevel;
    private Integer updateIntervalSeconds;
    private SimulationConfig.MarketTrend marketTrend;
    private Boolean enableRandomEvents;
    private Boolean enableMarketHours;

    public static SimulationConfigData fromEntity(SimulationConfig config) {
        return SimulationConfigData.builder()
                .isEnabled(config.getIsEnabled())
                .volatilityLevel(config.getVolatilityLevel())
                .updateIntervalSeconds(config.getUpdateIntervalSeconds())
                .marketTrend(config.getMarketTrend())
                .enableRandomEvents(config.getEnableRandomEvents())
                .enableMarketHours(config.getEnableMarketHours())
                .build();
    }

    public SimulationConfig toEntity() {
        SimulationConfig config = new SimulationConfig();
        config.setIsEnabled(this.isEnabled);
        config.setVolatilityLevel(this.volatilityLevel);
        config.setUpdateIntervalSeconds(this.updateIntervalSeconds);
        config.setMarketTrend(this.marketTrend);
        config.setEnableRandomEvents(this.enableRandomEvents);
        config.setEnableMarketHours(this.enableMarketHours);
        return config;
    }
}
