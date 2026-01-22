package com.mintstack.finance.dto.response;

/**
 * Bollinger Bands sonucu
 */
public record BollingerBandsResult(
    double upperBand,
    double middleBand,
    double lowerBand,
    double bandwidth,
    double percentB,
    int period,
    double stdDevMultiplier
) {}
