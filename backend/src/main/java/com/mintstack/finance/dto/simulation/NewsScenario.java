package com.mintstack.finance.dto.simulation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsScenario {
    
    private String title;
    private String content;
    private String summary;
    private NewsType type;
    private String source;
    private List<String> affectedSectors;
    private List<String> affectedSymbols;
    private double impactPercent;
    private ImpactDirection direction;
    private int durationMinutes;
    private LocalDateTime timestamp;
    
    public enum NewsType {
        SECTOR_NEWS,
        COMPANY_NEWS,
        MACRO_NEWS,
        GEOPOLITICAL,
        CRYPTO_NEWS,
        EARNINGS,
        CENTRAL_BANK
    }
    
    public enum ImpactDirection {
        POSITIVE, NEGATIVE, NEUTRAL
    }
}
