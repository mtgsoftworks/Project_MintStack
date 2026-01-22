package com.mintstack.finance.service.simulation;

import com.mintstack.finance.entity.SimulationConfig.MarketTrend;
import com.mintstack.finance.entity.SimulationConfig.VolatilityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PriceSimulationEngine Tests")
class PriceSimulationEngineTest {

    private PriceSimulationEngine priceEngine;

    @BeforeEach
    void setUp() {
        priceEngine = new PriceSimulationEngine();
    }

    @Test
    @DisplayName("GBM simülasyonu geçerli fiyat döndürmeli")
    void testSimulateGBM_ReturnsValidPrice() {
        // Given
        String symbol = "THYAO";
        BigDecimal currentPrice = BigDecimal.valueOf(100.0);
        double baseVolatility = 0.02;
        int deltaTime = 5; // 5 saniye

        // When
        BigDecimal newPrice = priceEngine.simulateGBM(
                symbol, currentPrice, baseVolatility,
                VolatilityLevel.MEDIUM, MarketTrend.NEUTRAL, deltaTime);

        // Then
        assertThat(newPrice).isNotNull();
        assertThat(newPrice.doubleValue()).isGreaterThan(0);
        // Fiyat %10'dan fazla değişmemeli (sınır kontrolü)
        assertThat(newPrice.doubleValue()).isBetween(90.0, 110.0);
    }

    @Test
    @DisplayName("Yüksek volatilitede daha büyük hareketler olmalı")
    void testSimulateGBM_WithHighVolatility_ReturnsLargerMovement() {
        // Given
        String symbol = "TEST";
        BigDecimal currentPrice = BigDecimal.valueOf(100.0);
        double baseVolatility = 0.05;

        // 100 simülasyon yap ve volatilite farkını kontrol et
        double sumLowVol = 0;
        double sumHighVol = 0;
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            BigDecimal lowVolPrice = priceEngine.simulateGBM(
                    symbol + "_low_" + i, currentPrice, baseVolatility,
                    VolatilityLevel.LOW, MarketTrend.NEUTRAL, 5);
            
            BigDecimal highVolPrice = priceEngine.simulateGBM(
                    symbol + "_high_" + i, currentPrice, baseVolatility,
                    VolatilityLevel.HIGH, MarketTrend.NEUTRAL, 5);

            sumLowVol += Math.abs(lowVolPrice.doubleValue() - 100.0);
            sumHighVol += Math.abs(highVolPrice.doubleValue() - 100.0);
        }

