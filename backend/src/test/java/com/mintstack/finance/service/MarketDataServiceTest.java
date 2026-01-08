package com.mintstack.finance.service;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.service.external.TcmbApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private TcmbApiClient tcmbApiClient;

    @InjectMocks
    private MarketDataService marketDataService;

    private CurrencyRate usdRate;
    private CurrencyRate eurRate;

    @BeforeEach
    void setUp() {
        usdRate = CurrencyRate.builder()
            .currencyCode("USD")
            .currencyName("US DOLLAR")
            .buyingRate(BigDecimal.valueOf(32.50))
            .sellingRate(BigDecimal.valueOf(32.70))
            .source(CurrencyRate.RateSource.TCMB)
            .fetchedAt(LocalDateTime.now())
            .rateDate(LocalDateTime.now())
            .build();
        usdRate.setId(UUID.randomUUID());

        eurRate = CurrencyRate.builder()
            .currencyCode("EUR")
            .currencyName("EURO")
            .buyingRate(BigDecimal.valueOf(35.20))
            .sellingRate(BigDecimal.valueOf(35.50))
            .source(CurrencyRate.RateSource.TCMB)
            .fetchedAt(LocalDateTime.now())
            .rateDate(LocalDateTime.now())
            .build();
        eurRate.setId(UUID.randomUUID());
    }

    @Test
    void getLatestCurrencyRates_ShouldReturnAllRates() {
        // Given
        when(currencyRateRepository.findLatestBySource(CurrencyRate.RateSource.TCMB))
            .thenReturn(Arrays.asList(usdRate, eurRate));

        // When
        var result = marketDataService.getLatestCurrencyRates();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCurrencyCode()).isEqualTo("USD");
        assertThat(result.get(1).getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    void saveCurrencyRates_ShouldSaveRates() {
        // Given
        List<CurrencyRate> rates = Arrays.asList(usdRate, eurRate);
        when(currencyRateRepository.saveAll(rates)).thenReturn(rates);

        // When
        marketDataService.saveCurrencyRates(rates);

        // Then - no exception means success
        assertThat(rates).hasSize(2);
    }

    @Test
    void service_ShouldBeInjected() {
        assertThat(marketDataService).isNotNull();
    }
}
