package com.mintstack.finance.dto.response;

/**
 * MACD (Moving Average Convergence Divergence) sonucu
 */
public record MACDResult(
    double macdLine,
    double signalLine,
    double histogram,
    int fastPeriod,
    int slowPeriod,
    int signalPeriod
) {}
