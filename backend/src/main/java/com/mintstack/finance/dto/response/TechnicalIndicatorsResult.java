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
    Double atr14,
    Double adx14,
    Long obv,
    Double vwap20,
    Double cci20,
    Double mfi14,
    Double williamsR14,
    String dataQuality,
    String overallSignal // BULLISH, BEARISH, NEUTRAL
) {}
