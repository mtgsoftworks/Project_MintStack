package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.CurrencyRateResponse;
import com.mintstack.finance.dto.response.InstrumentResponse;
import com.mintstack.finance.dto.response.PriceHistoryResponse;
import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.CurrencyRateRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService Tests")
class MarketDataServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private MarketDataService marketDataService;

    private CurrencyRate usdRate;
    private CurrencyRate eurRate;
    private Instrument thyStock;
    private PriceHistory priceHistory;

    @BeforeEach
    void setUp() {
        // Setup test data
        usdRate = CurrencyRate.builder()
                .id(1L)
                .currencyCode("USD")
                .currencyName("US Dollar")
                .buyingRate(new BigDecimal("32.50"))
                .sellingRate(new BigDecimal("32.80"))
                .averageRate(new BigDecimal("32.65"))
                .source(CurrencyRate.RateSource.TCMB)
                .fetchedAt(LocalDateTime.now())
                .rateDate(LocalDateTime.now())
                .build();

        eurRate = CurrencyRate.builder()
                .id(2L)
                .currencyCode("EUR")
                .currencyName("Euro")
                .buyingRate(new BigDecimal("35.20"))
                .sellingRate(new BigDecimal("35.50"))
                .averageRate(new BigDecimal("35.35"))
                .source(CurrencyRate.RateSource.TCMB)
                .fetchedAt(LocalDateTime.now())
                .rateDate(LocalDateTime.now())
                .build();

        thyStock = Instrument.builder()
                .id(1L)
                .symbol("THYAO")
                .name("Türk Hava Yolları")
                .type(Instrument.InstrumentType.STOCK)
                .exchange("BIST")
                .currency("TRY")
                .currentPrice(new BigDecimal("280.50"))
                .previousClose(new BigDecimal("275.00"))
                .isActive(true)
                .build();

        priceHistory = PriceHistory.builder()
                .id(1L)
                .instrument(thyStock)
                .priceDate(LocalDate.now().minusDays(1))
                .openPrice(new BigDecimal("275.00"))
                .highPrice(new BigDecimal("282.00"))
                .lowPrice(new BigDecimal("273.00"))
                .closePrice(new BigDecimal("280.50"))
                .volume(1500000L)
                .build();
    }

    @Nested
    @DisplayName("Currency Rate Tests")
    class CurrencyRateTests {

        @Test
        @DisplayName("Should return latest currency rates")
        void getLatestCurrencyRates_shouldReturnRates() {
            // Given
            when(currencyRateRepository.findLatestBySource(CurrencyRate.RateSource.TCMB))
                    .thenReturn(Arrays.asList(usdRate, eurRate));

            // When
            List<CurrencyRateResponse> result = marketDataService.getLatestCurrencyRates();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCurrencyCode()).isEqualTo("USD");
            assertThat(result.get(1).getCurrencyCode()).isEqualTo("EUR");
            verify(currencyRateRepository, times(1)).findLatestBySource(CurrencyRate.RateSource.TCMB);
        }

        @Test
        @DisplayName("Should return empty list when no rates available")
        void getLatestCurrencyRates_whenNoRates_shouldReturnEmptyList() {
            // Given
            when(currencyRateRepository.findLatestBySource(CurrencyRate.RateSource.TCMB))
                    .thenReturn(Collections.emptyList());

            // When
            List<CurrencyRateResponse> result = marketDataService.getLatestCurrencyRates();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return currency rate by code")
        void getCurrencyRate_shouldReturnRate() {
            // Given
            when(currencyRateRepository.findTopByCurrencyCodeOrderByFetchedAtDesc("USD"))
                    .thenReturn(Optional.of(usdRate));

            // When
            CurrencyRateResponse result = marketDataService.getCurrencyRate("USD");

            // Then
            assertThat(result.getCurrencyCode()).isEqualTo("USD");
            assertThat(result.getBuyingRate()).isEqualByComparingTo(new BigDecimal("32.50"));
        }

        @Test
        @DisplayName("Should throw exception when currency not found")
        void getCurrencyRate_whenNotFound_shouldThrowException() {
            // Given
            when(currencyRateRepository.findTopByCurrencyCodeOrderByFetchedAtDesc("XYZ"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> marketDataService.getCurrencyRate("XYZ"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should return currency history")
        void getCurrencyHistory_shouldReturnHistory() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            when(currencyRateRepository.findHistoryByCurrencyCode(eq("USD"), any(), any()))
                    .thenReturn(Arrays.asList(usdRate));

            // When
            List<CurrencyRateResponse> result = marketDataService.getCurrencyHistory("USD", startDate, endDate);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCurrencyCode()).isEqualTo("USD");
        }
    }

    @Nested
    @DisplayName("Instrument Tests")
    class InstrumentTests {

        @Test
        @DisplayName("Should return instruments by type")
        void getInstrumentsByType_shouldReturnInstruments() {
            // Given
            when(instrumentRepository.findByTypeAndIsActiveTrue(Instrument.InstrumentType.STOCK))
                    .thenReturn(Arrays.asList(thyStock));

            // When
            List<InstrumentResponse> result = marketDataService.getInstrumentsByType(Instrument.InstrumentType.STOCK);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSymbol()).isEqualTo("THYAO");
        }

        @Test
        @DisplayName("Should return paginated instruments by type")
        void getInstrumentsByType_paginated_shouldReturnPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Instrument> page = new PageImpl<>(Arrays.asList(thyStock), pageable, 1);
            when(instrumentRepository.findByTypeAndIsActiveTrue(Instrument.InstrumentType.STOCK, pageable))
                    .thenReturn(page);

            // When
            Page<InstrumentResponse> result = marketDataService.getInstrumentsByType(Instrument.InstrumentType.STOCK, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return instrument by symbol")
        void getInstrument_shouldReturnInstrument() {
            // Given
            when(instrumentRepository.findBySymbol("THYAO"))
                    .thenReturn(Optional.of(thyStock));

            // When
            InstrumentResponse result = marketDataService.getInstrument("THYAO");

            // Then
            assertThat(result.getSymbol()).isEqualTo("THYAO");
            assertThat(result.getName()).isEqualTo("Türk Hava Yolları");
            assertThat(result.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("280.50"));
        }

        @Test
        @DisplayName("Should throw exception when instrument not found")
        void getInstrument_whenNotFound_shouldThrowException() {
            // Given
            when(instrumentRepository.findBySymbol("INVALID"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> marketDataService.getInstrument("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should search instruments by query")
        void searchInstruments_shouldReturnMatchingInstruments() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Instrument> page = new PageImpl<>(Arrays.asList(thyStock), pageable, 1);
            when(instrumentRepository.searchBySymbolOrName("THY", pageable))
                    .thenReturn(page);

            // When
            Page<InstrumentResponse> result = marketDataService.searchInstruments("THY", pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getSymbol()).isEqualTo("THYAO");
        }

        @Test
        @DisplayName("Should calculate price change correctly")
        void getInstrument_shouldCalculateChange() {
            // Given
            when(instrumentRepository.findBySymbol("THYAO"))
                    .thenReturn(Optional.of(thyStock));

            // When
            InstrumentResponse result = marketDataService.getInstrument("THYAO");

            // Then
            assertThat(result.getChange()).isEqualByComparingTo(new BigDecimal("5.50")); // 280.50 - 275.00
        }
    }

    @Nested
    @DisplayName("Price History Tests")
    class PriceHistoryTests {

        @Test
        @DisplayName("Should return price history for date range")
        void getPriceHistory_shouldReturnHistory() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            when(priceHistoryRepository.findBySymbolAndDateRange("THYAO", startDate, endDate))
                    .thenReturn(Arrays.asList(priceHistory));

            // When
            List<PriceHistoryResponse> result = marketDataService.getPriceHistory("THYAO", startDate, endDate);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getClose()).isEqualByComparingTo(new BigDecimal("280.50"));
        }

        @Test
        @DisplayName("Should return recent price history")
        void getRecentPriceHistory_shouldReturnHistory() {
            // Given
            when(priceHistoryRepository.findBySymbolAndDateRange(eq("THYAO"), any(), any()))
                    .thenReturn(Arrays.asList(priceHistory));

            // When
            List<PriceHistoryResponse> result = marketDataService.getRecentPriceHistory("THYAO", 30);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Update Operations Tests")
    class UpdateOperationsTests {

        @Test
        @DisplayName("Should save currency rates")
        void saveCurrencyRates_shouldSaveAll() {
            // Given
            List<CurrencyRate> rates = Arrays.asList(usdRate, eurRate);

            // When
            marketDataService.saveCurrencyRates(rates);

            // Then
            verify(currencyRateRepository, times(1)).saveAll(rates);
        }

        @Test
        @DisplayName("Should update instrument price")
        void updateInstrumentPrice_shouldUpdate() {
            // Given
            when(instrumentRepository.findBySymbol("THYAO"))
                    .thenReturn(Optional.of(thyStock));
            BigDecimal newPrice = new BigDecimal("285.00");

            // When
            marketDataService.updateInstrumentPrice("THYAO", newPrice);

            // Then
            verify(instrumentRepository, times(1)).save(any(Instrument.class));
        }
    }
}
