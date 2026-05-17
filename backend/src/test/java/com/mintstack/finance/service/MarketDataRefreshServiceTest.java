package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.MarketRefreshResponse;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.scheduler.MarketDataScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataRefreshServiceTest {

    @Mock
    private ObjectProvider<MarketDataScheduler> marketDataSchedulerProvider;

    @Mock
    private MarketDataScheduler marketDataScheduler;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private MarketDataRefreshService refreshService;

    @BeforeEach
    void setUp() {
        refreshService = new MarketDataRefreshService(marketDataSchedulerProvider, cacheManager);
    }

    @Test
    void refreshMarketData_ShouldRunRequestedFetchesAndClearCaches() {
        when(marketDataSchedulerProvider.getIfAvailable()).thenReturn(marketDataScheduler);
        when(cacheManager.getCache("currencyRates")).thenReturn(cache);

        MarketRefreshResponse response = refreshService.refreshMarketData(List.of(
            DataType.CURRENCY_RATES,
            DataType.BIST_STOCKS,
            DataType.BIST_INDICES
        ));

        assertThat(response.refreshedDataTypes()).containsExactly(
            "CURRENCY_RATES",
            "BIST_STOCKS",
            "BIST_INDICES"
        );
        assertThat(response.skippedDataTypes()).isEmpty();
        verify(marketDataScheduler).fetchTcmbRates();
        verify(marketDataScheduler).fetchNonTcmbForexRates();
        verify(marketDataScheduler).refreshStockPricesNow();
        verify(marketDataScheduler).refreshIndexPricesNow();
        verify(cache).clear();
    }

    @Test
    void refreshMarketData_ShouldSkipUnsupportedTypes() {
        when(marketDataSchedulerProvider.getIfAvailable()).thenReturn(marketDataScheduler);

        MarketRefreshResponse response = refreshService.refreshMarketData(List.of(DataType.NEWS));

        assertThat(response.refreshedDataTypes()).isEmpty();
        assertThat(response.skippedDataTypes()).containsExactly("NEWS");
    }

    @Test
    void refreshMarketData_ShouldFail_WhenSchedulerIsDisabled() {
        when(marketDataSchedulerProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> refreshService.refreshMarketData(List.of(DataType.CURRENCY_RATES)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("scheduler is disabled");
    }
}
