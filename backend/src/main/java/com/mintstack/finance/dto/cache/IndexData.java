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
public class IndexData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String symbol;
    private String name;
    private BigDecimal value;
    private BigDecimal changePercent;
    private BigDecimal previousClose;
    private LocalDateTime timestamp;
}
