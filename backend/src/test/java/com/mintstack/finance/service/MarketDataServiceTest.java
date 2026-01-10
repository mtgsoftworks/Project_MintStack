package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.CurrencyRateResponse;
import com.mintstack.finance.dto.response.InstrumentResponse;
import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.CurrencyRate.RateSource;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        when(currencyRateRepository.findTopByCurrencyCodeOrderByFetchedAtDesc("USD"))
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
        when(currencyRateRepository.findTopByCurrencyCodeOrderByFetchedAtDesc("XYZ"))
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
        when(instrumentRepository.findByTypeAndIsActiveTrue(InstrumentType.STOCK))
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
        
        when(instrumentRepository.findByTypeAndIsActiveTrue(InstrumentType.STOCK, pageable))
                .thenReturn(instrumentPage);

        Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(InstrumentType.STOCK, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getInstrument should return instrument by symbol")
    void getInstrument_ShouldReturnInstrumentBySymbol() {
        when(instrumentRepository.findBySymbol("THYAO"))
                .thenReturn(Optional.of(thyaoStock));

        InstrumentResponse result = marketDataService.getInstrument("THYAO");

        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("THYAO");
        assertThat(result.getName()).isEqualTo("Türk Hava Yolları");
    }

    @Test
    @DisplayName("getInstrument should throw exception when instrument not found")
    void getInstrument_ShouldThrowException_WhenNotFound() {
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
