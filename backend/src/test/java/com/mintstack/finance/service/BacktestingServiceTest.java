package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.BacktestResult;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BacktestingService Tests")
class BacktestingServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    private BacktestingService backtestingService;
    private Instrument testInstrument;
    private UUID instrumentId;

    @BeforeEach
    void setUp() {
        backtestingService = new BacktestingService(instrumentRepository, priceHistoryRepository);
        
        instrumentId = UUID.randomUUID();
        testInstrument = Instrument.builder()
                .symbol("THYAO")
                .name("Türk Hava Yolları")
                .type(Instrument.InstrumentType.STOCK)
                .currentPrice(BigDecimal.valueOf(285.50))
                .build();
        testInstrument.setId(instrumentId);
    }

    @Test
    @DisplayName("MA Crossover stratejisi ile backtest çalışmalı")
    void testRunBacktest_WithMACrossover_ReturnsResult() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        // Trend oluşturan fiyat verisi (önce yükseliş, sonra düşüş)
        List<PriceHistory> priceHistory = createTrendingPriceHistory(400, 100.0);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        BacktestResult result = backtestingService.runBacktest(
                "MA_CROSSOVER",
                "THYAO",
                LocalDate.now().minusYears(1),
                LocalDate.now(),
                BigDecimal.valueOf(10000)
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("THYAO");
        assertThat(result.initialCapital()).isEqualTo(BigDecimal.valueOf(10000));
        assertThat(result.strategyName()).contains("MA_CROSSOVER");
    }

    @Test
    @DisplayName("RSI stratejisi ile backtest çalışmalı")
    void testRunBacktest_WithRSI_ReturnsResult() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        List<PriceHistory> priceHistory = createVolatilePriceHistory(100, 100.0);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        BacktestResult result = backtestingService.runBacktest(
                "RSI",
                "THYAO",
                LocalDate.now().minusMonths(3),
                LocalDate.now(),
                BigDecimal.valueOf(5000)
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.strategyName()).contains("RSI");
    }

    @Test
    @DisplayName("Mevcut stratejiler listesi boş olmamalı")
    void testGetAvailableStrategies_ReturnsNonEmptyList() {
        // When
        var strategies = backtestingService.getAvailableStrategies();

        // Then
        assertThat(strategies).isNotEmpty();
        assertThat(strategies.size()).isGreaterThanOrEqualTo(4);
        
        // MA_CROSSOVER ve RSI olmalı
        assertThat(strategies.stream().anyMatch(s -> s.key().contains("MA_CROSSOVER"))).isTrue();
        assertThat(strategies.stream().anyMatch(s -> s.key().contains("RSI"))).isTrue();
    }

    @Test
    @DisplayName("Geçersiz strateji için null döndürmeli")
    void testRunBacktest_WithInvalidStrategy_ReturnsNull() {
        // When
        BacktestResult result = backtestingService.runBacktest(
                "INVALID_STRATEGY",
                "THYAO",
                LocalDate.now().minusYears(1),
                LocalDate.now(),
                BigDecimal.valueOf(10000)
        );

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Olmayan enstrüman için null döndürmeli")
    void testRunBacktest_WithNonExistentSymbol_ReturnsNull() {
        // Given
        when(instrumentRepository.findBySymbol("INVALID")).thenReturn(Optional.empty());

        // When
        BacktestResult result = backtestingService.runBacktest(
                "MA_CROSSOVER",
                "INVALID",
                LocalDate.now().minusYears(1),
                LocalDate.now(),
                BigDecimal.valueOf(10000)
        );

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Backtest metrikleri geçerli olmalı")
    void testRunBacktest_MetricsAreValid() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        List<PriceHistory> priceHistory = createTrendingPriceHistory(300, 100.0);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        BacktestResult result = backtestingService.runBacktest(
                "MA_CROSSOVER_20_50",
                "THYAO",
                LocalDate.now().minusMonths(6),
                LocalDate.now(),
                BigDecimal.valueOf(10000)
        );

        // Then
        if (result != null) {
            assertThat(result.winRatePercent()).isBetween(0.0, 100.0);
            assertThat(result.maxDrawdownPercent()).isGreaterThanOrEqualTo(0);
            assertThat(result.finalCapital().doubleValue()).isGreaterThan(0);
        }
    }

    // =================== HELPER METHODS ===================

    private List<PriceHistory> createTrendingPriceHistory(int days, double startPrice) {
        List<PriceHistory> history = new ArrayList<>();
        double price = startPrice;

        for (int i = 0; i < days; i++) {
            // Trend: ilk yarı yükseliş, ikinci yarı düşüş
            double trend = i < days / 2 ? 0.002 : -0.001;
            double noise = (Math.random() - 0.5) * 0.02;
            price = price * (1 + trend + noise);
            price = Math.max(price, 1.0);

            PriceHistory ph = PriceHistory.builder()
                    .instrument(testInstrument)
                    .priceDate(LocalDate.now().minusDays(days - i))
                    .openPrice(BigDecimal.valueOf(price * 0.99))
                    .highPrice(BigDecimal.valueOf(price * 1.02))
                    .lowPrice(BigDecimal.valueOf(price * 0.98))
                    .closePrice(BigDecimal.valueOf(price))
                    .volume(1000000L)
                    .build();
            ph.setId(UUID.randomUUID());
            history.add(ph);
        }

        return history;
    }

    private List<PriceHistory> createVolatilePriceHistory(int days, double startPrice) {
        List<PriceHistory> history = new ArrayList<>();
        Random random = new Random(123);
        double price = startPrice;

        for (int i = 0; i < days; i++) {
            double change = random.nextGaussian() * 0.03; // %3 volatilite
            price = price * (1 + change);
            price = Math.max(price, 1.0);

            PriceHistory ph = PriceHistory.builder()
                    .instrument(testInstrument)
                    .priceDate(LocalDate.now().minusDays(days - i))
                    .openPrice(BigDecimal.valueOf(price * 0.99))
                    .highPrice(BigDecimal.valueOf(price * 1.03))
                    .lowPrice(BigDecimal.valueOf(price * 0.97))
                    .closePrice(BigDecimal.valueOf(price))
                    .volume(1000000L)
                    .build();
            ph.setId(UUID.randomUUID());
            history.add(ph);
        }

        return history;
    }
}
