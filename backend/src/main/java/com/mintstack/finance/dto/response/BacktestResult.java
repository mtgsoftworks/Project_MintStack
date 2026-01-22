package com.mintstack.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Backtest sonucu
 */
public record BacktestResult(
    String strategyName,
    String strategyDescription,
    String symbol,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal initialCapital,
    BigDecimal finalCapital,
    double totalReturnPercent,    // Toplam getiri (%)
    double sharpeRatio,           // Risk-adjusted return
    double maxDrawdownPercent,    // Maksimum düşüş (%)
    int totalTrades,              // Toplam işlem sayısı
    int winningTrades,
    int losingTrades,
    double winRatePercent,        // Kazanma oranı (%)
    double profitFactor,          // Kar/Zarar oranı
    List<TradeRecord> trades      // İşlem geçmişi
) {}
