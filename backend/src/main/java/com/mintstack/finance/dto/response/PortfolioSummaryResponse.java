package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryResponse {

    private BigDecimal totalValue;

    private BigDecimal totalCost;

    private BigDecimal totalProfitLoss;

    private BigDecimal totalProfitLossPercent;

    private Integer portfolioCount;
}
