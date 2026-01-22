package com.mintstack.finance.service.strategy;

/**
 * Trading sinyali
 */
public enum Signal {
    BUY("Alım"),
    SELL("Satış"),
    HOLD("Bekle");
    
    private final String description;
    
    Signal(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
