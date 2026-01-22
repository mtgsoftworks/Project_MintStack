package com.mintstack.finance.service.strategy;

import com.mintstack.finance.entity.PriceHistory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RSI Stratejisi
 * RSI < 30 (Oversold) ise AL
 * RSI > 70 (Overbought) ise SAT
 */
@Component
public class RSIStrategy implements TradingStrategy {

    private final int period;
    private final double oversoldThreshold;
    private final double overboughtThreshold;

    public RSIStrategy() {
        this(14, 30.0, 70.0); // Varsayılan: 14 periyot, 30/70 eşikleri
    }

    public RSIStrategy(int period, double oversoldThreshold, double overboughtThreshold) {
        this.period = period;
        this.oversoldThreshold = oversoldThreshold;
        this.overboughtThreshold = overboughtThreshold;
    }

    @Override
    public String getName() {
        return "RSI_" + period;
    }

    @Override
    public String getDescription() {
        return String.format("RSI Strategy (Period: %d, Oversold: %.0f, Overbought: %.0f)", 
                period, oversoldThreshold, overboughtThreshold);
    }

    @Override
    public Signal generateSignal(PriceHistory currentData, 
                                  List<PriceHistory> historicalData,
                                  int currentPosition) {
        if (historicalData.size() < period + 1) {
            return Signal.HOLD;
        }

        double rsi = calculateRSI(historicalData);

        if (rsi < oversoldThreshold && currentPosition <= 0) {
            return Signal.BUY; // Oversold - Alım fırsatı
        } else if (rsi > overboughtThreshold && currentPosition > 0) {
            return Signal.SELL; // Overbought - Satış fırsatı
        }

        return Signal.HOLD;
    }

    @Override
    public int getRequiredHistoryLength() {
        return period + 10;
    }

    private double calculateRSI(List<PriceHistory> history) {
        if (history.size() < period + 1) {
            return 50.0; // Varsayılan nötr
        }

        double gains = 0;
        double losses = 0;

        int startIndex = history.size() - period - 1;
        for (int i = startIndex + 1; i < history.size(); i++) {
            double change = history.get(i).getClosePrice().doubleValue() 
                          - history.get(i - 1).getClosePrice().doubleValue();
            if (change > 0) {
                gains += change;
            } else {
                losses += Math.abs(change);
            }
        }

        double avgGain = gains / period;
        double avgLoss = losses / period;

        if (avgLoss == 0) {
            return 100.0;
        }

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}
