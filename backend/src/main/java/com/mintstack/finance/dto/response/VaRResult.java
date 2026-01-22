package com.mintstack.finance.dto.response;

import java.math.BigDecimal;

/**
 * VaR (Value at Risk) sonucu
 */
public record VaRResult(
    String symbol,
    int days,
    double confidence,
    BigDecimal currentValue,
    BigDecimal varAmount,
    double varPercent,
    String method
) {}
