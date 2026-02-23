package com.mintstack.finance.service.simulation;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class SimulatedIndex {

    private final String name;
    private BigDecimal currentValue;
    private BigDecimal previousClose;
    private final double baseVolatility;

    public SimulatedIndex(String name, double initialValue, double baseVolatility) {
        this.name = name;
        this.currentValue = BigDecimal.valueOf(initialValue);
        this.previousClose = BigDecimal.valueOf(initialValue);
        this.baseVolatility = baseVolatility;
    }

    public void updateValue(BigDecimal newValue) {
        this.currentValue = newValue;
    }

    public void setNewDayPreviousClose() {
        this.previousClose = this.currentValue;
    }

    public void setPreviousClose(BigDecimal previousClose) {
        this.previousClose = previousClose;
    }

    public BigDecimal getChangePercent() {
        if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentValue.subtract(previousClose)
            .divide(previousClose, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
}
