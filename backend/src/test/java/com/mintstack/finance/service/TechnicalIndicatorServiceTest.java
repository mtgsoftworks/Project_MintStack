package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.BollingerBandsResult;
import com.mintstack.finance.dto.response.MACDResult;
import com.mintstack.finance.dto.response.StochasticResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TechnicalIndicatorService Tests")
class TechnicalIndicatorServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    private TechnicalIndicatorService technicalIndicatorService;
    private Instrument testInstrument;
    private UUID instrumentId;

    @BeforeEach
    void setUp() {
        technicalIndicatorService = new TechnicalIndicatorService(instrumentRepository, priceHistoryRepository);
        
        instrumentId = UUID.randomUUID();
        testInstrument = Instrument.builder()
                .symbol("THYAO")
                .name("Türk Hava Yolları")
                .type(Instrument.InstrumentType.STOCK)
                .build();
        testInstrument.setId(instrumentId);
    }

    @Test
    @DisplayName("RSI hesaplaması geçerli değer döndürmeli")
    void testCalculateRSI_WithValidData_ReturnsValidRSI() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        // 15 günlük fiyat verisi oluştur (14 period + 1)
        List<PriceHistory> priceHistory = createPriceHistory(20, 100.0, 0.02);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        Double rsi = technicalIndicatorService.calculateRSI("THYAO", 14);

        // Then
        assertThat(rsi).isNotNull();
        assertThat(rsi).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("RSI yetersiz veri ile sentetik seri uretmemeli")
    void testCalculateRSI_WithInsufficientData_ReturnsNull() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        // Sadece 5 günlük veri (14 için yetersiz)
        List<PriceHistory> priceHistory = createPriceHistory(5, 100.0, 0.02);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        Double rsi = technicalIndicatorService.calculateRSI("THYAO", 14);

        // Then
        assertThat(rsi).isNull();
    }

    @Test
    @DisplayName("MACD hesaplaması geçerli sonuç döndürmeli")
    void testCalculateMACD_ReturnsValidResult() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        List<PriceHistory> priceHistory = createPriceHistory(50, 100.0, 0.02);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        MACDResult macd = technicalIndicatorService.calculateMACD("THYAO");

        // Then
        assertThat(macd).isNotNull();
        assertThat(macd.fastPeriod()).isEqualTo(12);
        assertThat(macd.slowPeriod()).isEqualTo(26);
        assertThat(macd.signalPeriod()).isEqualTo(9);
    }

    @Test
    @DisplayName("Bollinger Bands hesaplaması geçerli bantlar döndürmeli")
    void testCalculateBollingerBands_ReturnsValidBands() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        List<PriceHistory> priceHistory = createPriceHistory(25, 100.0, 0.02);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        BollingerBandsResult bollinger = technicalIndicatorService.calculateBollingerBands("THYAO", 20, 2.0);

        // Then
        assertThat(bollinger).isNotNull();
        assertThat(bollinger.upperBand()).isGreaterThan(bollinger.middleBand());
        assertThat(bollinger.middleBand()).isGreaterThan(bollinger.lowerBand());
        assertThat(bollinger.period()).isEqualTo(20);
    }

    @Test
    @DisplayName("SMA hesaplaması doğru ortalama döndürmeli")
    void testCalculateSMA_WithValidPrices_ReturnsCorrectAverage() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        // Sabit fiyatlarla test
        List<PriceHistory> priceHistory = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PriceHistory ph = PriceHistory.builder()
                    .instrument(testInstrument)
                    .priceDate(LocalDate.now().minusDays(10 - i))
                    .closePrice(BigDecimal.valueOf(100)) // Tüm fiyatlar 100
                    .highPrice(BigDecimal.valueOf(105))
                    .lowPrice(BigDecimal.valueOf(95))
                    .build();
            priceHistory.add(ph);
        }
        
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        Double sma = technicalIndicatorService.calculateSMA("THYAO", 10);

        // Then
        assertThat(sma).isNotNull();
        assertThat(sma).isEqualTo(100.0); // Tüm fiyatlar aynı olduğu için SMA = 100
    }

    @Test
    @DisplayName("EMA hesaplaması geçerli değer döndürmeli")
    void testCalculateEMA_ReturnsValidValue() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        List<PriceHistory> priceHistory = createPriceHistory(50, 100.0, 0.02);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        Double ema = technicalIndicatorService.calculateEMA("THYAO", 20);

        // Then
        assertThat(ema).isNotNull();
        assertThat(ema).isGreaterThan(0);
    }

    @Test
    @DisplayName("Stochastic hesaplaması geçerli sinyal döndürmeli")
    void testCalculateStochastic_ReturnsValidSignal() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        List<PriceHistory> priceHistory = createPriceHistory(20, 100.0, 0.05);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        StochasticResult stochastic = technicalIndicatorService.calculateStochastic("THYAO", 14, 3);

        // Then
        assertThat(stochastic).isNotNull();
        assertThat(stochastic.percentK()).isBetween(0.0, 100.0);
        assertThat(stochastic.percentD()).isBetween(0.0, 100.0);
        assertThat(stochastic.signal()).isIn("OVERSOLD", "OVERBOUGHT", "BULLISH", "BEARISH", "NEUTRAL");
    }

    @Test
    @DisplayName("Olmayan enstrüman için null döndürmeli")
    void testCalculateIndicators_WithNonExistentSymbol_ReturnsNull() {
        // Given
        when(instrumentRepository.findBySymbol("INVALID")).thenReturn(Optional.empty());

        // When
        Double rsi = technicalIndicatorService.calculateRSI("INVALID", 14);
        MACDResult macd = technicalIndicatorService.calculateMACD("INVALID");
        Double sma = technicalIndicatorService.calculateSMA("INVALID", 20);

        // Then
        assertThat(rsi).isNull();
        assertThat(macd).isNull();
        assertThat(sma).isNull();
    }

    @Test
    @DisplayName("Tüm göstergeler tek seferde hesaplanabilmeli")
    void testCalculateAllIndicators_ReturnsCompleteResult() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));
        
        List<PriceHistory> priceHistory = createPriceHistory(250, 100.0, 0.02);
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                eq(instrumentId), any(), any())).thenReturn(priceHistory);

        // When
        var result = technicalIndicatorService.calculateAllIndicators("THYAO");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("THYAO");
        assertThat(result.overallSignal()).isIn("BULLISH", "BEARISH", "NEUTRAL");
    }

    // =================== HELPER METHODS ===================

    private List<PriceHistory> createPriceHistory(int days, double startPrice, double volatility) {
        List<PriceHistory> history = new ArrayList<>();
        Random random = new Random(42); // Tekrarlanabilirlik için seed
        double price = startPrice;

        for (int i = 0; i < days; i++) {
            double change = (random.nextGaussian() * volatility);
            price = price * (1 + change);
            
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
}
