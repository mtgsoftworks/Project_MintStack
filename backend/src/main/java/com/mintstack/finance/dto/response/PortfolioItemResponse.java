package com.mintstack.finance.dto.response;

import com.mintstack.finance.entity.Instrument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioItemResponse {

    private UUID id;
    
    private UUID instrumentId;
    
    private String instrumentSymbol;
    
    private String instrumentName;
    
    private Instrument.InstrumentType instrumentType;
    
    private BigDecimal quantity;
    
    private BigDecimal purchasePrice;
    
    private BigDecimal currentPrice;
    
    private BigDecimal totalCost;
    
    private BigDecimal currentValue;
    
    private BigDecimal profitLoss;
    
    private BigDecimal profitLossPercent;
    
    private LocalDate purchaseDate;
    
    private String notes;
}
