package com.mintstack.finance.service.simulation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class SimulatedStock {

    private final String name;
    private final String exchange;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;
    private final double baseVolatility;
    private final String sector;

    private Double rsi;
    private Double macd;
    private Double bollingerUpper;
    private Double bollingerLower;

    private final Map<Integer, BigDecimal> bidLevels = new ConcurrentHashMap<>();
    private final Map<Integer, BigDecimal> askLevels = new ConcurrentHashMap<>();

    public SimulatedStock(String name, String exchange, double initialPrice, double baseVolatility) {
        this(name, exchange, initialPrice, baseVolatility, null);
    }

    public SimulatedStock(String name, String exchange, double initialPrice, double baseVolatility, String sector) {
        this.name = name;
        this.exchange = exchange;
        this.currentPrice = BigDecimal.valueOf(initialPrice);
        this.previousClose = BigDecimal.valueOf(initialPrice);
        this.openPrice = BigDecimal.valueOf(initialPrice);
        this.highPrice = BigDecimal.valueOf(initialPrice);
        this.lowPrice = BigDecimal.valueOf(initialPrice);
        this.baseVolatility = baseVolatility;
        this.sector = sector;
        this.volume = (long) (1000000 + Math.random() * 5000000);

        initializeOrderBook(initialPrice);
    }

    private void initializeOrderBook(double price) {
        for (int i = 1; i <= 5; i++) {
            double spread = price * 0.001 * i;
            bidLevels.put(i, BigDecimal.valueOf(price - spread));
            askLevels.put(i, BigDecimal.valueOf(price + spread));
        }
    }

    public void updateOrderBook(BigDecimal price) {
        double p = price.doubleValue();
        for (int i = 1; i <= 5; i++) {
            double spread = p * 0.001 * i;
            bidLevels.put(i, BigDecimal.valueOf(p - spread));
            askLevels.put(i, BigDecimal.valueOf(p + spread));
        }
    }

    public void updatePrice(BigDecimal newPrice) {
        this.currentPrice = newPrice;

        if (this.highPrice == null || newPrice.compareTo(this.highPrice) > 0) {
            this.highPrice = newPrice;
        }

        if (this.lowPrice == null || newPrice.compareTo(this.lowPrice) < 0) {
            this.lowPrice = newPrice;
        }

        updateOrderBook(newPrice);
    }

    public void setNewDayPreviousClose() {
        this.previousClose = this.currentPrice;
    }

    public void setPreviousClose(BigDecimal previousClose) {
        this.previousClose = previousClose;
    }

    public void resetDailyOHLC(BigDecimal price) {
        this.openPrice = price;
        this.highPrice = price;
        this.lowPrice = price;
    }

    public BigDecimal getChangePercent() {
        if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentPrice.subtract(previousClose)
            .divide(previousClose, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    public void updateVolume() {
        double change = 0.8 + Math.random() * 0.4;
        this.volume = (long) (this.volume * change);
    }

    @Deprecated
    public void calculateTechnicalIndicators() {
        log.warn("calculateTechnicalIndicators() deprecated - use TechnicalIndicatorService instead");
    }
}
