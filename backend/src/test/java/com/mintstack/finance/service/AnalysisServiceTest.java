package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.CompareInstrumentsRequest;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisService Tests")
class AnalysisServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private AnalysisService analysisService;

    private Instrument thyStock;
    private List<PriceHistory> priceHistoryList;

    @BeforeEach
    void setUp() {
        thyStock = Instrument.builder()
                .id(1L)
                .symbol("THYAO")
                .name("Türk Hava Yolları")
                .type(Instrument.InstrumentType.STOCK)
                .currentPrice(new BigDecimal("280.50"))
                .build();

        // Generate 100 days of price history for MA calculations
        priceHistoryList = new ArrayList<>();
        BigDecimal basePrice = new BigDecimal("250.00");
        
        for (int i = 99; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            // Simulating uptrend: price increases over time
            BigDecimal variance = new BigDecimal(Math.random() * 5 - 2.5);
            BigDecimal trendIncrease = new BigDecimal(i * 0.3);
            BigDecimal closePrice = basePrice.add(trendIncrease).add(variance);
            
            PriceHistory history = PriceHistory.builder()
                    .id((long) (100 - i))
                    .instrument(thyStock)
                    .priceDate(date)
                    .openPrice(closePrice.subtract(new BigDecimal("2")))
                    .highPrice(closePrice.add(new BigDecimal("3")))
                    .lowPrice(closePrice.subtract(new BigDecimal("3")))
                    .closePrice(closePrice)
                    .volume(1000000L + (long)(Math.random() * 500000))
                    .build();
            
            priceHistoryList.add(history);
        }
    }

    @Nested
    @DisplayName("Moving Average Tests")
    class MovingAverageTests {

        @Test
        @DisplayName("Should calculate simple moving average")
        void getMovingAverage_shouldCalculateMA() {
            // Given
            LocalDate endDate = LocalDate.now();
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(priceHistoryList);

            // When
            Map<String, Object> result = analysisService.getMovingAverage("THYAO", 7, endDate);

            // Then
            assertThat(result).containsKey("symbol");
            assertThat(result).containsKey("period");
            assertThat(result).containsKey("data");
            assertThat(result.get("symbol")).isEqualTo("THYAO");
            assertThat(result.get("period")).isEqualTo(7);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).isNotEmpty();
            assertThat(data.get(0)).containsKeys("date", "price", "ma");
        }

        @Test
        @DisplayName("Should throw exception when no price history")
        void getMovingAverage_whenNoHistory_shouldThrowException() {
            // Given
            LocalDate endDate = LocalDate.now();
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("INVALID"), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When/Then
            assertThatThrownBy(() -> analysisService.getMovingAverage("INVALID", 7, endDate))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should calculate multiple moving averages (MA7, MA25, MA99)")
        void getMultipleMovingAverages_shouldCalculateAllMAs() {
            // Given
            LocalDate endDate = LocalDate.now();
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(priceHistoryList);

            // When
            Map<String, Object> result = analysisService.getMultipleMovingAverages("THYAO", endDate);

            // Then
            assertThat(result).containsKey("symbol");
            assertThat(result).containsKey("data");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).isNotEmpty();
            assertThat(data.get(0)).containsKeys("date", "price", "ma7", "ma25", "ma99");
        }

        @Test
        @DisplayName("Should throw exception when insufficient data for MA99")
        void getMultipleMovingAverages_whenInsufficientData_shouldThrowException() {
            // Given
            LocalDate endDate = LocalDate.now();
            // Only 50 days of data, not enough for MA99
            List<PriceHistory> shortHistory = priceHistoryList.subList(0, 50);
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(shortHistory);

            // When/Then
            assertThatThrownBy(() -> analysisService.getMultipleMovingAverages("THYAO", endDate))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Trend Analysis Tests")
    class TrendAnalysisTests {

        @Test
        @DisplayName("Should detect uptrend")
        void getTrendAnalysis_shouldDetectUptrend() {
            // Given
            // Create uptrend data: prices consistently increase
            List<PriceHistory> uptrendHistory = new ArrayList<>();
            for (int i = 29; i >= 0; i--) {
                PriceHistory history = PriceHistory.builder()
                        .id((long) (30 - i))
                        .instrument(thyStock)
                        .priceDate(LocalDate.now().minusDays(i))
                        .closePrice(new BigDecimal(200 + (30 - i) * 3)) // Strong uptrend
                        .highPrice(new BigDecimal(205 + (30 - i) * 3))
                        .lowPrice(new BigDecimal(195 + (30 - i) * 3))
                        .build();
                uptrendHistory.add(history);
            }
            
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(uptrendHistory);

            // When
            Map<String, Object> result = analysisService.getTrendAnalysis("THYAO", 30);

            // Then
            assertThat(result.get("trend")).isEqualTo("UPTREND");
            assertThat(result.get("trendStrength")).isIn("MODERATE", "STRONG");
            assertThat(((BigDecimal) result.get("changePercent")).doubleValue()).isGreaterThan(5);
        }

        @Test
        @DisplayName("Should detect downtrend")
        void getTrendAnalysis_shouldDetectDowntrend() {
            // Given
            // Create downtrend data: prices consistently decrease
            List<PriceHistory> downtrendHistory = new ArrayList<>();
            for (int i = 29; i >= 0; i--) {
                PriceHistory history = PriceHistory.builder()
                        .id((long) (30 - i))
                        .instrument(thyStock)
                        .priceDate(LocalDate.now().minusDays(i))
                        .closePrice(new BigDecimal(300 - (30 - i) * 3)) // Strong downtrend
                        .highPrice(new BigDecimal(305 - (30 - i) * 3))
                        .lowPrice(new BigDecimal(295 - (30 - i) * 3))
                        .build();
                downtrendHistory.add(history);
            }
            
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(downtrendHistory);

            // When
            Map<String, Object> result = analysisService.getTrendAnalysis("THYAO", 30);

            // Then
            assertThat(result.get("trend")).isEqualTo("DOWNTREND");
            assertThat(((BigDecimal) result.get("changePercent")).doubleValue()).isLessThan(-5);
        }

        @Test
        @DisplayName("Should detect sideways trend")
        void getTrendAnalysis_shouldDetectSideways() {
            // Given
            // Create sideways data: prices stay relatively flat
            List<PriceHistory> sidewaysHistory = new ArrayList<>();
            for (int i = 29; i >= 0; i--) {
                BigDecimal variance = new BigDecimal(Math.random() * 4 - 2);
                PriceHistory history = PriceHistory.builder()
                        .id((long) (30 - i))
                        .instrument(thyStock)
                        .priceDate(LocalDate.now().minusDays(i))
                        .closePrice(new BigDecimal("250").add(variance)) // Nearly flat
                        .highPrice(new BigDecimal("253"))
                        .lowPrice(new BigDecimal("247"))
                        .build();
                sidewaysHistory.add(history);
            }
            
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(sidewaysHistory);

            // When
            Map<String, Object> result = analysisService.getTrendAnalysis("THYAO", 30);

            // Then
            assertThat(result.get("trend")).isEqualTo("SIDEWAYS");
        }

        @Test
        @DisplayName("Should calculate volatility")
        void getTrendAnalysis_shouldCalculateVolatility() {
            // Given
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(priceHistoryList.subList(0, 30));

            // When
            Map<String, Object> result = analysisService.getTrendAnalysis("THYAO", 30);

            // Then
            assertThat(result).containsKey("volatility");
            BigDecimal volatility = (BigDecimal) result.get("volatility");
            assertThat(volatility).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Instrument Comparison Tests")
    class InstrumentComparisonTests {

        @Test
        @DisplayName("Should compare multiple instruments")
        void compareInstruments_shouldCompare() {
            // Given
            CompareInstrumentsRequest request = new CompareInstrumentsRequest();
            request.setSymbols(Arrays.asList("THYAO", "GARAN"));
            request.setStartDate(LocalDate.now().minusDays(30));
            request.setEndDate(LocalDate.now());

            Instrument garan = Instrument.builder()
                    .id(2L)
                    .symbol("GARAN")
                    .name("Garanti Bankası")
                    .build();

            List<PriceHistory> garanHistory = new ArrayList<>();
            for (int i = 29; i >= 0; i--) {
                PriceHistory history = PriceHistory.builder()
                        .id((long) (30 - i + 100))
                        .instrument(garan)
                        .priceDate(LocalDate.now().minusDays(i))
                        .closePrice(new BigDecimal(45 + (30 - i) * 0.5))
                        .build();
                garanHistory.add(history);
            }

            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(priceHistoryList.subList(0, 30));
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("GARAN"), any(), any()))
                    .thenReturn(garanHistory);
            when(instrumentRepository.findBySymbol("THYAO"))
                    .thenReturn(Optional.of(thyStock));
            when(instrumentRepository.findBySymbol("GARAN"))
                    .thenReturn(Optional.of(garan));

            // When
            Map<String, Object> result = analysisService.compareInstruments(request);

            // Then
            assertThat(result).containsKeys("startDate", "endDate", "instruments");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> instruments = (List<Map<String, Object>>) result.get("instruments");
            assertThat(instruments).hasSize(2);
        }

        @Test
        @DisplayName("Should normalize prices as percentage change")
        void compareInstruments_shouldNormalizePrices() {
            // Given
            CompareInstrumentsRequest request = new CompareInstrumentsRequest();
            request.setSymbols(Arrays.asList("THYAO"));
            request.setStartDate(LocalDate.now().minusDays(30));
            request.setEndDate(LocalDate.now());

            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(priceHistoryList.subList(0, 30));
            when(instrumentRepository.findBySymbol("THYAO"))
                    .thenReturn(Optional.of(thyStock));

            // When
            Map<String, Object> result = analysisService.compareInstruments(request);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> instruments = (List<Map<String, Object>>) result.get("instruments");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) instruments.get(0).get("data");
            
            // First data point should be 0% (baseline)
            BigDecimal firstValue = (BigDecimal) data.get(0).get("value");
            assertThat(firstValue).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should skip instruments with no history")
        void compareInstruments_shouldSkipMissingData() {
            // Given
            CompareInstrumentsRequest request = new CompareInstrumentsRequest();
            request.setSymbols(Arrays.asList("THYAO", "INVALID"));
            request.setStartDate(LocalDate.now().minusDays(30));
            request.setEndDate(LocalDate.now());

            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(priceHistoryList.subList(0, 30));
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("INVALID"), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(instrumentRepository.findBySymbol("THYAO"))
                    .thenReturn(Optional.of(thyStock));

            // When
            Map<String, Object> result = analysisService.compareInstruments(request);

            // Then
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> instruments = (List<Map<String, Object>>) result.get("instruments");
            assertThat(instruments).hasSize(1); // Only THYAO, INVALID skipped
        }
    }
}
