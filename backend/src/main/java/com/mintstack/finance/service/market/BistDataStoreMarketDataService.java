package com.mintstack.finance.service.market;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.service.external.BistDataStoreClient;
import com.mintstack.finance.service.external.BistDataStoreClient.BistBondPrice;
import com.mintstack.finance.service.external.BistDataStoreClient.BistViopPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BistDataStoreMarketDataService {

    private final BistDataStoreClient bistDataStoreClient;
    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Value("${app.external-api.bist-datastore.enabled:true}")
    private boolean enabled;

    @Value("${app.external-api.bist-datastore.latest-lookback-days:7}")
    private int latestLookbackDays;

    @Transactional
    public int refreshBondPrices() {
        if (!enabled) {
            log.debug("BIST DataStore integration disabled. Skipping bond refresh.");
            return 0;
        }
        List<BistBondPrice> prices = fetchLatestBondPrices();
        int saved = upsertBondPrices(prices, ignored -> true, Integer.MAX_VALUE);
        log.info("BIST DataStore bond refresh completed: {} rows processed", saved);
        return saved;
    }

    @Transactional
    public int refreshViopPrices() {
        if (!enabled) {
            log.debug("BIST DataStore integration disabled. Skipping VIOP refresh.");
            return 0;
        }
        List<BistViopPrice> prices = fetchLatestViopPrices();
        int saved = upsertViopPrices(prices, ignored -> true, Integer.MAX_VALUE);
        log.info("BIST DataStore VIOP refresh completed: {} rows processed", saved);
        return saved;
    }

    @Transactional
    public int backfillBondPrices(LocalDate startDate, LocalDate endDate, Set<String> symbols, int maxInstruments) {
        if (!enabled) {
            return 0;
        }
        Predicate<String> filter = symbolFilter(symbols);
        int saved = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (isWeekend(date)) {
                continue;
            }
            try {
                saved += upsertBondPrices(bistDataStoreClient.fetchBondPrices(date), filter, maxInstruments);
            } catch (Exception error) {
                log.debug("BIST DataStore bond backfill skipped for {}: {}", date, error.getMessage());
            }
        }
        return saved;
    }

    @Transactional
    public int backfillViopPrices(LocalDate startDate, LocalDate endDate, Set<String> symbols, int maxInstruments) {
        if (!enabled) {
            return 0;
        }
        Predicate<String> filter = symbolFilter(symbols);
        int saved = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (isWeekend(date)) {
                continue;
            }
            try {
                saved += upsertViopPrices(bistDataStoreClient.fetchViopPrices(date), filter, maxInstruments);
            } catch (Exception error) {
                log.debug("BIST DataStore VIOP backfill skipped for {}: {}", date, error.getMessage());
            }
        }
        return saved;
    }

    private List<BistBondPrice> fetchLatestBondPrices() {
        LocalDate date = LocalDate.now();
        int attempts = Math.max(1, latestLookbackDays);
        for (int index = 0; index < attempts; index++) {
            try {
                List<BistBondPrice> prices = bistDataStoreClient.fetchBondPrices(date);
                if (!prices.isEmpty()) {
                    return prices;
                }
            } catch (Exception error) {
                log.debug("BIST DataStore bond fetch skipped for {}: {}", date, error.getMessage());
            }
            date = date.minusDays(1);
        }
        return List.of();
    }

    private List<BistViopPrice> fetchLatestViopPrices() {
        LocalDate date = LocalDate.now();
        int attempts = Math.max(1, latestLookbackDays);
        for (int index = 0; index < attempts; index++) {
            try {
                List<BistViopPrice> prices = bistDataStoreClient.fetchViopPrices(date);
                if (!prices.isEmpty()) {
                    return prices;
                }
            } catch (Exception error) {
                log.debug("BIST DataStore VIOP fetch skipped for {}: {}", date, error.getMessage());
            }
            date = date.minusDays(1);
        }
        return List.of();
    }

    private int upsertBondPrices(List<BistBondPrice> prices, Predicate<String> symbolFilter, int maxInstruments) {
        int saved = 0;
        Set<String> processed = new LinkedHashSet<>();
        for (BistBondPrice price : prices) {
            if (processed.size() >= maxInstruments) {
                break;
            }
            if (!symbolFilter.test(price.symbol())) {
                continue;
            }
            Instrument instrument = instrumentRepository.findBySymbol(price.symbol())
                .orElseGet(() -> Instrument.builder()
                    .symbol(price.symbol())
                    .name(price.name())
                    .type(Instrument.InstrumentType.BOND)
                    .exchange("BIST")
                    .currency(normalizeCurrency(price.currency()))
                    .isActive(true)
                    .isSimulated(false)
                    .build());

            instrument.setName(price.name());
            instrument.setType(Instrument.InstrumentType.BOND);
            instrument.setExchange("BIST");
            instrument.setCurrency(normalizeCurrency(price.currency()));
            instrument.setIsActive(true);
            instrument.setIsSimulated(false);
            instrument.setPreviousClose(firstPositive(price.previousClose(), instrument.getCurrentPrice()));
            instrument.setCurrentPrice(price.closePrice());
            Instrument savedInstrument = instrumentRepository.save(instrument);
            upsertHistory(
                savedInstrument,
                price.date(),
                firstPositive(price.openPrice(), price.closePrice()),
                price.highPrice(),
                price.lowPrice(),
                price.closePrice(),
                toLong(price.quantity())
            );
            processed.add(price.symbol());
            saved++;
        }
        return saved;
    }

    private int upsertViopPrices(List<BistViopPrice> prices, Predicate<String> symbolFilter, int maxInstruments) {
        int saved = 0;
        Set<String> processed = new LinkedHashSet<>();
        for (BistViopPrice price : prices) {
            if (processed.size() >= maxInstruments) {
                break;
            }
            if (!symbolFilter.test(price.symbol())) {
                continue;
            }
            Instrument instrument = instrumentRepository.findBySymbol(price.symbol())
                .orElseGet(() -> Instrument.builder()
                    .symbol(price.symbol())
                    .name(price.name())
                    .type(Instrument.InstrumentType.VIOP)
                    .exchange("VIOP")
                    .currency(normalizeCurrency(price.currency()))
                    .isActive(true)
                    .isSimulated(false)
                    .build());

            instrument.setName(price.name());
            instrument.setType(Instrument.InstrumentType.VIOP);
            instrument.setExchange("VIOP");
            instrument.setCurrency(normalizeCurrency(price.currency()));
            instrument.setIsActive(true);
            instrument.setIsSimulated(false);
            instrument.setPreviousClose(firstPositive(price.previousSettlementPrice(), instrument.getCurrentPrice()));
            instrument.setCurrentPrice(price.settlementPrice());
            Instrument savedInstrument = instrumentRepository.save(instrument);
            upsertHistory(
                savedInstrument,
                price.date(),
                firstPositive(price.openPrice(), price.settlementPrice()),
                price.highPrice(),
                price.lowPrice(),
                firstPositive(price.closingPrice(), price.settlementPrice()),
                toLong(price.tradeVolume())
            );
            processed.add(price.symbol());
            saved++;
        }
        return saved;
    }

    private void upsertHistory(
        Instrument instrument,
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume
    ) {
        if (date == null || close == null || close.signum() <= 0) {
            return;
        }
        PriceHistory history = priceHistoryRepository
            .findByInstrumentIdAndPriceDate(instrument.getId(), date)
            .orElseGet(() -> PriceHistory.builder()
                .instrument(instrument)
                .priceDate(date)
                .build());
        history.setOpenPrice(firstPositive(open, close));
        history.setHighPrice(firstPositive(high, close));
        history.setLowPrice(firstPositive(low, close));
        history.setClosePrice(close);
        history.setAdjustedClose(close);
        history.setVolume(volume);
        priceHistoryRepository.save(history);
    }

    private Predicate<String> symbolFilter(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return ignored -> true;
        }
        Set<String> normalized = symbols.stream()
            .filter(symbol -> symbol != null && !symbol.isBlank())
            .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toSet());
        return symbol -> normalized.contains(symbol.toUpperCase(Locale.ROOT));
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "TRY" : currency.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null && value.signum() > 0) {
                return value;
            }
        }
        return null;
    }

    private Long toLong(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
            return null;
        }
        return value.longValue();
    }
}
