package com.mintstack.finance.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String symbol;
    private String name;
    private BigDecimal price;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private Long volume;
    private BigDecimal changePercent;
    private String sector;
    private LocalDateTime timestamp;
    private BigDecimal previousClose;
}
