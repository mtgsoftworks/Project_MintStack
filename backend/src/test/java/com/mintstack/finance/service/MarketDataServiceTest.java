package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.CurrencyRateResponse;
import com.mintstack.finance.dto.response.InstrumentResponse;
import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.CurrencyRate.RateSource;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.service.external.YahooFinanceClient;
import com.mintstack.finance.service.simulation.SimulatedIndex;
import com.mintstack.finance.service.simulation.SimulatedStock;
import com.mintstack.finance.service.simulation.SimulationDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
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
    private UserApiConfigRepository userApiConfigRepository;

    @Mock
    private YahooFinanceClient yahooFinanceClient;

    @Mock
    private SimulationDataService simulationDataService;

    @InjectMocks
    private MarketDataService marketDataService;

    private CurrencyRate usdRate;
    private CurrencyRate eurRate;
    private Instrument thyaoStock;

    @BeforeEach
    void setUp() {
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
    @DisplayName("getMarketIndex should throw exception when no data source is available")
    void getMarketIndex_ShouldThrowException_WhenNoDataAvailable() {
        when(simulationDataService.isSimulationEnabled()).thenReturn(false);
        when(instrumentRepository.findBySymbol("XU100.IS")).thenReturn(Optional.empty());
        when(userApiConfigRepository.findByProviderAndIsActiveTrue(ApiProvider.YAHOO_FINANCE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> marketDataService.getMarketIndex("XU100.IS"))
                .isInstanceOf(ResourceNotFoundException.class);
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
    @DisplayName("Service should be properly injected")
    void service_ShouldBeInjected() {
        assertThat(marketDataService).isNotNull();
    }
}
