package com.mintstack.finance.dto.simulation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketEvent {
    
    public enum EventType {
        CIRCUIT_BREAKER,
        SHORT_SQUEEZE,
        WHALE_ACTIVITY,
        SECTOR_ROTATION,
        VOLATILITY_SPIKE,
        LIQUIDITY_CRISIS,
        HALT,
        RALLY,
        FLASH_CRASH,
        GOLDEN_CROSS,
        DEATH_CROSS
    }
    
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    private String id;
    private EventType type;
    private String description;
    @Builder.Default
    private List<String> affectedSymbols = new ArrayList<>();
    @Builder.Default
    private List<String> affectedSectors = new ArrayList<>();
    @Builder.Default
    private double priceMultiplier = 1.0;
    @Builder.Default
    private double volatilityMultiplier = 1.0;
    private int remainingDurationTicks;
    private int totalDurationTicks;
    private LocalDateTime startTime;
    @Builder.Default
    private boolean isGlobal = false;
    private Severity severity;
    @Builder.Default
    private boolean tradingHalted = false;
    private int haltDurationMinutes;
    
    public boolean isActive() {
        return remainingDurationTicks > 0;
    }
    
    public void tick() {
        if (remainingDurationTicks > 0) {
            remainingDurationTicks--;
        }
    }
    
    public double getProgressPercent() {
        if (totalDurationTicks == 0) return 100.0;
        return ((totalDurationTicks - remainingDurationTicks) * 100.0) / totalDurationTicks;
    }
}
