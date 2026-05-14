package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.CompareInstrumentsRequest;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private AnalysisService analysisService;

    private Instrument testInstrument;
    private List<PriceHistory> priceHistoryList;

    @BeforeEach
    void setUp() {
        testInstrument = Instrument.builder()
            .symbol("THYAO")
            .name("Türk Hava Yolları")
            .type(Instrument.InstrumentType.STOCK)
            .exchange("BIST")
            .currentPrice(BigDecimal.valueOf(100))
            .isActive(true)
            .build();
        testInstrument.setId(UUID.randomUUID());

        LocalDate today = LocalDate.now();
        priceHistoryList = List.of(
            createPriceHistory(today.minusDays(2), BigDecimal.valueOf(95)),
            createPriceHistory(today.minusDays(1), BigDecimal.valueOf(98)),
            createPriceHistory(today, BigDecimal.valueOf(100))
        );
    }

    private PriceHistory createPriceHistory(LocalDate date, BigDecimal closePrice) {
        PriceHistory history = PriceHistory.builder()
            .instrument(testInstrument)
            .openPrice(closePrice.subtract(BigDecimal.ONE))
            .highPrice(closePrice.add(BigDecimal.ONE))
            .lowPrice(closePrice.subtract(BigDecimal.valueOf(2)))
            .closePrice(closePrice)
            .volume(1000000L)
            .priceDate(date)
            .build();
        history.setId(UUID.randomUUID());
        return history;
    }

    @Test
    void analysisService_ShouldBeInjected() {
        assertThat(analysisService).isNotNull();
    }

    @Test
    void instrumentRepository_ShouldFindBySymbol() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));

        // When
        Optional<Instrument> result = instrumentRepository.findBySymbol("THYAO");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("THYAO");
    }

    @Test
    void priceHistoryRepository_ShouldFindByInstrument() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
            eq(testInstrument.getId()), eq(startDate), eq(endDate)))
            .thenReturn(priceHistoryList);

        // When
        List<PriceHistory> result = priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
            testInstrument.getId(), startDate, endDate);

        // Then
        assertThat(result).hasSize(3);
    }

    @Test
    void getMovingAverage_ShouldCalculateSmaAndSignal() {
        LocalDate endDate = LocalDate.now();
        when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(LocalDate.class), eq(endDate)))
            .thenReturn(priceHistoryList);

        Map<String, Object> result = analysisService.getMovingAverage("THYAO", 3, endDate, "SMA");

        assertThat(result.get("symbol")).isEqualTo("THYAO");
        assertThat(result.get("type")).isEqualTo("SMA");
        assertThat(result.get("signal")).isEqualTo("BUY");
        assertThat((BigDecimal) result.get("maValue")).isEqualByComparingTo("97.666667");
    }

    @Test
    void getTrendAnalysis_ShouldReturnUptrend() {
        when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(priceHistoryList);

        Map<String, Object> result = analysisService.getTrendAnalysis("THYAO", 2);

        assertThat(result.get("symbol")).isEqualTo("THYAO");
        assertThat(result.get("trend")).isEqualTo("UPTREND");
        assertThat(result.get("trendStrength")).isEqualTo("MODERATE");
        assertThat(result.get("strength")).isEqualTo(55);
    }

    @Test
    @SuppressWarnings("unchecked")
    void compareInstruments_ShouldNormalizeSeries() {
        CompareInstrumentsRequest request = CompareInstrumentsRequest.builder()
            .symbols(List.of("THYAO"))
            .startDate(LocalDate.now().minusDays(2))
            .endDate(LocalDate.now())
            .build();

        when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(priceHistoryList);
        when(instrumentRepository.findBySymbolAndIsSimulated("THYAO", true)).thenReturn(Optional.of(testInstrument));

        Map<String, Object> result = analysisService.compareInstruments(request);

        List<Map<String, Object>> instruments = (List<Map<String, Object>>) result.get("instruments");
        assertThat(instruments).hasSize(1);
        assertThat(instruments.get(0).get("symbol")).isEqualTo("THYAO");

        List<Map<String, Object>> series = (List<Map<String, Object>>) instruments.get(0).get("data");
        assertThat(series).hasSize(3);
        assertThat((BigDecimal) series.get(0).get("value")).isEqualByComparingTo("0.000000");
    }
}
