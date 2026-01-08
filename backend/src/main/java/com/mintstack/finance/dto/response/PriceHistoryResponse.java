package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistoryResponse {

    private LocalDate date;
    
    private BigDecimal open;
    
    private BigDecimal high;
    
    private BigDecimal low;
    
    private BigDecimal close;
    
    private BigDecimal adjustedClose;
    
    private Long volume;
}
