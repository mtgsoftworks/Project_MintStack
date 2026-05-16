package com.mintstack.finance.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationMetrics {
    
    private long tickCount;
    
    @JsonSerialize(using = DurationSerializer.class)
    private Duration uptime;
    
    private int stocks;
    private int bonds;
    private int funds;
    private int viop;
    private int currencies;
    private int indices;
    private int cryptos;
    private int activeEvents;
    
    private Map<String, Long> volatilityStats;
    
    private double cacheHitRatio;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdateTime;
    
    private double avgTickDurationMs;
    private long totalEventsGenerated;
    private long totalNewsGenerated;
}
