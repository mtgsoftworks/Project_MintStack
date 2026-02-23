package com.mintstack.finance.service.simulation;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class SimulatedCurrency {

    private final String name;
    private BigDecimal buyingRate;
    private BigDecimal sellingRate;
    private final BigDecimal meanRate;
    private final double baseVolatility;
    private final double spreadPercent;

    public SimulatedCurrency(String name, double buyingRate, double sellingRate, double baseVolatility) {
        this.name = name;
        this.buyingRate = BigDecimal.valueOf(buyingRate);
        this.sellingRate = BigDecimal.valueOf(sellingRate);
        this.meanRate = BigDecimal.valueOf((buyingRate + sellingRate) / 2);
        this.baseVolatility = baseVolatility;
        this.spreadPercent = (sellingRate - buyingRate) / ((buyingRate + sellingRate) / 2);
    }

    public void updateRates(BigDecimal buying, BigDecimal selling) {
        this.buyingRate = buying;
        this.sellingRate = selling;
    }

    public BigDecimal getMidRate() {
        return buyingRate.add(sellingRate).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
    }
}
