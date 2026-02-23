package com.mintstack.finance.service.simulation;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class SimulatedCrypto {

    private final String name;
    private final String symbol;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal high24h;
    private BigDecimal low24h;
    private BigDecimal marketCap;
    private Long volume24h;
    private final double baseVolatility;
    private BigDecimal changePercent24h;

    public SimulatedCrypto(String name, String symbol, double initialPrice, double baseVolatility) {
        this.name = name;
        this.symbol = symbol;
        this.currentPrice = BigDecimal.valueOf(initialPrice);
        this.previousClose = BigDecimal.valueOf(initialPrice);
        this.high24h = BigDecimal.valueOf(initialPrice);
        this.low24h = BigDecimal.valueOf(initialPrice);
        this.marketCap = BigDecimal.valueOf(initialPrice * 1_000_000_000L);
        this.volume24h = (long) (initialPrice * 10_000_000);
        this.baseVolatility = baseVolatility;
        this.changePercent24h = BigDecimal.ZERO;
    }

    public void updatePrice(BigDecimal newPrice) {
        this.currentPrice = newPrice;

        if (this.high24h == null || newPrice.compareTo(this.high24h) > 0) {
            this.high24h = newPrice;
        }

        if (this.low24h == null || newPrice.compareTo(this.low24h) < 0) {
            this.low24h = newPrice;
        }

        updateChangePercent();
        updateVolume();
    }

    public void setNewDayPreviousClose() {
        this.previousClose = this.currentPrice;
        this.high24h = this.currentPrice;
        this.low24h = this.currentPrice;
    }

    public BigDecimal getChangePercent() {
        return changePercent24h;
    }

    private void updateChangePercent() {
        if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
            this.changePercent24h = BigDecimal.ZERO;
        } else {
            this.changePercent24h = currentPrice.subtract(previousClose)
                .divide(previousClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
    }

    private void updateVolume() {
        double change = 0.7 + Math.random() * 0.6;
        this.volume24h = (long) (this.volume24h * change);
    }

    public void updateMarketCap(BigDecimal price) {
        long circulatingSupply = (long) (this.marketCap
            .divide(BigDecimal.valueOf(currentPrice.doubleValue()), 0, RoundingMode.HALF_UP)
            .doubleValue());
        this.marketCap = price.multiply(BigDecimal.valueOf(circulatingSupply));
    }
}
