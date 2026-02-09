package com.mintstack.finance.service.strategy;

import com.mintstack.finance.entity.PriceHistory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Moving Average Crossover Stratejisi
 * Kısa vadeli SMA uzun vadeli SMA'yı yukarı keserse AL
 * Kısa vadeli SMA uzun vadeli SMA'yı aşağı keserse SAT
 */
@Component
public class MovingAverageCrossoverStrategy implements TradingStrategy {

    private final int shortPeriod;
    private final int longPeriod;

    public MovingAverageCrossoverStrategy() {
        this(50, 200); // Varsayılan: SMA 50/200
    }

    public MovingAverageCrossoverStrategy(int shortPeriod, int longPeriod) {
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
    }

    @Override
    public String getName() {
        return "MA_CROSSOVER_" + shortPeriod + "_" + longPeriod;
    }

    @Override
    public String getDescription() {
        return String.format("Moving Average Crossover (SMA %d/%d)", shortPeriod, longPeriod);
    }

    @Override
    public Signal generateSignal(PriceHistory currentData, 
                                  List<PriceHistory> historicalData,
                                  int currentPosition) {
        if (historicalData.size() < longPeriod) {
            return Signal.HOLD; // Yeterli veri yok
        }

        // Mevcut SMA değerlerini hesapla
        double currentShortSMA = calculateSMA(historicalData, shortPeriod);
        double currentLongSMA = calculateSMA(historicalData, longPeriod);

        // Önceki gün SMA değerlerini hesapla
        List<PriceHistory> previousHistory = historicalData.subList(0, historicalData.size() - 1);
        if (previousHistory.size() < longPeriod) {
            return Signal.HOLD;
        }
        
        double prevShortSMA = calculateSMA(previousHistory, shortPeriod);
        double prevLongSMA = calculateSMA(previousHistory, longPeriod);

        // Crossover kontrolü
        boolean goldenCross = prevShortSMA <= prevLongSMA && currentShortSMA > currentLongSMA;
        boolean deathCross = prevShortSMA >= prevLongSMA && currentShortSMA < currentLongSMA;

        if (goldenCross && currentPosition <= 0) {
            return Signal.BUY; // Golden Cross - Alım sinyali
        } else if (deathCross && currentPosition > 0) {
            return Signal.SELL; // Death Cross - Satım sinyali
        }

        return Signal.HOLD;
    }

    @Override
    public int getRequiredHistoryLength() {
        return longPeriod + 10; // Biraz buffer
    }

    private double calculateSMA(List<PriceHistory> history, int period) {
        if (history.size() < period) {
            return 0;
        }
        
        double sum = 0;
        int startIndex = history.size() - period;
        for (int i = startIndex; i < history.size(); i++) {
            sum += history.get(i).getClosePrice().doubleValue();
        }
        return sum / period;
    }
}
