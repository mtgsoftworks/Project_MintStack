package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.CurrencyRateResponse;
import com.mintstack.finance.dto.response.InstrumentResponse;
import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.CurrencyRate.RateSource;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.NewsRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.repository.UserDataPreferenceRepository;
import com.mintstack.finance.service.market.InstrumentMetricsService;
import com.mintstack.finance.service.market.MarketDataMaintenanceService;
import com.mintstack.finance.service.external.YahooFinanceClient;
import com.mintstack.finance.service.simulation.SimulatedIndex;
import com.mintstack.finance.service.simulation.SimulatedStock;
import com.mintstack.finance.service.simulation.SimulationDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService Unit Tests")
class MarketDataServiceTest {

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private UserApiConfigRepository userApiConfigRepository;

    @Mock
    private UserDataPreferenceRepository userDataPreferenceRepository;

    @Mock
    private YahooFinanceClient yahooFinanceClient;

    @Mock
    private SimulationDataService simulationDataService;

    private MarketDataService marketDataService;

    private CurrencyRate usdRate;
    private CurrencyRate eurRate;
    private Instrument thyaoStock;

    @BeforeEach
    void setUp() {
        MarketDataMaintenanceService marketDataMaintenanceService = new MarketDataMaintenanceService(
            instrumentRepository,
            currencyRateRepository,
            priceHistoryRepository,
            newsRepository
        );
        InstrumentMetricsService instrumentMetricsService = new InstrumentMetricsService(
            priceHistoryRepository,
            yahooFinanceClient
        );
        marketDataService = new MarketDataService(
            instrumentRepository,
            currencyRateRepository,
            priceHistoryRepository,
            userApiConfigRepository,
            userDataPreferenceRepository,
            yahooFinanceClient,
            simulationDataService,
            marketDataMaintenanceService,
            instrumentMetricsService
        );

        // Setup test currency rates
        usdRate = CurrencyRate.builder()
                .currencyCode("USD")
                .currencyName("US Dollar")
                .buyingRate(new BigDecimal("32.50"))
                .sellingRate(new BigDecimal("32.70"))
                .effectiveBuyingRate(new BigDecimal("32.45"))
                .effectiveSellingRate(new BigDecimal("32.75"))
                .source(RateSource.TCMB)
                .fetchedAt(LocalDateTime.now())
                .rateDate(LocalDateTime.now())
                .build();
        usdRate.setId(UUID.randomUUID());

        eurRate = CurrencyRate.builder()
                .currencyCode("EUR")
                .currencyName("Euro")
                .buyingRate(new BigDecimal("35.20"))
                .sellingRate(new BigDecimal("35.50"))
                .source(RateSource.TCMB)
                .fetchedAt(LocalDateTime.now())
                .build();
        eurRate.setId(UUID.randomUUID());

        // Setup test instrument
        thyaoStock = Instrument.builder()
                .symbol("THYAO")
                .name("Türk Hava Yolları")
                .type(InstrumentType.STOCK)
                .exchange("BIST")
                .currency("TRY")
                .currentPrice(new BigDecimal("280.50"))
                .previousClose(new BigDecimal("275.00"))
                .isActive(true)
                .build();
        thyaoStock.setId(UUID.randomUUID());

        lenient().when(simulationDataService.isSimulationEnabled()).thenReturn(false);
        lenient().when(instrumentRepository.findBySymbol(anyString())).thenReturn(Optional.empty());
        lenient().when(userDataPreferenceRepository.findFirstByDataTypeAndIsEnabledTrueOrderByUpdatedAtDesc(any()))
            .thenReturn(Optional.empty());
        lenient().when(currencyRateRepository.findPreviousRatesByRateDate(anyString(), any(), any(), any(), any()))
            .thenReturn(List.of());
    }

    // ===================== CURRENCY RATE TESTS =====================

    @Test
    @DisplayName("getLatestCurrencyRates should return all latest rates from TCMB")
    void getLatestCurrencyRates_ShouldReturnAllRates() {
        when(currencyRateRepository.findLatestBySource(RateSource.TCMB))
                .thenReturn(Arrays.asList(usdRate, eurRate));

        List<CurrencyRateResponse> result = marketDataService.getLatestCurrencyRates();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCurrencyCode()).isEqualTo("USD");
        assertThat(result.get(0).getBuyingRate()).isEqualTo(new BigDecimal("32.50"));
        assertThat(result.get(1).getCurrencyCode()).isEqualTo("EUR");
        
