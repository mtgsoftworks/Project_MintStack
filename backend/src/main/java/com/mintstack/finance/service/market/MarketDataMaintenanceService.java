package com.mintstack.finance.service.market;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.NewsRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataMaintenanceService {

    private final InstrumentRepository instrumentRepository;
    private final CurrencyRateRepository currencyRateRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final NewsRepository newsRepository;

    @Transactional
    public void saveCurrencyRates(List<CurrencyRate> rates) {
        currencyRateRepository.saveAll(rates);
        rates.forEach(this::upsertCurrencyInstrument);
        log.info("Saved {} currency rates - cache invalidated", rates.size());
    }

    @Transactional
    public void updateInstrumentPrice(String symbol, BigDecimal price) {
        Instrument instrument = instrumentRepository.findBySymbol(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Enstrüman", "sembol", symbol));

        instrument.setPreviousClose(instrument.getCurrentPrice());
        instrument.setCurrentPrice(price);
        instrumentRepository.save(instrument);

        log.debug("Updated price for {}: {}", symbol, price);
    }

    @Transactional
    public void savePriceHistory(PriceHistory priceHistory) {
        UUID instrumentId = priceHistory.getInstrument().getId();
        LocalDate priceDate = priceHistory.getPriceDate();

        Optional<PriceHistory> existing = priceHistoryRepository.findByInstrumentIdAndPriceDate(instrumentId, priceDate);
        if (existing.isPresent()) {
            PriceHistory current = existing.get();
            boolean replaceSyntheticOhlc = isSinglePriceHistory(current) && hasIntradayRange(priceHistory);
            if (priceHistory.getOpenPrice() != null && (!isPositive(current.getOpenPrice()) || replaceSyntheticOhlc)) {
                current.setOpenPrice(priceHistory.getOpenPrice());
            }
            if (priceHistory.getHighPrice() != null) {
                current.setHighPrice(replaceSyntheticOhlc
                    ? priceHistory.getHighPrice()
                    : maxPositive(current.getHighPrice(), priceHistory.getHighPrice()));
            }
            if (priceHistory.getLowPrice() != null) {
                current.setLowPrice(replaceSyntheticOhlc
                    ? priceHistory.getLowPrice()
                    : minPositive(current.getLowPrice(), priceHistory.getLowPrice()));
            }
            if (priceHistory.getClosePrice() != null) {
                current.setClosePrice(priceHistory.getClosePrice());
            }
            if (priceHistory.getAdjustedClose() != null) {
                current.setAdjustedClose(priceHistory.getAdjustedClose());
            }
            if (priceHistory.getVolume() != null) {
                current.setVolume(priceHistory.getVolume());
            }
            priceHistoryRepository.save(current);
            return;
        }

        priceHistoryRepository.save(priceHistory);
    }

    private boolean isSinglePriceHistory(PriceHistory history) {
        if (history == null) {
            return false;
        }
        BigDecimal close = firstPositive(history.getAdjustedClose(), history.getClosePrice());
        if (!isPositive(close)) {
            return false;
        }
        return matchesOrMissing(history.getOpenPrice(), close)
            && matchesOrMissing(history.getHighPrice(), close)
            && matchesOrMissing(history.getLowPrice(), close);
    }

    private boolean hasIntradayRange(PriceHistory history) {
        if (history == null) {
            return false;
        }
        BigDecimal close = firstPositive(history.getAdjustedClose(), history.getClosePrice());
        if (!isPositive(close)) {
            return false;
        }
        return differsFrom(history.getOpenPrice(), close)
            || differsFrom(history.getHighPrice(), close)
            || differsFrom(history.getLowPrice(), close);
    }

    private boolean differsFrom(BigDecimal value, BigDecimal reference) {
        return value != null && value.compareTo(reference) != 0;
    }

    private boolean matchesOrMissing(BigDecimal value, BigDecimal reference) {
        return value == null || value.compareTo(reference) == 0;
    }

    private BigDecimal maxPositive(BigDecimal current, BigDecimal incoming) {
        if (!isPositive(current)) {
            return incoming;
        }
        if (!isPositive(incoming)) {
            return current;
        }
        return current.max(incoming);
    }

    private BigDecimal minPositive(BigDecimal current, BigDecimal incoming) {
        if (!isPositive(current)) {
            return incoming;
        }
        if (!isPositive(incoming)) {
            return current;
        }
        return current.min(incoming);
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        if (values == null) {
            return null;
        }
        for (BigDecimal value : values) {
            if (isPositive(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    @Transactional
    public Map<String, Object> deleteAllMarketData() {
        long currencyCount = currencyRateRepository.count();
        long priceHistoryCount = priceHistoryRepository.count();
        long newsCount = newsRepository.count();
        InstrumentDeactivationSummary deactivationSummary = deactivateAllRealInstruments();

        currencyRateRepository.deleteAll();
        priceHistoryRepository.deleteAll();
        newsRepository.deleteAllInBatch();

        log.info("Deleted all market data: {} currency rates, {} price history records, {} news, {} real instruments deactivated ({} indices)",
            currencyCount, priceHistoryCount, newsCount, deactivationSummary.total(), deactivationSummary.indices());

        return Map.of(
            "deletedCurrencyRates", currencyCount,
            "deletedPriceHistory", priceHistoryCount,
            "deletedNews", newsCount,
            "deactivatedRealInstruments", deactivationSummary.total(),
            "deactivatedIndices", deactivationSummary.indices()
        );
    }

    private InstrumentDeactivationSummary deactivateAllRealInstruments() {
        List<Instrument> activeRealInstruments = instrumentRepository.findAll().stream()
            .filter(this::isRealInstrument)
            .filter(instrument -> Boolean.TRUE.equals(instrument.getIsActive()))
            .collect(Collectors.toList());
        if (activeRealInstruments.isEmpty()) {
            return new InstrumentDeactivationSummary(0L, 0L);
        }
        long activeIndexCount = activeRealInstruments.stream()
            .filter(instrument -> instrument.getType() == InstrumentType.INDEX)
            .count();
        activeRealInstruments.forEach(instrument -> instrument.setIsActive(false));
        instrumentRepository.saveAll(activeRealInstruments);
        return new InstrumentDeactivationSummary(activeRealInstruments.size(), activeIndexCount);
    }

    private boolean isRealInstrument(Instrument instrument) {
        return instrument != null && !Boolean.TRUE.equals(instrument.getIsSimulated());
    }

    private void upsertCurrencyInstrument(CurrencyRate rate) {
        if (rate == null || rate.getCurrencyCode() == null || rate.getSellingRate() == null
            || rate.getSellingRate().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String currencyCode = rate.getCurrencyCode().trim().toUpperCase();
        if ("TRY".equals(currencyCode)) {
            return;
        }

        String symbol = currencyCode + "TRY";
        Instrument instrument = instrumentRepository.findBySymbol(symbol)
            .orElseGet(() -> Instrument.builder()
                .symbol(symbol)
                .name(currencyCode + "/TRY")
                .type(InstrumentType.CURRENCY)
                .exchange(rate.getSource() != null ? rate.getSource().name() : "FX")
                .currency("TRY")
                .isActive(true)
                .isSimulated(false)
                .build());

        instrument.setName(rate.getCurrencyName() != null && !rate.getCurrencyName().isBlank()
            ? rate.getCurrencyName() + " / TRY"
            : currencyCode + "/TRY");
        instrument.setType(InstrumentType.CURRENCY);
        instrument.setExchange(rate.getSource() != null ? rate.getSource().name() : "FX");
        instrument.setCurrency("TRY");
        instrument.setIsActive(true);
        instrument.setIsSimulated(false);
        instrument.setPreviousClose(instrument.getCurrentPrice());
        instrument.setCurrentPrice(rate.getSellingRate());
        Instrument saved = instrumentRepository.save(instrument);
        if (saved == null) {
            saved = instrument;
        }
        if (saved.getId() == null) {
            return;
        }
        Instrument savedInstrument = saved;

        LocalDate priceDate = rate.getRateDate() != null
            ? rate.getRateDate().toLocalDate()
            : rate.getFetchedAt() != null ? rate.getFetchedAt().toLocalDate() : LocalDate.now();
        PriceHistory history = priceHistoryRepository.findByInstrumentIdAndPriceDate(savedInstrument.getId(), priceDate)
            .orElseGet(() -> PriceHistory.builder()
                .instrument(savedInstrument)
                .priceDate(priceDate)
                .build());
        BigDecimal buyingRate = rate.getBuyingRate() != null ? rate.getBuyingRate() : rate.getSellingRate();
        history.setOpenPrice(buyingRate);
        history.setLowPrice(buyingRate.min(rate.getSellingRate()));
        history.setHighPrice(buyingRate.max(rate.getSellingRate()));
        history.setClosePrice(rate.getSellingRate());
        history.setAdjustedClose(rate.getSellingRate());
        priceHistoryRepository.save(history);
    }

    private record InstrumentDeactivationSummary(long total, long indices) {
    }
}
