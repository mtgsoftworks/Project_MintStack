package com.mintstack.finance.service.simulation;

import com.mintstack.finance.dto.cache.CurrencyRateData;
import com.mintstack.finance.service.PriceCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimulationMarketBootstrapService Unit Tests")
class SimulationMarketBootstrapServiceTest {

    @Mock
    private PriceCacheService priceCacheService;

    private SimulationMarketBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        bootstrapService = new SimulationMarketBootstrapService(priceCacheService);
    }

    @Test
    @DisplayName("initializeCaches should load all defaults when Redis is unavailable")
    void initializeCaches_ShouldLoadDefaults_WhenRedisUnavailable() {
        Map<String, SimulatedStock> stockCache = new HashMap<>();
        Map<String, SimulatedStock> bondCache = new HashMap<>();
        Map<String, SimulatedStock> fundCache = new HashMap<>();
        Map<String, SimulatedStock> viopCache = new HashMap<>();
        Map<String, SimulatedCurrency> currencyCache = new HashMap<>();
        Map<String, SimulatedIndex> indexCache = new HashMap<>();
        Map<String, SimulatedCrypto> cryptoCache = new HashMap<>();

        when(priceCacheService.isRedisAvailable()).thenReturn(false);

        bootstrapService.initializeCaches(
            stockCache,
            bondCache,
            fundCache,
            viopCache,
            currencyCache,
            indexCache,
            cryptoCache
        );

        assertThat(stockCache).isNotEmpty();
        assertThat(stockCache).containsKey("THYAO");
        assertThat(bondCache).containsKey("TRT150127T14");
        assertThat(fundCache).containsKey("MAC");
        assertThat(viopCache).containsKey("XU0300426");
        assertThat(currencyCache).isNotEmpty();
        assertThat(currencyCache).containsKey("USD");
        assertThat(indexCache).containsKey("XU100");
        assertThat(indexCache).containsKey("XU100.IS");
        assertThat(cryptoCache).isEmpty();
    }

    @Test
    @DisplayName("initializeCaches should backfill missing stock/index data when Redis has only currency data")
    void initializeCaches_ShouldBackfillMissingDatasets_WhenRedisHasOnlyCurrencies() {
        Map<String, SimulatedStock> stockCache = new HashMap<>();
        Map<String, SimulatedStock> bondCache = new HashMap<>();
        Map<String, SimulatedStock> fundCache = new HashMap<>();
        Map<String, SimulatedStock> viopCache = new HashMap<>();
        Map<String, SimulatedCurrency> currencyCache = new HashMap<>();
        Map<String, SimulatedIndex> indexCache = new HashMap<>();
        Map<String, SimulatedCrypto> cryptoCache = new HashMap<>();

        CurrencyRateData usdRate = CurrencyRateData.builder()
            .code("USD")
            .name("US Dollar")
            .buyingRate(new BigDecimal("38.10"))
            .sellingRate(new BigDecimal("38.30"))
            .midRate(new BigDecimal("38.20"))
            .build();

        when(priceCacheService.isRedisAvailable()).thenReturn(true);
        when(priceCacheService.getAllStockPrices()).thenReturn(Map.of());
        when(priceCacheService.getAllCurrencyRates()).thenReturn(Map.of("USD", usdRate));
        when(priceCacheService.getAllIndexValues()).thenReturn(Map.of());

        bootstrapService.initializeCaches(
            stockCache,
            bondCache,
            fundCache,
            viopCache,
            currencyCache,
            indexCache,
            cryptoCache
        );

        assertThat(currencyCache).containsKey("USD");
        assertThat(currencyCache.get("USD").getName()).isEqualTo("US Dollar");
        assertThat(stockCache).isNotEmpty();
        assertThat(stockCache).containsKey("THYAO");
        assertThat(bondCache).containsKey("TRT150127T14");
        assertThat(fundCache).containsKey("MAC");
        assertThat(viopCache).containsKey("XU0300426");
        assertThat(indexCache).containsKey("XU100");
        assertThat(indexCache).containsKey("XU100.IS");
        assertThat(cryptoCache).isEmpty();
    }
}