        verify(currencyRateRepository, times(1)).findLatestBySource(RateSource.TCMB);
    }

    @Test
    @DisplayName("getCurrencyRate should return specific currency rate")
    void getCurrencyRate_ShouldReturnSpecificRate() {
        when(currencyRateRepository.findTopByCurrencyCodeAndSourceOrderByFetchedAtDesc("USD", RateSource.TCMB))
                .thenReturn(Optional.of(usdRate));

        CurrencyRateResponse result = marketDataService.getCurrencyRate("USD");

        assertThat(result).isNotNull();
        assertThat(result.getCurrencyCode()).isEqualTo("USD");
        assertThat(result.getCurrencyName()).isEqualTo("US Dollar");
        assertThat(result.getBuyingRate()).isEqualTo(new BigDecimal("32.50"));
    }

    @Test
    @DisplayName("getCurrencyRate should fallback missing effective rates to forex rates")
    void getCurrencyRate_ShouldFallbackMissingEffectiveRates() {
        eurRate.setEffectiveBuyingRate(BigDecimal.ZERO);
        eurRate.setEffectiveSellingRate(BigDecimal.ZERO);
        when(currencyRateRepository.findTopByCurrencyCodeAndSourceOrderByFetchedAtDesc("EUR", RateSource.TCMB))
                .thenReturn(Optional.of(eurRate));

        CurrencyRateResponse result = marketDataService.getCurrencyRate("EUR");

        assertThat(result.getEffectiveBuyingRate()).isEqualTo(new BigDecimal("35.20"));
        assertThat(result.getEffectiveSellingRate()).isEqualTo(new BigDecimal("35.50"));
    }

    @Test
    @DisplayName("getCurrencyRate should calculate change from previous distinct rate date")
    void getCurrencyRate_ShouldCalculateChangeFromPreviousRateDate() {
        LocalDateTime currentRateDate = LocalDateTime.of(2026, 5, 15, 0, 0);
        LocalDateTime previousRateDate = LocalDateTime.of(2026, 5, 14, 0, 0);
        usdRate.setRateDate(currentRateDate);
        usdRate.setSellingRate(new BigDecimal("33.00"));

        CurrencyRate previousRate = CurrencyRate.builder()
                .currencyCode("USD")
                .currencyName("US Dollar")
                .buyingRate(new BigDecimal("31.80"))
                .sellingRate(new BigDecimal("32.00"))
                .source(RateSource.TCMB)
                .fetchedAt(previousRateDate.plusHours(10))
                .rateDate(previousRateDate)
                .build();

        when(currencyRateRepository.findTopByCurrencyCodeAndSourceOrderByFetchedAtDesc("USD", RateSource.TCMB))
                .thenReturn(Optional.of(usdRate));
        when(currencyRateRepository.findPreviousRatesByRateDate(
                eq("USD"), eq(RateSource.TCMB), eq(currentRateDate), eq(BigDecimal.ZERO), any(Pageable.class)))
                .thenReturn(List.of(previousRate));

        CurrencyRateResponse result = marketDataService.getCurrencyRate("USD");

        assertThat(result.getChangePercent()).isEqualByComparingTo(new BigDecimal("3.125000"));
    }

    @Test
    @DisplayName("getCurrencyRate should throw exception when currency not found")
    void getCurrencyRate_ShouldThrowException_WhenNotFound() {
        when(currencyRateRepository.findTopByCurrencyCodeAndSourceOrderByFetchedAtDesc("XYZ", RateSource.TCMB))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketDataService.getCurrencyRate("XYZ"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getCurrencyHistory should return rates within date range")
    void getCurrencyHistory_ShouldReturnRatesInDateRange() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        when(currencyRateRepository.findHistoryByCurrencyCode(
                eq("USD"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(usdRate));

        List<CurrencyRateResponse> result = marketDataService.getCurrencyHistory("USD", startDate, endDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrencyCode()).isEqualTo("USD");
    }

    // ===================== INSTRUMENT TESTS =====================

    @Test
    @DisplayName("getInstrumentsByType should return instruments of given type")
    void getInstrumentsByType_ShouldReturnInstrumentsOfType() {
        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.STOCK, false))
                .thenReturn(Arrays.asList(thyaoStock));

        List<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.STOCK);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("THYAO");
        assertThat(result.get(0).getType()).isEqualTo(InstrumentType.STOCK);
    }

    @Test
    @DisplayName("getInstrumentsByType with pagination should return paginated results")
    void getInstrumentsByType_WithPagination_ShouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Instrument> instrumentPage = new PageImpl<>(Arrays.asList(thyaoStock), pageable, 1);
        
        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.STOCK, false, pageable))
                .thenReturn(instrumentPage);

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.STOCK, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getInstrumentsByType with pagination should derive stock change from price history")
    void getInstrumentsByType_WithPagination_ShouldDeriveStockChangeFromPriceHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        thyaoStock.setCurrentPrice(new BigDecimal("300.000000"));
        thyaoStock.setPreviousClose(new BigDecimal("300.000000"));
        PriceHistory latest = PriceHistory.builder()
                .instrument(thyaoStock)
                .priceDate(LocalDate.of(2026, 5, 15))
                .openPrice(new BigDecimal("299.000000"))
                .highPrice(new BigDecimal("305.000000"))
                .lowPrice(new BigDecimal("298.000000"))
                .closePrice(new BigDecimal("302.000000"))
                .volume(12_000_000L)
                .build();
        PriceHistory duplicateClose = PriceHistory.builder()
                .instrument(thyaoStock)
                .priceDate(LocalDate.of(2026, 5, 14))
                .closePrice(new BigDecimal("302.000000"))
                .volume(11_800_000L)
                .build();
        PriceHistory previous = PriceHistory.builder()
                .instrument(thyaoStock)
                .priceDate(LocalDate.of(2026, 5, 13))
                .closePrice(new BigDecimal("300.000000"))
                .volume(11_500_000L)
                .build();

        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.STOCK, false, pageable))
                .thenReturn(new PageImpl<>(List.of(thyaoStock), pageable, 1));
        when(priceHistoryRepository.findByInstrumentIdOrderByPriceDateDesc(eq(thyaoStock.getId()), any(Pageable.class)))
                .thenReturn(List.of(latest, duplicateClose, previous));
        when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(latest, duplicateClose, previous));

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.STOCK, pageable);

        InstrumentResponse response = result.getContent().get(0);
        assertThat(response.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("302.000000"));
        assertThat(response.getPreviousClose()).isEqualByComparingTo(new BigDecimal("300.000000"));
        assertThat(response.getChange()).isEqualByComparingTo(new BigDecimal("2.000000"));
        assertThat(response.getChangePercent()).isEqualByComparingTo(new BigDecimal("0.666700"));
        assertThat(response.getVolume()).isEqualTo(12_000_000L);
        assertThat(response.getTotalValue()).isEqualByComparingTo(new BigDecimal("3624000000.00"));
    }

    @Test
    @DisplayName("getInstrumentsByType with pagination should calculate change from requested date range")
    void getInstrumentsByType_WithPagination_ShouldCalculateChangeFromRequestedDateRange() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.now();
        thyaoStock.setCurrentPrice(new BigDecimal("120.000000"));
        thyaoStock.setPreviousClose(new BigDecimal("119.000000"));

        PriceHistory rangeStart = PriceHistory.builder()
                .instrument(thyaoStock)
                .priceDate(startDate)
                .closePrice(new BigDecimal("100.000000"))
                .build();

        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.STOCK, false, pageable))
                .thenReturn(new PageImpl<>(List.of(thyaoStock), pageable, 1));
        when(priceHistoryRepository.findByInstrumentIdOrderByPriceDateDesc(eq(thyaoStock.getId()), any(Pageable.class)))
                .thenReturn(List.of());
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                eq(thyaoStock.getId()), eq(startDate), any(Pageable.class)))
                .thenReturn(List.of(rangeStart));

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(
                InstrumentType.STOCK,
                pageable,
                startDate,
                endDate
        );

        InstrumentResponse response = result.getContent().get(0);
        assertThat(response.getChangeBasePrice()).isEqualByComparingTo(new BigDecimal("100.000000"));
        assertThat(response.getChange()).isEqualByComparingTo(new BigDecimal("20.000000"));
        assertThat(response.getChangePercent()).isEqualByComparingTo(new BigDecimal("20.000000"));
        assertThat(response.getChangeStartDate()).isEqualTo(startDate);
        assertThat(response.getChangeEndDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("getInstrumentsByType with 1D range should calculate change from today's open")
    void getInstrumentsByType_WithOneDayRange_ShouldCalculateChangeFromTodayOpen() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate today = LocalDate.now();
        thyaoStock.setCurrentPrice(new BigDecimal("120.000000"));
        thyaoStock.setPreviousClose(new BigDecimal("109.000000"));

        PriceHistory todayHistory = PriceHistory.builder()
                .instrument(thyaoStock)
                .priceDate(today)
                .openPrice(new BigDecimal("110.000000"))
                .closePrice(new BigDecimal("120.000000"))
                .build();

        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.STOCK, false, pageable))
                .thenReturn(new PageImpl<>(List.of(thyaoStock), pageable, 1));
        when(priceHistoryRepository.findByInstrumentIdOrderByPriceDateDesc(eq(thyaoStock.getId()), any(Pageable.class)))
                .thenReturn(List.of(todayHistory));
        when(priceHistoryRepository.findByInstrumentIdAndPriceDate(thyaoStock.getId(), today))
                .thenReturn(Optional.of(todayHistory));

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(
                InstrumentType.STOCK,
                pageable,
                today,
                today
        );

        InstrumentResponse response = result.getContent().get(0);
        assertThat(response.getChangeBasePrice()).isEqualByComparingTo(new BigDecimal("110.000000"));
        assertThat(response.getChange()).isEqualByComparingTo(new BigDecimal("10.000000"));
        assertThat(response.getChangePercent()).isEqualByComparingTo(new BigDecimal("9.090900"));
        assertThat(response.getChangeStartDate()).isEqualTo(today);
        assertThat(response.getChangeEndDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("getInstrumentsByType with 1D range should use latest session open when today's history is synthetic")
    void getInstrumentsByType_WithOneDayRange_ShouldUseLatestSessionOpenForSyntheticTodayHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate today = LocalDate.now();
        thyaoStock.setCurrentPrice(new BigDecimal("120.000000"));
        thyaoStock.setPreviousClose(new BigDecimal("120.000000"));

        PriceHistory todayHistory = PriceHistory.builder()
                .instrument(thyaoStock)
                .priceDate(today)
                .openPrice(new BigDecimal("120.000000"))
                .highPrice(new BigDecimal("120.000000"))
                .lowPrice(new BigDecimal("120.000000"))
                .closePrice(new BigDecimal("120.000000"))
                .adjustedClose(new BigDecimal("120.000000"))
                .build();
        PriceHistory previousHistory = PriceHistory.builder()
                .instrument(thyaoStock)
                .priceDate(today.minusDays(1))
                .openPrice(new BigDecimal("100.000000"))
                .highPrice(new BigDecimal("125.000000"))
                .lowPrice(new BigDecimal("99.000000"))
                .closePrice(new BigDecimal("110.000000"))
                .adjustedClose(new BigDecimal("110.000000"))
                .build();

        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.STOCK, false, pageable))
                .thenReturn(new PageImpl<>(List.of(thyaoStock), pageable, 1));
        when(priceHistoryRepository.findByInstrumentIdOrderByPriceDateDesc(eq(thyaoStock.getId()), any(Pageable.class)))
                .thenReturn(List.of(todayHistory, previousHistory));
        when(priceHistoryRepository.findByInstrumentIdAndPriceDate(thyaoStock.getId(), today))
                .thenReturn(Optional.of(todayHistory));
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                eq(thyaoStock.getId()), eq(today.minusDays(1)), any(Pageable.class)))
                .thenReturn(List.of(previousHistory));

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(
                InstrumentType.STOCK,
                pageable,
                today,
                today
        );

        InstrumentResponse response = result.getContent().get(0);
        assertThat(response.getChangeBasePrice()).isEqualByComparingTo(new BigDecimal("100.000000"));
        assertThat(response.getChange()).isEqualByComparingTo(new BigDecimal("20.000000"));
        assertThat(response.getChangePercent()).isEqualByComparingTo(new BigDecimal("20.000000"));
        assertThat(response.getChangeStartDate()).isEqualTo(today.minusDays(1));
        assertThat(response.getChangeEndDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("getInstrumentsByType with pagination should not fabricate stock change for sparse flat history")
    void getInstrumentsByType_WithPagination_ShouldNotFabricateStockChangeForSparseFlatHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        Instrument stock = Instrument.builder()
                .symbol("KOZAL")
                .name("Koza Altin")
                .type(InstrumentType.STOCK)
                .exchange("BIST")
                .currency("TRY")
                .currentPrice(new BigDecimal("34.950000"))
                .previousClose(new BigDecimal("34.950000"))
                .isActive(true)
                .build();
        stock.setId(UUID.randomUUID());
        PriceHistory latest = PriceHistory.builder()
                .instrument(stock)
                .priceDate(LocalDate.of(2026, 5, 16))
                .openPrice(new BigDecimal("34.950000"))
                .closePrice(new BigDecimal("34.950000"))
                .build();
        PriceHistory duplicate = PriceHistory.builder()
                .instrument(stock)
                .priceDate(LocalDate.of(2026, 5, 15))
                .openPrice(new BigDecimal("34.950000"))
                .closePrice(new BigDecimal("34.950000"))
                .build();

        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.STOCK, false, pageable))
                .thenReturn(new PageImpl<>(List.of(stock), pageable, 1));
        when(priceHistoryRepository.findByInstrumentIdOrderByPriceDateDesc(eq(stock.getId()), any(Pageable.class)))
                .thenReturn(List.of(latest, duplicate));

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.STOCK, pageable);

        InstrumentResponse response = result.getContent().get(0);
        assertThat(response.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("34.950000"));
        assertThat(response.getPreviousClose()).isEqualByComparingTo(new BigDecimal("34.950000"));
        assertThat(response.getChange()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getChangePercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getInstrumentsByType with pagination should not fabricate VIOP volume when history is empty")
    void getInstrumentsByType_WithPagination_ShouldNotFabricateViopVolume_WhenHistoryIsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        Instrument viop = Instrument.builder()
                .symbol("F_XU0300426")
                .name("BIST30 Nisan Vadeli")
                .type(InstrumentType.VIOP)
                .exchange("VIOP")
                .currency("TRY")
                .currentPrice(new BigDecimal("11120.00"))
                .previousClose(new BigDecimal("11065.00"))
                .isActive(true)
                .build();
        viop.setId(UUID.randomUUID());

        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.VIOP, false, pageable))
                .thenReturn(new PageImpl<>(List.of(viop), pageable, 1));
        when(priceHistoryRepository.findByInstrumentIdOrderByPriceDateDesc(eq(viop.getId()), any(Pageable.class)))
                .thenReturn(List.of());

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.VIOP, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getVolume()).isNull();
        assertThat(result.getContent().get(0).getTotalValue()).isNull();
    }

    @Test
    @DisplayName("getInstrumentsByType with pagination should derive derivatives change from price history")
    void getInstrumentsByType_WithPagination_ShouldDeriveViopChangeFromPriceHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        Instrument viop = Instrument.builder()
                .symbol("F_AKBNK0426")
                .name("AKBNK Nisan Vadeli")
                .type(InstrumentType.VIOP)
                .exchange("VIOP")
                .currency("TRY")
                .currentPrice(new BigDecimal("67.250000"))
                .previousClose(new BigDecimal("67.250000"))
                .isActive(true)
                .build();
        viop.setId(UUID.randomUUID());

        PriceHistory latest = PriceHistory.builder()
                .instrument(viop)
                .priceDate(LocalDate.of(2026, 5, 15))
                .openPrice(new BigDecimal("66.522877"))
                .highPrice(new BigDecimal("67.083526"))
                .lowPrice(new BigDecimal("66.123305"))
                .closePrice(new BigDecimal("66.687672"))
                .volume(102_969L)
                .build();
        PriceHistory previous = PriceHistory.builder()
                .instrument(viop)
                .priceDate(LocalDate.of(2026, 5, 14))
                .closePrice(new BigDecimal("66.955733"))
                .volume(102_832L)
                .build();

        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.VIOP, false, pageable))
                .thenReturn(new PageImpl<>(List.of(viop), pageable, 1));
        when(priceHistoryRepository.findByInstrumentIdOrderByPriceDateDesc(eq(viop.getId()), any(Pageable.class)))
                .thenReturn(List.of(latest, previous));

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.VIOP, pageable);

        InstrumentResponse response = result.getContent().get(0);
        assertThat(response.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("66.687672"));
        assertThat(response.getPreviousClose()).isEqualByComparingTo(new BigDecimal("66.955733"));
        assertThat(response.getChange()).isLessThan(BigDecimal.ZERO);
        assertThat(response.getChange()).isNotEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getChangePercent()).isLessThan(BigDecimal.ZERO);
        assertThat(response.getVolume()).isEqualTo(102_969L);
        assertThat(response.getTotalValue()).isEqualByComparingTo(new BigDecimal("6866762.90"));
    }

    @Test
    @DisplayName("getInstrumentsByType with pagination should fallback to simulation cache for stocks when DB is empty")
    void getInstrumentsByType_WithPagination_ShouldFallbackToSimulationCacheForStocks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Instrument> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        SimulatedStock simulatedStock = new SimulatedStock("Turk Hava Yollari", "BIST", 285.50, 0.025, "HAVACILIK");

        when(simulationDataService.isSimulationEnabled()).thenReturn(true);
        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.STOCK, true, pageable))
            .thenReturn(emptyPage);
        when(simulationDataService.getStocks()).thenReturn(Map.of("THYAO", simulatedStock));

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.STOCK, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getSymbol()).isEqualTo("THYAO");
    }

    @Test
    @DisplayName("getInstrument should return instrument by symbol")
    void getInstrument_ShouldReturnInstrumentBySymbol() {
        when(instrumentRepository.findBySymbolAndIsSimulated("THYAO", false))
                .thenReturn(Optional.of(thyaoStock));

        InstrumentResponse result = marketDataService.getInstrument("THYAO");

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("THYAO");
        assertThat(result.getName()).isEqualTo("Türk Hava Yolları");
    }

    @Test
    @DisplayName("getInstrument should throw exception when instrument not found")
    void getInstrument_ShouldThrowException_WhenNotFound() {
        when(instrumentRepository.findBySymbolAndIsSimulated("INVALID", false))
                .thenReturn(Optional.empty());
        when(instrumentRepository.findBySymbol("INVALID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketDataService.getInstrument("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getInstrument should fallback to simulation cache when simulation DB row does not exist")
    void getInstrument_ShouldFallbackToSimulationCache_WhenSimulationDbRowMissing() {
        SimulatedStock simulatedStock = new SimulatedStock("Turk Hava Yollari", "BIST", 285.50, 0.025, "HAVACILIK");

        when(simulationDataService.isSimulationEnabled()).thenReturn(true);
        when(instrumentRepository.findBySymbolAndIsSimulated("THYAO", true)).thenReturn(Optional.empty());
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.empty());
        when(simulationDataService.getStock("THYAO")).thenReturn(simulatedStock);

        InstrumentResponse result = marketDataService.getInstrument("THYAO");

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("THYAO");
        assertThat(result.getType()).isEqualTo(InstrumentType.STOCK);
        assertThat(result.getCurrentPrice()).isEqualByComparingTo("285.5");
    }

    @Test
    @DisplayName("searchInstruments should return matching instruments")
    void searchInstruments_ShouldReturnMatchingInstruments() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Instrument> instrumentPage = new PageImpl<>(Arrays.asList(thyaoStock), pageable, 1);
        
        when(instrumentRepository.searchBySymbolOrName("THY", pageable))
                .thenReturn(instrumentPage);

        Page<InstrumentResponse> result = marketDataService.searchInstruments("THY", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSymbol()).isEqualTo("THYAO");
    }

    @Test
    @DisplayName("searchInstruments should use simulated search only when simulation is enabled")
    void searchInstruments_ShouldUseSimulatedSearch_WhenSimulationEnabled() {
        Pageable pageable = PageRequest.of(0, 10);
        Instrument simulatedStock = Instrument.builder()
                .symbol("SIMTHY")
                .name("Simulated THY")
                .type(InstrumentType.STOCK)
                .exchange("BIST")
                .currency("TRY")
                .currentPrice(new BigDecimal("285.50"))
                .previousClose(new BigDecimal("280.00"))
                .isActive(true)
                .isSimulated(true)
                .build();
        Page<Instrument> instrumentPage = new PageImpl<>(List.of(simulatedStock), pageable, 1);

        when(simulationDataService.isSimulationEnabled()).thenReturn(true);
        when(instrumentRepository.searchBySymbolOrNameAndSimulationMode("SIM", true, pageable))
                .thenReturn(instrumentPage);

        Page<InstrumentResponse> result = marketDataService.searchInstruments("SIM", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSymbol()).isEqualTo("SIMTHY");
        assertThat(result.getContent().get(0).getIsSimulated()).isTrue();
    }

    @Test
    @DisplayName("searchInstruments by type should use real search when simulation is disabled")
    void searchInstrumentsByType_ShouldUseRealSearch_WhenSimulationDisabled() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Instrument> instrumentPage = new PageImpl<>(List.of(thyaoStock), pageable, 1);

        when(instrumentRepository.searchByTypeAndQuery(InstrumentType.STOCK, "THY", pageable))
                .thenReturn(instrumentPage);

        Page<InstrumentResponse> result = marketDataService.searchInstruments(InstrumentType.STOCK, "THY", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSymbol()).isEqualTo("THYAO");
        assertThat(result.getContent().get(0).getIsSimulated()).isFalse();
    }

    @Test
    @DisplayName("getInstrumentsByType should fallback to real data for unsupported simulation types")
    void getInstrumentsByType_ShouldFallbackToRealData_WhenSimulationDataEmpty() {
        Instrument bondInstrument = Instrument.builder()
                .symbol("TRT120128T11")
                .name("2Y Bond")
                .type(InstrumentType.BOND)
                .exchange("BIST")
                .currency("TRY")
                .currentPrice(new BigDecimal("98.45"))
                .previousClose(new BigDecimal("98.10"))
                .isActive(true)
                .isSimulated(false)
                .build();

        when(simulationDataService.isSimulationEnabled()).thenReturn(true);
        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.BOND, true))
                .thenReturn(List.of());
        when(instrumentRepository.findByTypeAndIsActiveTrue(InstrumentType.BOND))
                .thenReturn(List.of(bondInstrument));

        List<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.BOND);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("TRT120128T11");
        assertThat(result.get(0).getType()).isEqualTo(InstrumentType.BOND);
    }

    @Test
    @DisplayName("getInstrumentsByType should return simulated bonds from cache when simulation DB is empty")
    void getInstrumentsByType_ShouldReturnSimulatedBondsFromCache_WhenSimulationDbEmpty() {
        SimulatedStock simulatedBond = new SimulatedStock(
            "Devlet Tahvili 2027",
            "BIST",
            98.20,
            0.006,
            "TAHVIL"
        );

        when(simulationDataService.isSimulationEnabled()).thenReturn(true);
        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.BOND, true))
            .thenReturn(List.of());
        when(simulationDataService.getBonds()).thenReturn(Map.of("TRT150127T14", simulatedBond));

        List<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.BOND);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("TRT150127T14");
        assertThat(result.get(0).getType()).isEqualTo(InstrumentType.BOND);
    }

    @Test
    @DisplayName("getInstrumentsByType with pagination should fallback to real data for unsupported simulation types")
    void getInstrumentsByType_WithPagination_ShouldFallbackToRealData_WhenSimulationDataEmpty() {
        Instrument fundInstrument = Instrument.builder()
                .symbol("MAC")
                .name("Fund A")
                .type(InstrumentType.FUND)
                .exchange("TEFAS")
                .currency("TRY")
                .currentPrice(new BigDecimal("12.54"))
                .previousClose(new BigDecimal("12.42"))
                .isActive(true)
                .isSimulated(false)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<Instrument> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        Page<Instrument> realPage = new PageImpl<>(List.of(fundInstrument), pageable, 1);

        when(simulationDataService.isSimulationEnabled()).thenReturn(true);
        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(InstrumentType.FUND, true, pageable))
                .thenReturn(emptyPage);
        when(instrumentRepository.findByTypeAndIsActiveTrue(InstrumentType.FUND, pageable))
                .thenReturn(realPage);

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.FUND, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSymbol()).isEqualTo("MAC");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getMarketIndex should return simulated value when simulation mode is enabled")
    void getMarketIndex_ShouldReturnSimulationIndex_WhenSimulationEnabled() {
        SimulatedIndex simulatedIndex = new SimulatedIndex("BIST 100", 9850.0, 0.015);

        when(simulationDataService.isSimulationEnabled()).thenReturn(true);
        when(simulationDataService.getIndex("XU100.IS")).thenReturn(simulatedIndex);

        InstrumentResponse result = marketDataService.getMarketIndex("XU100.IS");

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("XU100.IS");
        assertThat(result.getType()).isEqualTo(InstrumentType.INDEX);
        assertThat(result.getCurrentPrice()).isEqualByComparingTo("9850.0");

        verify(yahooFinanceClient, never()).fetchStockPrice(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("getMarketIndex should resolve XU100.IS alias from XU100 simulation cache")
    void getMarketIndex_ShouldResolveAliasFromSimulationCache() {
        SimulatedIndex simulatedIndex = new SimulatedIndex("BIST 100", 9925.5, 0.015);

        when(simulationDataService.isSimulationEnabled()).thenReturn(true);
        when(simulationDataService.getIndex("XU100.IS")).thenReturn(null);
        when(simulationDataService.getIndex("XU100")).thenReturn(simulatedIndex);

        InstrumentResponse result = marketDataService.getMarketIndex("XU100.IS");

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("XU100.IS");
        assertThat(result.getCurrentPrice()).isEqualByComparingTo("9925.5");
        assertThat(result.getType()).isEqualTo(InstrumentType.INDEX);

        verify(yahooFinanceClient, never()).fetchStockPrice(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("getMarketIndex should fallback to simulated stock when index cache is empty")
    void getMarketIndex_ShouldFallbackToStock_WhenIndexMissing() {
        SimulatedStock stockFallback = new SimulatedStock("BIST 100", "BIST", 10012.3, 0.015);

        when(simulationDataService.isSimulationEnabled()).thenReturn(true);
        when(simulationDataService.getIndex("XU100.IS")).thenReturn(null);
        when(simulationDataService.getIndex("XU100")).thenReturn(null);
        when(simulationDataService.getStock("XU100")).thenReturn(stockFallback);

        InstrumentResponse result = marketDataService.getMarketIndex("XU100.IS");

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("XU100.IS");
        assertThat(result.getType()).isEqualTo(InstrumentType.INDEX);
        assertThat(result.getCurrentPrice()).isEqualByComparingTo("10012.3");

        verify(yahooFinanceClient, never()).fetchStockPrice(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("getMarketIndex should resolve XU100.IS alias from XU100 in real data")
    void getMarketIndex_ShouldResolveAliasFromRealData() {
        Instrument realIndex = Instrument.builder()
                .symbol("XU100")
                .name("BIST 100")
                .type(InstrumentType.INDEX)
                .exchange("BIST")
                .currency("TRY")
                .currentPrice(new BigDecimal("9875.40"))
                .previousClose(new BigDecimal("9800.10"))
                .isActive(true)
                .isSimulated(false)
                .build();

        when(simulationDataService.isSimulationEnabled()).thenReturn(false);
        when(instrumentRepository.findBySymbol("XU100.IS")).thenReturn(Optional.empty());
        when(instrumentRepository.findBySymbol("XU100")).thenReturn(Optional.of(realIndex));

        InstrumentResponse result = marketDataService.getMarketIndex("XU100.IS");

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("XU100");
        assertThat(result.getCurrentPrice()).isEqualByComparingTo("9875.40");
        assertThat(result.getType()).isEqualTo(InstrumentType.INDEX);

        verify(yahooFinanceClient, never()).fetchStockPrice(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("getMarketIndex should throw exception when no data source is available")
    void getMarketIndex_ShouldThrowException_WhenNoDataAvailable() {
        when(simulationDataService.isSimulationEnabled()).thenReturn(false);
        when(instrumentRepository.findBySymbol("XU100.IS")).thenReturn(Optional.empty());
        when(instrumentRepository.findBySymbol("XU100")).thenReturn(Optional.empty());
        when(userApiConfigRepository.findByProviderAndIsActiveTrue(ApiProvider.YAHOO_FINANCE)).thenReturn(List.of());
        when(yahooFinanceClient.fetchStockPrice(anyString(), any(), any()))
                .thenThrow(new RuntimeException("No market feed"));

        assertThatThrownBy(() -> marketDataService.getMarketIndex("XU100.IS"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getMarketIndex should fetch from Yahoo direct fallback when no API config exists")
    void getMarketIndex_ShouldFetchFromYahooDirectFallback_WhenNoConfig() {
        when(simulationDataService.isSimulationEnabled()).thenReturn(false);
        when(instrumentRepository.findBySymbol("XU100.IS")).thenReturn(Optional.empty());
        when(instrumentRepository.findBySymbol("XU100")).thenReturn(Optional.empty());
        when(userApiConfigRepository.findByProviderAndIsActiveTrue(ApiProvider.YAHOO_FINANCE)).thenReturn(List.of());
        when(yahooFinanceClient.fetchStockPrice(eq("XU100.IS"), isNull(), isNull()))
                .thenReturn(new BigDecimal("10123.45"));

        InstrumentResponse result = marketDataService.getMarketIndex("XU100.IS");

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("XU100.IS");
        assertThat(result.getCurrentPrice()).isEqualByComparingTo("10123.45");
        assertThat(result.getType()).isEqualTo(InstrumentType.INDEX);
    }

    // ===================== SAVE METHODS TESTS =====================

    @Test
    @DisplayName("saveCurrencyRates should save all rates")
    void saveCurrencyRates_ShouldSaveAllRates() {
        List<CurrencyRate> rates = Arrays.asList(usdRate, eurRate);
        when(currencyRateRepository.saveAll(rates)).thenReturn(rates);

        marketDataService.saveCurrencyRates(rates);

        verify(currencyRateRepository, times(1)).saveAll(rates);
    }

    @Test
    @DisplayName("updateInstrumentPrice should update instrument price")
    void updateInstrumentPrice_ShouldUpdatePrice() {
        BigDecimal newPrice = new BigDecimal("290.00");
        when(instrumentRepository.findBySymbol("THYAO"))
                .thenReturn(Optional.of(thyaoStock));
        when(instrumentRepository.save(any(Instrument.class)))
                .thenReturn(thyaoStock);

        marketDataService.updateInstrumentPrice("THYAO", newPrice);

        verify(instrumentRepository, times(1)).save(any(Instrument.class));
        assertThat(thyaoStock.getCurrentPrice()).isEqualTo(newPrice);
    }

    @Test
    @DisplayName("deleteAllMarketData should clear rates/history/news and deactivate real instruments")
    void deleteAllMarketData_ShouldClearDataAndDeactivateRealInstruments() {
        Instrument realIndex = Instrument.builder()
            .symbol("XU100.IS")
            .type(InstrumentType.INDEX)
            .isActive(true)
            .isSimulated(false)
            .build();
        Instrument realStock = Instrument.builder()
            .symbol("THYAO")
            .type(InstrumentType.STOCK)
            .isActive(true)
            .isSimulated(false)
            .build();
        Instrument simulatedStock = Instrument.builder()
            .symbol("SIM_STOCK")
            .type(InstrumentType.STOCK)
            .isActive(true)
            .isSimulated(true)
            .build();

        when(currencyRateRepository.count()).thenReturn(12L);
        when(priceHistoryRepository.count()).thenReturn(34L);
        when(newsRepository.count()).thenReturn(7L);
        when(instrumentRepository.findAll()).thenReturn(List.of(realIndex, realStock, simulatedStock));

        var result = marketDataService.deleteAllMarketData();

        assertThat(result.get("deletedCurrencyRates")).isEqualTo(12L);
        assertThat(result.get("deletedPriceHistory")).isEqualTo(34L);
        assertThat(result.get("deletedNews")).isEqualTo(7L);
        assertThat(result.get("deactivatedRealInstruments")).isEqualTo(2L);
        assertThat(result.get("deactivatedIndices")).isEqualTo(1L);
        assertThat(realIndex.getIsActive()).isFalse();
        assertThat(realStock.getIsActive()).isFalse();
        assertThat(simulatedStock.getIsActive()).isTrue();

        verify(currencyRateRepository, times(1)).deleteAll();
        verify(priceHistoryRepository, times(1)).deleteAll();
        verify(newsRepository, times(1)).deleteAllInBatch();
        verify(instrumentRepository, times(1)).saveAll(any(List.class));
    }

    @Test
    @DisplayName("Service should be properly injected")
    void service_ShouldBeInjected() {
        assertThat(marketDataService).isNotNull();
    }
}
