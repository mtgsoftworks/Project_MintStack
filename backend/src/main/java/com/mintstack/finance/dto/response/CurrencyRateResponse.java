package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRateResponse {

    private UUID id;
    
    private String currencyCode;
    
    private String currencyName;
    
    private BigDecimal buyingRate;
    
    private BigDecimal sellingRate;
    
    private BigDecimal effectiveBuyingRate;
    
    private BigDecimal effectiveSellingRate;
    
    private BigDecimal averageRate;
    
    private BigDecimal changePercent;
    
    private String source;
    
    private LocalDateTime fetchedAt;
    
    private LocalDateTime rateDate;
}
