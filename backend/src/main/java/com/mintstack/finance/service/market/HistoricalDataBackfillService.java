package com.mintstack.finance.service.market;

import com.mintstack.finance.dto.request.HistoricalDataBackfillRequest;
import com.mintstack.finance.dto.response.HistoricalDataBackfillResponse;
import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.external.TcmbApiClient;
import com.mintstack.finance.service.external.TefasFundClient;
import com.mintstack.finance.service.external.TefasFundClient.TefasFundPrice;
import com.mintstack.finance.service.external.YahooFinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataBackfillService {

    private static final int DEFAULT_DAYS = 30;
    private static final int DEFAULT_MAX_INSTRUMENTS = 100;
    private static final int MAX_DAYS = 365;

    private final InstrumentRepository instrumentRepository;
    private final MarketDataService marketDataService;
    private final YahooFinanceClient yahooFinanceClient;
    private final TefasFundClient tefasFundClient;
    private final TcmbApiClient tcmbApiClient;
    private final BistDataStoreMarketDataService bistDataStoreMarketDataService;

    public HistoricalDataBackfillResponse backfill(HistoricalDataBackfillRequest request) {
        BackfillWindow window = resolveWindow(request);
        Set<InstrumentType> requestedTypes = resolveTypes(request);
        Set<String> requestedSymbols = normalizeSymbols(request.getSymbols());
        int maxInstruments = resolveMaxInstruments(request.getMaxInstruments());

        BackfillStats stats = new BackfillStats(window.startDate(), window.endDate(), window.days());

        if (requestedTypes.contains(InstrumentType.CURRENCY)) {
            backfillCurrencies(window, stats, requestedSymbols);
        }

        for (InstrumentType type : requestedTypes) {
            if (type == InstrumentType.CURRENCY) {
                continue;
            }
            List<Instrument> instruments = resolveInstruments(type, requestedSymbols, maxInstruments);
            if (type == InstrumentType.FUND) {
                stats.processedInstruments += instruments.size();
                int savedRows = backfillFunds(instruments, window, stats);
                stats.rowsByType.merge(type.name(), savedRows, Integer::sum);
                continue;
            }
            if (type == InstrumentType.BOND) {
                stats.processedInstruments += instruments.size();
                int savedRows = bistDataStoreMarketDataService.backfillBondPrices(
                    window.startDate(),
                    window.endDate(),
                    requestedSymbols,
                    maxInstruments
                );
                stats.rowsByType.merge(type.name(), savedRows, Integer::sum);
                if (savedRows == 0) {
                    stats.warnings.add("BIST DataStore tahvil/bono tarihsel veri donmedi.");
                }
                continue;
            }
            if (type == InstrumentType.VIOP) {
                stats.processedInstruments += instruments.size();
                int savedRows = bistDataStoreMarketDataService.backfillViopPrices(
                    window.startDate(),
                    window.endDate(),
                    requestedSymbols,
                    maxInstruments
                );
                stats.rowsByType.merge(type.name(), savedRows, Integer::sum);
                if (savedRows == 0) {
                    stats.warnings.add("BIST DataStore VIOP tarihsel veri donmedi.");
                }
                continue;
            }
            for (Instrument instrument : instruments) {
                stats.processedInstruments++;
                int savedRows = backfillInstrument(instrument, window, stats);
                stats.rowsByType.merge(type.name(), savedRows, Integer::sum);
                if (savedRows == 0) {
                    stats.skippedInstruments++;
                }
            }
        }

        return stats.toResponse();
    }

    private int backfillInstrument(
            Instrument instrument,
            BackfillWindow window,
            BackfillStats stats) {
        try {
            if (instrument.getType() == InstrumentType.STOCK || instrument.getType() == InstrumentType.INDEX) {
                List<PriceHistory> history = yahooFinanceClient.fetchHistoricalData(
                    instrument.getSymbol(),
                    window.startDate(),
                    window.endDate().plusDays(1),
                    null,
                    null
                );
                return saveHistory(history);
            }

            stats.warnings.add(instrument.getSymbol() + " icin tarihsel kaynak desteklenmiyor.");
            return 0;
        } catch (Exception error) {
            log.warn("Historical backfill failed for {}: {}", instrument.getSymbol(), error.getMessage());
            stats.warnings.add(instrument.getSymbol() + " atlandi: " + error.getMessage());
            return 0;
        }
    }

    private int backfillFunds(List<Instrument> instruments, BackfillWindow window, BackfillStats stats) {
        if (instruments.isEmpty()) {
            return 0;
        }

        Map<String, Instrument> selectedFunds = instruments.stream()
            .collect(Collectors.toMap(
                instrument -> normalizeSymbol(instrument.getSymbol()),
                instrument -> instrument,
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Map<String, Integer> savedBySymbol = new LinkedHashMap<>();
        int saved = 0;

        for (LocalDate date = window.startDate(); !date.isAfter(window.endDate()); date = date.plusDays(1)) {
            if (isWeekend(date)) {
                continue;
            }

            List<TefasFundPrice> prices = tefasFundClient.fetchFundPrices(date);
            for (TefasFundPrice price : prices) {
                Instrument instrument = selectedFunds.get(normalizeSymbol(price.fundCode()));
                if (instrument == null) {
                    continue;
                }
                try {
                    upsertFundPrice(instrument, price);
                    saved++;
                    savedBySymbol.merge(instrument.getSymbol(), 1, Integer::sum);
                } catch (OptimisticLockingFailureException | DataIntegrityViolationException error) {
                    String warning = instrument.getSymbol() + " " + date
                        + " fiyat satiri es zamanli guncelleme nedeniyle atlandi.";
                    stats.warnings.add(warning);
                    log.warn("{} {}", warning, error.getMessage());
                }
            }
        }

        for (Instrument instrument : instruments) {
            if (!savedBySymbol.containsKey(instrument.getSymbol())) {
                stats.skippedInstruments++;
                stats.warnings.add(instrument.getSymbol() + " icin TEFAS tarihsel veri bulunamadi.");
            }
        }
        return saved;
    }

    private void backfillCurrencies(BackfillWindow window, BackfillStats stats, Set<String> requestedSymbols) {
        int saved = 0;
        Set<String> processedCodes = new LinkedHashSet<>();
        for (LocalDate date = window.startDate(); !date.isAfter(window.endDate()); date = date.plusDays(1)) {
            if (isWeekend(date)) {
                continue;
            }

            try {
                LocalDate queryDate = date;
                List<CurrencyRate> rates = tcmbApiClient.fetchRates(queryDate).stream()
                    .filter(rate -> rate.getRateDate() != null && rate.getRateDate().toLocalDate().equals(queryDate))
                    .filter(rate -> isCurrencyRequested(rate, requestedSymbols))
                    .toList();
                if (rates.isEmpty()) {
                    stats.warnings.add("TCMB " + date + " icin veri donmedi.");
                    continue;
                }
                rates.forEach(rate -> processedCodes.add(normalizeSymbol(rate.getCurrencyCode())));
                marketDataService.saveCurrencyRates(rates);
                saved += rates.size();
            } catch (Exception error) {
                log.warn("TCMB backfill failed for {}: {}", date, error.getMessage());
                stats.warnings.add("TCMB " + date + " atlandi: " + error.getMessage());
            }
        }
        stats.processedInstruments += processedCodes.size();
        stats.savedCurrencyRows += saved;
        stats.rowsByType.merge(InstrumentType.CURRENCY.name(), saved, Integer::sum);
    }

    private boolean isCurrencyRequested(CurrencyRate rate, Set<String> requestedSymbols) {
        if (requestedSymbols.isEmpty()) {
            return true;
        }

        String code = normalizeSymbol(rate.getCurrencyCode());
        return requestedSymbols.contains(code)
            || requestedSymbols.contains(code + "TRY")
            || requestedSymbols.contains(code + "/TRY");
    }

    private int saveHistory(List<PriceHistory> history) {
        if (history == null || history.isEmpty()) {
            return 0;
        }
        int saved = 0;
        for (PriceHistory item : history) {
            if (item.getInstrument() == null || item.getPriceDate() == null || item.getClosePrice() == null) {
                continue;
            }
            if (savePriceHistoryWithRetry(item)) {
                saved++;
            }
        }
        return saved;
    }

    private boolean savePriceHistoryWithRetry(PriceHistory item) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                marketDataService.savePriceHistory(item);
                return true;
            } catch (OptimisticLockingFailureException error) {
                lastFailure = error;
                log.debug("Retrying historical row save for {} on {} after optimistic lock conflict",
                    item.getInstrument().getSymbol(), item.getPriceDate());
            } catch (DataIntegrityViolationException error) {
                lastFailure = error;
                log.warn("Skipping historical row for {} on {} because of data conflict: {}",
                    item.getInstrument().getSymbol(), item.getPriceDate(), error.getMessage());
                return false;
            }
        }

        log.warn("Skipping historical row for {} on {} after concurrent update conflict: {}",
            item.getInstrument().getSymbol(),
            item.getPriceDate(),
            lastFailure != null ? lastFailure.getMessage() : "unknown");
        return false;
    }

    private List<Instrument> resolveInstruments(InstrumentType type, Set<String> requestedSymbols, int maxInstruments) {
        List<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulatedOrderBySymbolAsc(type, false);
        if (!requestedSymbols.isEmpty()) {
            instruments = instruments.stream()
                .filter(instrument -> requestedSymbols.contains(normalizeSymbol(instrument.getSymbol())))
                .toList();
        }
        return instruments.stream()
            .limit(maxInstruments)
            .toList();
    }

    private Set<InstrumentType> resolveTypes(HistoricalDataBackfillRequest request) {
        if (request.getInstrumentTypes() == null || request.getInstrumentTypes().isEmpty()) {
            return Set.of(InstrumentType.STOCK, InstrumentType.FUND, InstrumentType.CURRENCY);
        }
        return request.getInstrumentTypes().stream()
            .filter(type -> type != null && type != InstrumentType.CRYPTO)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizeSymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Set.of();
        }
        return symbols.stream()
            .map(this::normalizeSymbol)
            .filter(symbol -> !symbol.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeSymbol(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
    }

    private BackfillWindow resolveWindow(HistoricalDataBackfillRequest request) {
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();
        LocalDate startDate = request.getStartDate();
        int requestedDays = request.getDays() != null ? Math.min(request.getDays(), MAX_DAYS) : DEFAULT_DAYS;
        if (startDate == null) {
            startDate = endDate.minusDays(requestedDays - 1L);
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Baslangic tarihi bitis tarihinden sonra olamaz");
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1L;
        if (days > MAX_DAYS) {
            throw new IllegalArgumentException("Tarih araligi en fazla " + MAX_DAYS + " gun olabilir");
        }
        return new BackfillWindow(startDate, endDate, (int) days);
    }

    private int resolveMaxInstruments(Integer maxInstruments) {
        if (maxInstruments == null) {
            return DEFAULT_MAX_INSTRUMENTS;
        }
        return Math.max(1, Math.min(500, maxInstruments));
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private void upsertFundPrice(Instrument instrument, TefasFundPrice fundPrice) {
        instrument.setName(fundPrice.fundName());
        instrument.setType(InstrumentType.FUND);
        instrument.setExchange("TEFAS");
        instrument.setCurrency("TRY");
        instrument.setIsActive(true);
        instrument.setIsSimulated(false);
        instrument.setPreviousClose(instrument.getCurrentPrice());
        instrument.setCurrentPrice(fundPrice.price());
        Instrument saved = instrumentRepository.save(instrument);

        PriceHistory history = PriceHistory.builder()
            .instrument(saved)
            .priceDate(fundPrice.date())
            .openPrice(firstPositive(fundPrice.exchangeBulletinPrice(), fundPrice.price()))
            .highPrice(fundPrice.price())
            .lowPrice(fundPrice.price())
            .closePrice(fundPrice.price())
            .adjustedClose(fundPrice.price())
            .volume(toLong(fundPrice.sharesOutstanding()))
            .build();
        marketDataService.savePriceHistory(history);
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null && value.signum() > 0) {
                return value;
            }
        }
        return BigDecimal.ONE;
    }

    private Long toLong(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal(Long.MAX_VALUE)) > 0) {
            return null;
        }
        return value.longValue();
    }

    private record BackfillWindow(LocalDate startDate, LocalDate endDate, int days) {
    }

    private static class BackfillStats {
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final int days;
        private int processedInstruments;
        private int savedCurrencyRows;
        private int skippedInstruments;
        private final List<String> warnings = new ArrayList<>();
        private final Map<String, Integer> rowsByType = new LinkedHashMap<>();

        BackfillStats(LocalDate startDate, LocalDate endDate, int days) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.days = days;
        }

        HistoricalDataBackfillResponse toResponse() {
            int savedPriceRows = rowsByType.entrySet().stream()
                .filter(entry -> !InstrumentType.CURRENCY.name().equals(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();
            return new HistoricalDataBackfillResponse(
                startDate,
                endDate,
                days,
                processedInstruments,
                savedPriceRows,
                savedCurrencyRows,
                skippedInstruments,
                List.copyOf(warnings),
                Map.copyOf(rowsByType)
            );
        }
    }
}
