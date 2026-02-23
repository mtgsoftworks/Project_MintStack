package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.MonteCarloResult;
import com.mintstack.finance.dto.response.VaRResult;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PortfolioRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonteCarloService Tests")
class MonteCarloServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    private Executor testExecutor;
    private MonteCarloService monteCarloService;
    private Instrument testInstrument;
    private UUID instrumentId;

    @BeforeEach
    void setUp() {
        testExecutor = Executors.newFixedThreadPool(2);
        monteCarloService = new MonteCarloService(
                instrumentRepository, priceHistoryRepository, portfolioRepository, testExecutor);
        
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
    @DisplayName("Monte Carlo simülasyonu geçerli sonuç döndürmeli")
    void testRunSimulation_ReturnsValidResult() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        List<PriceHistory> priceHistory = createPriceHistory(300, 280.0, 0.02);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        MonteCarloResult result = monteCarloService.runSimulation("THYAO", 30, 1000, 0.95);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("THYAO");
        assertThat(result.days()).isEqualTo(30);
        assertThat(result.simulations()).isEqualTo(1000);
        assertThat(result.p5().doubleValue()).isLessThan(result.p95().doubleValue());
        assertThat(result.histogram()).hasSize(20);
    }

    @Test
    @DisplayName("VaR hesaplaması geçerli sonuç döndürmeli")
    void testCalculateVaR_ReturnsValidResult() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        List<PriceHistory> priceHistory = createPriceHistory(300, 280.0, 0.02);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        VaRResult result = monteCarloService.calculateVaR("THYAO", 10, 0.95);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("THYAO");
        assertThat(result.days()).isEqualTo(10);
        assertThat(result.confidence()).isEqualTo(0.95);
        assertThat(result.varAmount().doubleValue()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Olmayan enstrüman için null döndürmeli")
    void testRunSimulation_WithNonExistentSymbol_ReturnsNull() {
        // Given
        when(instrumentRepository.findBySymbol("INVALID")).thenReturn(Optional.empty());

        // When
        MonteCarloResult result = monteCarloService.runSimulation("INVALID", 30, 1000, 0.95);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Histogram 20 bin içermeli")
    void testRunSimulation_HistogramHas20Bins() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(createPriceHistory(100, 280.0, 0.02));

        // When
        MonteCarloResult result = monteCarloService.runSimulation("THYAO", 10, 500, 0.95);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.histogram()).hasSize(20);
        
        // Histogram toplamı yaklaşık 100 olmalı
        double sum = Arrays.stream(result.histogram()).sum();
        assertThat(sum).isBetween(99.0, 101.0);
    }

    @Test
    @DisplayName("P5 < P50 < P95 sıralaması doğru olmalı")
    void testRunSimulation_PercentilesInCorrectOrder() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(createPriceHistory(300, 280.0, 0.03));

        // When
        MonteCarloResult result = monteCarloService.runSimulation("THYAO", 30, 5000, 0.95);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.p5().doubleValue()).isLessThan(result.p50().doubleValue());
        assertThat(result.p50().doubleValue()).isLessThan(result.p95().doubleValue());
    }

    // =================== HELPER METHODS ===================

    private List<PriceHistory> createPriceHistory(int days, double startPrice, double volatility) {
        List<PriceHistory> history = new ArrayList<>();
        Random random = new Random(42);
        double price = startPrice;

        for (int i = 0; i < days; i++) {
            double change = random.nextGaussian() * volatility;
            price = price * (1 + change);

            PriceHistory ph = PriceHistory.builder()
                    .instrument(testInstrument)
                    .priceDate(LocalDate.now().minusDays(days - i))
                    .closePrice(BigDecimal.valueOf(price))
                    .highPrice(BigDecimal.valueOf(price * 1.02))
                    .lowPrice(BigDecimal.valueOf(price * 0.98))
                    .volume(1000000L)
                    .build();
            ph.setId(UUID.randomUUID());
            history.add(ph);
        }

        return history;
    }
}
