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
public class CurrencyRateData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private String name;
    private BigDecimal buyingRate;
    private BigDecimal sellingRate;
    private BigDecimal midRate;
    private LocalDateTime timestamp;
}
