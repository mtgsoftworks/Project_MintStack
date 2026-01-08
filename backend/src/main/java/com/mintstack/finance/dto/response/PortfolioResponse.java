package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {

    private UUID id;
    
    private String name;
    
    private String description;
    
    private Boolean isDefault;
    
    private BigDecimal totalValue;
    
    private BigDecimal totalCost;
    
    private BigDecimal profitLoss;
    
    private BigDecimal profitLossPercent;
    
    private int itemCount;
    
    private List<PortfolioItemResponse> items;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
