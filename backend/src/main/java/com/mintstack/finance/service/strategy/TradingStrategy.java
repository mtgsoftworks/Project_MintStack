package com.mintstack.finance.service.strategy;

import com.mintstack.finance.entity.PriceHistory;

/**
 * Trading stratejisi interface'i
 * Tüm backtesting stratejileri bu interface'i implement etmeli
 */
public interface TradingStrategy {
    
    /**
     * Strateji adını döndürür
     */
    String getName();
    
    /**
     * Strateji açıklamasını döndürür
     */
    String getDescription();
    
    /**
     * Mevcut fiyat verisine göre sinyal üretir
     * 
     * @param currentData Mevcut gün verileri
     * @param historicalData Geçmiş veriler (en yeni son sırada)
     * @param currentPosition Mevcut pozisyon (pozitif: long, negatif: short, 0: yok)
     * @return Trading sinyali
     */
    Signal generateSignal(PriceHistory currentData, 
                          java.util.List<PriceHistory> historicalData,
                          int currentPosition);
    
    /**
     * Strateji için gerekli minimum geçmiş veri sayısı
     */
    int getRequiredHistoryLength();
}
