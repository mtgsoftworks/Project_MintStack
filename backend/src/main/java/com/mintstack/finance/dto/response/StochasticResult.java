package com.mintstack.finance.dto.response;

/**
 * Stochastic Oscillator sonucu
 */
public record StochasticResult(
    double percentK,
    double percentD,
    int kPeriod,
    int dPeriod,
    String signal // OVERSOLD, OVERBOUGHT, BULLISH, BEARISH, NEUTRAL
) {}