        // Yüksek volatilite ortalama olarak daha büyük hareket üretmeli
        assertThat(sumHighVol / iterations).isGreaterThan(sumLowVol / iterations);
    }

    @Test
    @DisplayName("Bullish trend pozitif drift uygulamalı")
    void testSimulateGBM_WithBullishTrend_HasPositiveBias() {
        // Given
        String symbol = "TREND_TEST";
        BigDecimal currentPrice = BigDecimal.valueOf(100.0);
        int iterations = 500;
        
        double sumBullish = 0;
        double sumBearish = 0;

        // When
        for (int i = 0; i < iterations; i++) {
            BigDecimal bullishPrice = priceEngine.simulateGBM(
                    symbol + "_bull_" + i, currentPrice, 0.01,
                    VolatilityLevel.LOW, MarketTrend.BULLISH, 60);
            
            BigDecimal bearishPrice = priceEngine.simulateGBM(
                    symbol + "_bear_" + i, currentPrice, 0.01,
                    VolatilityLevel.LOW, MarketTrend.BEARISH, 60);

            sumBullish += bullishPrice.doubleValue();
            sumBearish += bearishPrice.doubleValue();
        }

        // Then - Bullish ortalama > Bearish ortalama
        assertThat(sumBullish / iterations).isGreaterThan(sumBearish / iterations);
    }

    @Test
    @DisplayName("Mean Reversion ortalamaya dönmeli")
    void testSimulateMeanReversion_ReturnsToMean() {
        // Given
        String symbol = "USD";
        BigDecimal currentPrice = BigDecimal.valueOf(38.50);
        BigDecimal meanPrice = BigDecimal.valueOf(38.00);
        double baseVolatility = 0.008;
        double reversionSpeed = 0.1;
        int iterations = 100;

        // Başlangıç fiyatı ortalamanın üstünde, zaman içinde düşmeli
        double sumDeviation = 0;
        BigDecimal price = currentPrice;

        // When
        for (int i = 0; i < iterations; i++) {
            price = priceEngine.simulateMeanReversion(
                    symbol, price, meanPrice, baseVolatility,
                    VolatilityLevel.MEDIUM, reversionSpeed, 5);
            sumDeviation += Math.abs(price.doubleValue() - meanPrice.doubleValue());
        }

        // Then - Fiyat sınırlar içinde kalmalı
        assertThat(price.doubleValue()).isBetween(
                meanPrice.doubleValue() * 0.85,
                meanPrice.doubleValue() * 1.15);
    }

    @Test
    @DisplayName("Spread simülasyonu geçerli bid-ask oluşturmalı")
    void testSimulateSpread_CreatesValidBidAsk() {
        // Given
        BigDecimal midPrice = BigDecimal.valueOf(38.50);
        double spreadPercent = 0.004; // %0.4 spread

        // When
        BigDecimal[] spread = priceEngine.simulateSpread(midPrice, spreadPercent);

        // Then
        assertThat(spread).hasSize(2);
        assertThat(spread[0]).isLessThan(spread[1]); // bid < ask
        assertThat(spread[0].doubleValue()).isLessThan(midPrice.doubleValue()); // bid < mid
        assertThat(spread[1].doubleValue()).isGreaterThan(midPrice.doubleValue()); // ask > mid
    }

    @Test
    @DisplayName("Rastgele olaylar kapalıyken 1.0 döndürmeli")
    void testMarketEvent_WithRandomEventsDisabled_ReturnsOne() {
        // When
        double result = priceEngine.simulateMarketEvent(false);

        // Then
        assertThat(result).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Açılış gap'i geçerli aralıkta olmalı")
    void testOpeningGap_ReturnsValidMultiplier() {
        // When
        double gap = priceEngine.simulateOpeningGap();

        // Then
        assertThat(gap).isBetween(0.995, 1.005); // -%0.5 ile +%0.5 arası
    }

    @Test
    @DisplayName("Gün içi volatilite U-şeklinde olmalı")
    void testIntradayVolatility_HasUShape() {
        // Given & When
        double openingVol = priceEngine.getIntradayVolatilityMultiplier(10, 0);  // Açılışta
        double middayVol = priceEngine.getIntradayVolatilityMultiplier(14, 0);   // Öğle
        double closingVol = priceEngine.getIntradayVolatilityMultiplier(17, 30); // Kapanışa yakın

        // Then - U-şekli: açılış ve kapanış > öğle
        assertThat(openingVol).isGreaterThan(middayVol);
        assertThat(closingVol).isGreaterThan(middayVol);
    }

    @Test
    @DisplayName("Son fiyat kaydedilmeli ve alınabilmeli")
    void testLastPrice_IsStoredAndRetrievable() {
        // Given
        String symbol = "TEST_SYMBOL";
        BigDecimal price = BigDecimal.valueOf(150.50);

        // When
        priceEngine.setLastPrice(symbol, price);
        BigDecimal retrievedPrice = priceEngine.getLastPrice(symbol);

        // Then
        assertThat(retrievedPrice).isEqualTo(price);
    }

    @Test
    @DisplayName("clearState tüm verileri temizlemeli")
    void testClearState_ClearsAllData() {
        // Given
        priceEngine.setLastPrice("SYMBOL1", BigDecimal.valueOf(100));
        priceEngine.setLastPrice("SYMBOL2", BigDecimal.valueOf(200));

        // When
        priceEngine.clearState();

        // Then
        assertThat(priceEngine.getLastPrice("SYMBOL1")).isNull();
        assertThat(priceEngine.getLastPrice("SYMBOL2")).isNull();
    }
}
