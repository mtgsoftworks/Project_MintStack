package com.mintstack.finance.dto.response;

import com.mintstack.finance.service.strategy.Signal;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Backtest sırasında yapılan bir işlem kaydı
 */
public record TradeRecord(
    LocalDate date,
    Signal signal,           // BUY veya SELL
    BigDecimal price,
    int quantity,
    BigDecimal value         // Toplam değer (price * quantity)
) {}
