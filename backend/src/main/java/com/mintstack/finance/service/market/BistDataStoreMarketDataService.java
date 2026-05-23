package com.mintstack.finance.service.market;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.service.PriceUpdateService;
import com.mintstack.finance.service.external.BistDataStoreClient;
import com.mintstack.finance.service.external.BistDataStoreClient.BistBondPrice;
import com.mintstack.finance.service.external.BistDataStoreClient.BistViopPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BistDataStoreMarketDataService {

    private static final Pattern TREASURY_BOND_MATURITY_SYMBOL_PATTERN =
        Pattern.compile("^TR[A-Z0-9](\\d{2})(\\d{2})(\\d{2})[A-Z0-9]{3}$");
    private static final Pattern CORPORATE_BOND_MONTH_YEAR_DAY_PATTERN =
        Pattern.compile("([1-9]|1[0-2])(\\d{2})(\\d{2})$");
    private static final Pattern CORPORATE_BOND_MONTH_CODE_PATTERN =
        Pattern.compile("([A-L])(\\d{2})(\\d{2})$");

    private final BistDataStoreClient bistDataStoreClient;
    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceUpdateService priceUpdateService;

    @Value("${app.external-api.bist-datastore.enabled:true}")
    private boolean enabled;

    @Value("${app.external-api.bist-datastore.latest-lookback-days:7}")
    private int latestLookbackDays;

    @Value("${app.external-api.bist-datastore.maturity-lookback-days:120}")
    private int maturityLookbackDays;

    @Value("${app.market-data.max-active-viop-instruments:300}")
    private int maxActiveViopInstruments;

    @Value("${app.market-data.min-viop-volume:1}")
    private long minViopVolume;

    @Transactional
    public int refreshBondPrices() {
        if (!enabled) {
            log.debug("BIST DataStore integration disabled. Skipping bond refresh.");
            return 0;
        }
        List<BistBondPrice> prices = fetchLatestBondPrices();
        int saved = upsertBondPrices(prices, ignored -> true, Integer.MAX_VALUE);
        BondMaturityEnrichmentResult enrichmentResult = enrichBondMaturityMetadata();
        log.info("BIST DataStore bond refresh completed: {} rows processed", saved);
        logBondMaturityEnrichment(enrichmentResult);
        return saved;
    }

    @Transactional
    public int refreshViopPrices() {
        if (!enabled) {
            log.debug("BIST DataStore integration disabled. Skipping VIOP refresh.");
            return 0;
        }
        List<BistViopPrice> prices = fetchLatestViopPrices();
        if (prices.isEmpty()) {
            log.warn("BIST DataStore VIOP refresh returned empty list; keeping current active contracts.");
            return 0;
        }

        int cap = resolveViopCap();
        List<BistViopPrice> ranked = rankViopPrices(prices);
        List<BistViopPrice> selected = ranked.stream()
            .limit(cap)
            .toList();
        Set<String> selectedSymbols = selected.stream()
            .map(BistViopPrice::symbol)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        int saved = upsertViopPrices(selected, ignored -> true, cap);
        deactivateViopOutsideSelection(selectedSymbols);
        log.info("BIST DataStore VIOP refresh completed: {} selected / {} fetched (active cap={})",
            saved, prices.size(), cap);
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
        logBondMaturityEnrichment(enrichBondMaturityMetadata());
        return saved;
    }

    @Transactional
    public BondMaturityEnrichmentResult enrichBondMaturityMetadata() {
        int parsedFromSymbols = backfillBondMaturityFromSymbols();
        int enrichedFromBulletins = backfillBondMaturityFromRecentBulletins();
        return new BondMaturityEnrichmentResult(parsedFromSymbols, enrichedFromBulletins);
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
            if (price.maturityDate() != null) {
                instrument.setMaturityDate(price.maturityDate());
            }
            instrument.setPreviousClose(resolvePreviousClose(price.previousClose(), instrument.getPreviousClose()));
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
            broadcastMarketUpdate(
                "BOND",
                savedInstrument,
                price.closePrice(),
                "previousClose", price.previousClose(),
                "maturityDate", price.maturityDate(),
                "tradeDate", price.date()
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
            if (price.maturityDate() != null) {
                instrument.setMaturityDate(price.maturityDate());
            }
            instrument.setPreviousClose(resolvePreviousSettlementPrice(price, instrument));
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
            broadcastMarketUpdate(
                "VIOP",
                savedInstrument,
                price.settlementPrice(),
                "previousSettlementPrice", price.previousSettlementPrice(),
                "maturityDate", price.maturityDate(),
                "tradeDate", price.date(),
                "tradeVolume", price.tradeVolume()
            );
            processed.add(price.symbol());
            saved++;
        }
        return saved;
    }

    private void broadcastMarketUpdate(String type, Instrument instrument, BigDecimal price, Object... keyValues) {
        if (instrument == null || instrument.getSymbol() == null || price == null) {
            return;
        }

        Map<String, Object> additionalData = new HashMap<>();
        if (keyValues != null) {
            for (int index = 0; index + 1 < keyValues.length; index += 2) {
                Object key = keyValues[index];
                Object value = keyValues[index + 1];
                if (key instanceof String keyText && value != null) {
                    additionalData.put(keyText, value);
                }
            }
        }

        priceUpdateService.broadcastMarketUpdate(type, instrument.getSymbol(), price, additionalData);
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

    private BigDecimal resolvePreviousClose(BigDecimal incomingPreviousClose, BigDecimal existingPreviousClose) {
        BigDecimal explicitPrevious = firstPositive(incomingPreviousClose);
        if (explicitPrevious != null) {
            return explicitPrevious;
        }
        return firstPositive(existingPreviousClose);
    }

    private BigDecimal resolvePreviousSettlementPrice(BistViopPrice price, Instrument existing) {
        BigDecimal explicitPrevious = firstPositive(price.previousSettlementPrice());
        if (explicitPrevious != null) {
            return explicitPrevious;
        }

        BigDecimal settlement = price.settlementPrice();
        BigDecimal changePercent = price.changePercent();
        if (settlement != null && settlement.signum() > 0 && changePercent != null) {
            BigDecimal divisor = BigDecimal.ONE.add(
                changePercent.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
            );
            if (divisor.signum() > 0) {
                BigDecimal derived = settlement
                    .divide(divisor, 6, RoundingMode.HALF_UP);
                if (derived.signum() > 0) {
                    return derived;
                }
            }
        }

        BigDecimal existingPrevious = firstPositive(existing.getPreviousClose());
        if (existingPrevious != null && settlement != null && existingPrevious.compareTo(settlement) != 0) {
            return existingPrevious;
        }

        BigDecimal historyPrevious = resolvePreviousCloseFromHistory(existing, price.date());
        if (historyPrevious != null && settlement != null && historyPrevious.compareTo(settlement) != 0) {
            return historyPrevious;
        }

        return null;
    }

    private BigDecimal resolvePreviousCloseFromHistory(Instrument instrument, LocalDate currentDate) {
        if (instrument == null || instrument.getId() == null || currentDate == null) {
            return null;
        }

        List<PriceHistory> recent = priceHistoryRepository.findByInstrumentIdOrderByPriceDateDesc(
            instrument.getId(),
            PageRequest.of(0, 20)
        );
        for (PriceHistory history : recent) {
            if (history.getPriceDate() == null || !history.getPriceDate().isBefore(currentDate)) {
                continue;
            }
            BigDecimal close = firstPositive(history.getClosePrice(), history.getAdjustedClose(), history.getOpenPrice());
            if (close != null) {
                return close;
            }
        }
        return null;
    }

    private List<BistViopPrice> rankViopPrices(List<BistViopPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            return List.of();
        }

        List<BistViopPrice> withPositiveVolume = prices.stream()
            .filter(item -> item != null && item.symbol() != null && item.settlementPrice() != null && item.settlementPrice().signum() > 0)
            .filter(item -> item.tradeVolume() != null && item.tradeVolume().longValue() >= Math.max(0L, minViopVolume))
            .sorted(Comparator
                .comparing((BistViopPrice item) -> item.tradeVolume() == null ? BigDecimal.ZERO : item.tradeVolume(), BigDecimal::compareTo).reversed()
                .thenComparing(item -> item.tradedValue() == null ? BigDecimal.ZERO : item.tradedValue(), BigDecimal::compareTo).reversed()
                .thenComparing(BistViopPrice::symbol))
            .toList();

        if (!withPositiveVolume.isEmpty()) {
            return withPositiveVolume;
        }

        return prices.stream()
            .filter(item -> item != null && item.symbol() != null && item.settlementPrice() != null && item.settlementPrice().signum() > 0)
            .sorted(Comparator
                .comparing((BistViopPrice item) -> item.tradedValue() == null ? BigDecimal.ZERO : item.tradedValue(), BigDecimal::compareTo).reversed()
                .thenComparing(BistViopPrice::symbol))
            .toList();
    }

    private void deactivateViopOutsideSelection(Set<String> selectedSymbols) {
        if (selectedSymbols == null || selectedSymbols.isEmpty()) {
            return;
        }

        List<Instrument> activeViop = instrumentRepository.findByTypeAndIsActiveTrue(Instrument.InstrumentType.VIOP);
        List<Instrument> toDeactivate = activeViop.stream()
            .filter(contract -> contract.getSymbol() != null && !selectedSymbols.contains(contract.getSymbol()))
            .peek(contract -> contract.setIsActive(false))
            .toList();

        if (!toDeactivate.isEmpty()) {
            instrumentRepository.saveAll(toDeactivate);
            log.info("BIST DataStore VIOP universe trimmed: {} contracts deactivated outside top selection", toDeactivate.size());
        }
    }

    private int resolveViopCap() {
        return Math.max(50, Math.min(2000, maxActiveViopInstruments));
    }

    private int backfillBondMaturityFromSymbols() {
        List<Instrument> bonds = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulatedOrderBySymbolAsc(
            Instrument.InstrumentType.BOND,
            false
        );
        int updated = 0;
        for (Instrument bond : bonds) {
            if (bond.getMaturityDate() != null) {
                continue;
            }
            LocalDate maturityDate = parseBondMaturityFromSymbol(bond.getSymbol());
            if (maturityDate == null) {
                continue;
            }
            bond.setMaturityDate(maturityDate);
            instrumentRepository.save(bond);
            updated++;
        }
        return updated;
    }

    private int backfillBondMaturityFromRecentBulletins() {
        List<Instrument> bonds = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulatedOrderBySymbolAsc(
            Instrument.InstrumentType.BOND,
            false
        );

        java.util.Map<String, Instrument> unresolved = bonds.stream()
            .filter(instrument -> instrument.getMaturityDate() == null)
            .collect(java.util.stream.Collectors.toMap(
                instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT),
                instrument -> instrument,
                (left, right) -> left,
                java.util.LinkedHashMap::new
            ));
        if (unresolved.isEmpty()) {
            return 0;
        }

        int lookbackDays = Math.max(1, maturityLookbackDays);
        int updated = 0;
        LocalDate date = LocalDate.now();
        for (int offset = 0; offset < lookbackDays && !unresolved.isEmpty(); offset++) {
            if (!isWeekend(date)) {
                try {
                    java.util.Map<String, LocalDate> hints = bistDataStoreClient.fetchBondMaturityHints(date);
                    for (java.util.Map.Entry<String, LocalDate> hint : hints.entrySet()) {
                        if (hint.getValue() == null) {
                            continue;
                        }
                        Instrument instrument = unresolved.remove(hint.getKey().toUpperCase(Locale.ROOT));
                        if (instrument == null) {
                            continue;
                        }
                        instrument.setMaturityDate(hint.getValue());
                        instrumentRepository.save(instrument);
                        updated++;
                    }
                } catch (Exception error) {
                    log.debug("BIST DataStore bond maturity hint fetch skipped for {}: {}", date, error.getMessage());
                }
            }
            date = date.minusDays(1);
        }
        return updated;
    }

    private void logBondMaturityEnrichment(BondMaturityEnrichmentResult result) {
        if (result == null) {
            return;
        }
        if (result.parsedFromSymbols() > 0) {
            log.info("BIST DataStore bond maturity enrichment completed: {} rows updated", result.parsedFromSymbols());
        }
        if (result.enrichedFromBulletins() > 0) {
            log.info("BIST DataStore bond maturity backfill from bulletins completed: {} rows updated", result.enrichedFromBulletins());
        }
    }

    public record BondMaturityEnrichmentResult(
        int parsedFromSymbols,
        int enrichedFromBulletins
    ) {
    }

    private LocalDate parseBondMaturityFromSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        String normalized = symbol.toUpperCase(Locale.ROOT);
        Matcher treasuryMatcher = TREASURY_BOND_MATURITY_SYMBOL_PATTERN.matcher(normalized);
        if (treasuryMatcher.matches()) {
            return safeDate(
                2000 + Integer.parseInt(treasuryMatcher.group(3)),
                Integer.parseInt(treasuryMatcher.group(2)),
                Integer.parseInt(treasuryMatcher.group(1))
            );
        }

        Matcher monthYearDayMatcher = CORPORATE_BOND_MONTH_YEAR_DAY_PATTERN.matcher(normalized);
        if (monthYearDayMatcher.find()) {
            return safeDate(
                2000 + Integer.parseInt(monthYearDayMatcher.group(2)),
                Integer.parseInt(monthYearDayMatcher.group(1)),
                Integer.parseInt(monthYearDayMatcher.group(3))
            );
        }

        Matcher monthCodeMatcher = CORPORATE_BOND_MONTH_CODE_PATTERN.matcher(normalized);
        if (monthCodeMatcher.find()) {
            Integer month = monthFromCode(monthCodeMatcher.group(1).charAt(0));
            if (month == null) {
                return null;
            }
            return safeDate(
                2000 + Integer.parseInt(monthCodeMatcher.group(2)),
                month,
                Integer.parseInt(monthCodeMatcher.group(3))
            );
        }

        return null;
    }

    private Integer monthFromCode(char monthCode) {
        if (monthCode < 'A' || monthCode > 'L') {
            return null;
        }
        return (monthCode - 'A') + 1;
    }

    private LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Long toLong(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
            return null;
        }
        return value.longValue();
    }
}
