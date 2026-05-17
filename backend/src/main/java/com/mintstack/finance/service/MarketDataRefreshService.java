package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.MarketRefreshResponse;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.scheduler.MarketDataScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataRefreshService {

    private static final List<String> MARKET_CACHE_NAMES = List.of(
        "currencyRates",
        "instruments",
        "stockPrices",
        "historicalData",
        "stock-prices",
        "currency-rates",
        "index-values"
    );

    private static final EnumSet<DataType> DEFAULT_REFRESH_TYPES = EnumSet.of(
        DataType.CURRENCY_RATES,
        DataType.BIST_STOCKS,
        DataType.BIST_INDICES
    );

    private final ObjectProvider<MarketDataScheduler> marketDataSchedulerProvider;
    private final CacheManager cacheManager;

    public MarketRefreshResponse refreshMarketData(List<DataType> requestedDataTypes) {
        LocalDateTime startedAt = LocalDateTime.now();
        MarketDataScheduler scheduler = marketDataSchedulerProvider.getIfAvailable();
        if (scheduler == null) {
            throw new IllegalStateException("Market data scheduler is disabled; manual refresh cannot run.");
        }

        LinkedHashSet<DataType> requested = normalizeRequestedDataTypes(requestedDataTypes);
        LinkedHashSet<DataType> refreshed = new LinkedHashSet<>();
        LinkedHashSet<DataType> skipped = new LinkedHashSet<>();

        for (DataType dataType : requested) {
            try {
                if (refreshByType(scheduler, dataType)) {
                    refreshed.add(dataType);
                } else {
                    skipped.add(dataType);
                }
            } catch (RuntimeException error) {
                skipped.add(dataType);
                log.warn("Manual market refresh failed for {}: {}", dataType, error.getMessage());
            }
        }

        evictMarketCaches();

        return new MarketRefreshResponse(
            namesOf(requested),
            namesOf(refreshed),
            namesOf(skipped),
            startedAt,
            LocalDateTime.now()
        );
    }

    private boolean refreshByType(MarketDataScheduler scheduler, DataType dataType) {
        return switch (dataType) {
            case CURRENCY_RATES -> {
                scheduler.fetchTcmbRates();
                scheduler.fetchNonTcmbForexRates();
                yield true;
            }
            case BIST_STOCKS, US_STOCKS -> {
                scheduler.refreshStockPricesNow();
                yield true;
            }
            case BIST_INDICES -> {
                scheduler.refreshIndexPricesNow();
                yield true;
            }
            case BONDS -> {
                scheduler.fetchBondPrices();
                yield true;
            }
            case FUNDS -> {
                scheduler.fetchFundPrices();
                yield true;
            }
            case VIOP -> {
                scheduler.fetchViopPrices();
                yield true;
            }
            default -> false;
        };
    }

    private LinkedHashSet<DataType> normalizeRequestedDataTypes(List<DataType> requestedDataTypes) {
        if (requestedDataTypes == null || requestedDataTypes.isEmpty()) {
            return new LinkedHashSet<>(DEFAULT_REFRESH_TYPES);
        }

        LinkedHashSet<DataType> normalized = new LinkedHashSet<>();
        for (DataType dataType : requestedDataTypes) {
            if (dataType != null) {
                normalized.add(dataType);
            }
        }
        return normalized.isEmpty()
            ? new LinkedHashSet<>(DEFAULT_REFRESH_TYPES)
            : normalized;
    }

    private void evictMarketCaches() {
        for (String cacheName : MARKET_CACHE_NAMES) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private List<String> namesOf(Set<DataType> dataTypes) {
        List<String> names = new ArrayList<>();
        for (DataType dataType : dataTypes) {
            names.add(dataType.name());
        }
        return names;
    }
}
