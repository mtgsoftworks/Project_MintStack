package com.mintstack.finance.dto.response;

/**
 * Tüm teknik göstergelerin birleşik sonucu
 */
public record TechnicalIndicatorsResult(
    String symbol,
    Double rsi,
    MACDResult macd,
    BollingerBandsResult bollingerBands,
    Double sma50,
    Double sma200,
    Double ema20,
    StochasticResult stochastic,
    String overallSignal // BULLISH, BEARISH, NEUTRAL
) {}
