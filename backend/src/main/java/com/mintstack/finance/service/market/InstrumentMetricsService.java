package com.mintstack.finance.service.market;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.service.external.YahooFinanceClient;
import com.mintstack.finance.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class InstrumentMetricsService {

    private static final Pattern VIOP_MATURITY_PATTERN = Pattern.compile("(\\d{4})$");
    private static final Pattern BOND_MATURITY_PATTERN = Pattern.compile("^TRT(\\d{2})(\\d{2})(\\d{2})T\\d{2}$");
    private static final int RECENT_HISTORY_LIMIT = 10;

    private final PriceHistoryRepository priceHistoryRepository;
    private final YahooFinanceClient yahooFinanceClient;

    public InstrumentMetrics resolveMetrics(Instrument instrument) {
        List<PriceHistory> recentHistory = resolveRecentHistory(instrument, RECENT_HISTORY_LIMIT);
        Optional<PriceHistory> latestHistory = recentHistory.stream().findFirst();
        BigDecimal currentPrice = resolveCurrentPrice(instrument, latestHistory);
        BigDecimal previousClose = resolvePreviousClose(instrument, recentHistory, currentPrice);
        Long volume = resolveLatestVolume(instrument, latestHistory);
        LocalDate maturityDate = resolveMaturityDate(instrument);
        BigDecimal yieldRate = resolveYieldRate(instrument, maturityDate, currentPrice);
        BigDecimal totalValue = resolveTotalValue(instrument, currentPrice, volume);
        BigDecimal marketCap = resolveMarketCap(instrument, totalValue, currentPrice, previousClose);
        PriceRange week52Range = resolveWeek52Range(instrument);
        BigDecimal openPrice = resolveOpenPrice(instrument, latestHistory);
        BigDecimal highPrice = resolveHighPrice(instrument, latestHistory);
        BigDecimal lowPrice = resolveLowPrice(instrument, latestHistory);

        return new InstrumentMetrics(
            currentPrice,
            previousClose,
            openPrice,
            highPrice,
            lowPrice,
            volume,
            yieldRate,
            totalValue,
            marketCap,
            week52Range.high(),
            week52Range.low(),
            maturityDate
        );
    }

    private List<PriceHistory> resolveRecentHistory(Instrument instrument, int limit) {
        if (instrument == null || instrument.getId() == null) {
            return List.of();
        }
        List<PriceHistory> history = priceHistoryRepository.findByInstrumentIdOrderByPriceDateDesc(
            instrument.getId(),
            PageRequest.of(0, limit)
        );
        return history != null ? history : List.of();
    }

    private BigDecimal resolveCurrentPrice(Instrument instrument, Optional<PriceHistory> latestHistory) {
        if (shouldResolvePriceFromHistory(instrument)
            && latestHistory.isPresent()
            && firstPositive(latestHistory.get().getClosePrice()) != null) {
            return latestHistory.get().getClosePrice();
        }
        return firstPositive(
            instrument != null ? instrument.getCurrentPrice() : null,
            latestHistory.map(PriceHistory::getClosePrice).orElse(null)
        );
    }

    private BigDecimal resolvePreviousClose(Instrument instrument, List<PriceHistory> recentHistory, BigDecimal currentPrice) {
        if (shouldResolvePriceFromHistory(instrument) && recentHistory != null && recentHistory.size() > 1) {
            BigDecimal previousHistoryClose = recentHistory.stream()
                .skip(1)
                .map(PriceHistory::getClosePrice)
                .map(value -> firstPositive(value))
                .filter(java.util.Objects::nonNull)
                .filter(close -> currentPrice == null || close.compareTo(currentPrice) != 0)
                .findFirst()
                .orElseGet(() -> firstPositive(recentHistory.get(1).getClosePrice()));
            if (previousHistoryClose != null) {
                return previousHistoryClose;
            }
        }
        return firstPositive(
            instrument != null ? instrument.getPreviousClose() : null,
            recentHistory != null && !recentHistory.isEmpty() ? recentHistory.get(0).getOpenPrice() : null
        );
    }

    private boolean shouldResolvePriceFromHistory(Instrument instrument) {
        if (instrument == null || instrument.getType() == null) {
            return false;
        }
        return instrument.getType() == InstrumentType.STOCK
            || instrument.getType() == InstrumentType.BOND
            || instrument.getType() == InstrumentType.FUND
            || instrument.getType() == InstrumentType.VIOP;
    }

    private BigDecimal resolveOpenPrice(Instrument instrument, Optional<PriceHistory> latestHistory) {
        if (latestHistory.isPresent() && latestHistory.get().getOpenPrice() != null) {
            return latestHistory.get().getOpenPrice();
        }
        return firstPositive(instrument.getPreviousClose(), instrument.getCurrentPrice());
    }

    private BigDecimal resolveHighPrice(Instrument instrument, Optional<PriceHistory> latestHistory) {
        if (latestHistory.isPresent() && latestHistory.get().getHighPrice() != null) {
            return latestHistory.get().getHighPrice();
        }
        return firstPositive(instrument.getCurrentPrice(), instrument.getPreviousClose());
    }

    private BigDecimal resolveLowPrice(Instrument instrument, Optional<PriceHistory> latestHistory) {
        if (latestHistory.isPresent() && latestHistory.get().getLowPrice() != null) {
            return latestHistory.get().getLowPrice();
        }
        return firstPositive(instrument.getCurrentPrice(), instrument.getPreviousClose());
    }

    private Long resolveLatestVolume(Instrument instrument, Optional<PriceHistory> latestHistory) {
        if (instrument == null) {
            return null;
        }

        if (latestHistory != null && latestHistory.isPresent() && latestHistory.get().getVolume() != null) {
            return latestHistory.get().getVolume();
        }

        if (instrument.getSymbol() != null && shouldQueryExternalVolume(instrument.getType())) {
            Long liveVolume = yahooFinanceClient.getLatestVolume(instrument.getSymbol());
            if (liveVolume != null && liveVolume > 0) {
                return liveVolume;
            }
        }
        return null;
    }

    private LocalDate resolveMaturityDate(Instrument instrument) {
        if (instrument == null || instrument.getSymbol() == null || instrument.getType() == null) {
            return null;
        }

        if (instrument.getType() == InstrumentType.BOND) {
            return resolveBondMaturityDate(instrument.getSymbol());
        }
        if (instrument.getType() != InstrumentType.VIOP) {
            return null;
        }

        Matcher matcher = VIOP_MATURITY_PATTERN.matcher(instrument.getSymbol());
        if (!matcher.find()) {
            return null;
        }

        String value = matcher.group(1);
        int month = Integer.parseInt(value.substring(0, 2));
        int year = 2000 + Integer.parseInt(value.substring(2, 4));

        if (month < 1 || month > 12) {
            return null;
        }

        return YearMonth.of(year, month).atEndOfMonth();
    }

    private LocalDate resolveBondMaturityDate(String symbol) {
        if (symbol == null) {
            return null;
        }

        Matcher matcher = BOND_MATURITY_PATTERN.matcher(symbol);
        if (!matcher.find()) {
            return null;
        }

        int day = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int year = 2000 + Integer.parseInt(matcher.group(3));

        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private BigDecimal resolveYieldRate(Instrument instrument, LocalDate maturityDate, BigDecimal currentPrice) {
        if (instrument == null || instrument.getType() != InstrumentType.BOND) {
            return null;
        }

        BigDecimal price = currentPrice;
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0 || maturityDate == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        if (!maturityDate.isAfter(today)) {
            return BigDecimal.ZERO;
        }

        long daysToMaturity = ChronoUnit.DAYS.between(today, maturityDate);
        if (daysToMaturity <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal years = new BigDecimal(daysToMaturity)
            .divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP);
        BigDecimal parValue = new BigDecimal("100");

        return parValue.subtract(price)
            .divide(price, 10, RoundingMode.HALF_UP)
            .divide(years, 10, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveTotalValue(Instrument instrument, BigDecimal currentPrice, Long volume) {
        if (currentPrice == null) {
            return null;
        }

        if (volume == null || volume <= 0) {
            return null;
        }

        return currentPrice
            .multiply(BigDecimal.valueOf(volume))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveMarketCap(Instrument instrument, BigDecimal totalValue,
                                        BigDecimal currentPrice, BigDecimal previousClose) {
        return null;
    }

    private PriceRange resolveWeek52Range(Instrument instrument) {
        if (instrument == null || instrument.getType() != InstrumentType.STOCK || instrument.getSymbol() == null) {
            return new PriceRange(null, null);
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(365);
        List<PriceHistory> history = priceHistoryRepository.findBySymbolAndDateRange(instrument.getSymbol(), startDate, endDate);

        BigDecimal high = history.stream()
            .map(price -> firstPositive(price.getHighPrice(), price.getClosePrice(), price.getOpenPrice()))
            .filter(java.util.Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(null);
        BigDecimal low = history.stream()
            .map(price -> firstPositive(price.getLowPrice(), price.getClosePrice(), price.getOpenPrice()))
            .filter(java.util.Objects::nonNull)
            .min(BigDecimal::compareTo)
            .orElse(null);

        if (high == null || low == null) {
            return new PriceRange(null, null);
        }
        if (low.compareTo(high) > 0) {
            BigDecimal temp = low;
            low = high;
            high = temp;
        }
        return new PriceRange(
            high.setScale(6, RoundingMode.HALF_UP),
            low.setScale(6, RoundingMode.HALF_UP)
        );
    }

    private boolean shouldQueryExternalVolume(InstrumentType type) {
        if (type == null) {
            return false;
        }
        return type == InstrumentType.STOCK || type == InstrumentType.INDEX || type == InstrumentType.CRYPTO;
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        if (values == null) {
            return null;
        }
        for (BigDecimal value : values) {
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                return value;
            }
        }
        return null;
    }

    public record InstrumentMetrics(
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        Long volume,
        BigDecimal yieldRate,
        BigDecimal totalValue,
        BigDecimal marketCap,
        BigDecimal week52High,
        BigDecimal week52Low,
        LocalDate maturityDate
    ) {
    }

    private record PriceRange(BigDecimal high, BigDecimal low) {
    }
}
