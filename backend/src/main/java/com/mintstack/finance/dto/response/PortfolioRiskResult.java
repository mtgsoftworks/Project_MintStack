package com.mintstack.finance.dto.response;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Portföy risk analizi sonucu
 */
public record PortfolioRiskResult(
    UUID portfolioId,
    String portfolioName,
    BigDecimal totalValue,
    BigDecimal portfolioVaR,
    double expectedReturn,        // Yüzde olarak
    double sharpeRatio,
    int days,
    Map<String, Double> assetContributions  // Her varlığın ağırlığı (%)
) {}
