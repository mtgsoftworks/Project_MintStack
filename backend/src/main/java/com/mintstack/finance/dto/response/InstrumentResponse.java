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
public class InstrumentResponse {

    private UUID id;
    
    private String symbol;
    
    private String name;
    
    private Instrument.InstrumentType type;
    
    private String exchange;
    
    private String currency;
    
    private BigDecimal currentPrice;
    
    private BigDecimal previousClose;
    
    private BigDecimal change;
    
    private BigDecimal changePercent;

    private Long volume;

    private LocalDate maturityDate;
    
    private Boolean isActive;
}
