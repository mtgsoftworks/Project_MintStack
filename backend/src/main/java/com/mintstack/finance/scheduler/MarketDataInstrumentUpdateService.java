package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.PriceUpdateService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Observed(name = "market-data.instrument-update", contextualName = "market-data-instrument-update")
class MarketDataInstrumentUpdateService {

    private final InstrumentRepository instrumentRepository;
    private final MarketDataService marketDataService;
    private final PriceUpdateService priceUpdateService;
    private final MarketDataProviderResolver providerResolver;

    @Value("${app.scheduler.instrument-batch-size:5}")
    private int configuredBatchSize;

    private final Map<Instrument.InstrumentType, Integer> updateOffsets =
        new EnumMap<>(Instrument.InstrumentType.class);

    @Observed(name = "market-data.update-prices-for-type", contextualName = "update-prices-for-type")
    public void updatePricesForType(
        Instrument.InstrumentType type,
        UserApiConfig yahooConfig,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig,
        EnumMap<DataType, ApiProvider> preferredProviders
    ) {
        if (!isExternallyUpdatableType(type)) {
            log.debug("Skipping external price update for unsupported type: {}", type);
            return;
        }

        List<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulatedOrderBySymbolAsc(type, false);

        if (instruments.isEmpty()) {
            log.info("No {} instruments found to update.", type);
            return;
        }

        int batchSize = resolveBatchSize();
        int currentOffset = updateOffsets.getOrDefault(type, 0);
        if (currentOffset >= instruments.size()) {
            currentOffset = 0;
        }

        int end = Math.min(currentOffset + batchSize, instruments.size());
        List<Instrument> batch = instruments.subList(currentOffset, end);

        log.info(
            "Round-Robin {}: Fetching batch {} instruments (Index {}-{} of {})",
            type,
            batch.size(),
            currentOffset,
            end,
            instruments.size()
        );

        for (Instrument instrument : batch) {
            try {
                DataType dataType = providerResolver.resolveDataTypeForInstrument(type, instrument);
                if (dataType == null) {
                    continue;
                }
                ApiProvider preferredProvider = preferredProviders.get(dataType);
                BigDecimal price = providerResolver.fetchInstrumentPrice(
                    instrument,
                    dataType,
                    preferredProvider,
                    yahooConfig,
                    alphaConfig,
                    finnhubConfig
                );

                if (price != null) {
                    Long volume = providerResolver.resolveLatestVolume(instrument);
                    updateInstrumentPrice(instrument, price, volume);
                }
            } catch (Exception error) {
                log.error("Failed to update price for {} ({}): {}", instrument.getSymbol(), type, error.getMessage());
            }
        }

        int nextOffset = currentOffset + batchSize;
        if (nextOffset >= instruments.size()) {
            nextOffset = 0;
            log.info("Finished one full cycle of {} updates. Resetting to start.", type);
        }
        updateOffsets.put(type, nextOffset);
    }

    @Observed(name = "market-data.update-instrument-price", contextualName = "update-instrument-price")
    public void updateInstrumentPrice(Instrument instrument, BigDecimal price, Long volume) {
        BigDecimal previousClose = instrument.getCurrentPrice();
        instrument.setPreviousClose(previousClose);
        instrument.setCurrentPrice(price);
        instrumentRepository.save(instrument);
        saveDailyPriceHistory(instrument, price, volume);

        BigDecimal change = previousClose != null ? price.subtract(previousClose) : BigDecimal.ZERO;
        BigDecimal changePercent = previousClose != null && previousClose.compareTo(BigDecimal.ZERO) != 0
            ? change.divide(previousClose, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        priceUpdateService.broadcastStockUpdate(
            instrument.getSymbol(),
            price,
            previousClose,
            change,
            changePercent
        );
    }

    private void saveDailyPriceHistory(Instrument instrument, BigDecimal price, Long volume) {
        if (price == null) {
            return;
        }

        try {
            PriceHistory history = PriceHistory.builder()
                .instrument(instrument)
                .priceDate(LocalDate.now())
                .closePrice(price)
                .openPrice(price)
                .highPrice(price)
                .lowPrice(price)
                .volume(volume)
                .build();
            marketDataService.savePriceHistory(history);
        } catch (Exception error) {
            log.warn("Failed to save daily history for {}", instrument.getSymbol());
        }
    }

    private int resolveBatchSize() {
        return configuredBatchSize > 0 ? configuredBatchSize : 5;
    }

    private boolean isExternallyUpdatableType(Instrument.InstrumentType type) {
        return type == Instrument.InstrumentType.STOCK
            || type == Instrument.InstrumentType.CRYPTO
            || type == Instrument.InstrumentType.INDEX;
    }
}
