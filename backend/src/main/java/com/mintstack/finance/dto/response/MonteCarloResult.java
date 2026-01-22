package com.mintstack.finance.dto.response;

import java.math.BigDecimal;

/**
 * Monte Carlo simülasyon sonucu
 */
public record MonteCarloResult(
    String symbol,
    int days,
    int simulations,
    BigDecimal currentPrice,
    BigDecimal meanForecast,
    BigDecimal p5,           // %5 percentile (kötü senaryo)
    BigDecimal p50,          // Median
    BigDecimal p95,          // %95 percentile (iyi senaryo)
    BigDecimal var,          // Value at Risk
    double confidence,
    double[] histogram       // Olasılık dağılımı
) {}
