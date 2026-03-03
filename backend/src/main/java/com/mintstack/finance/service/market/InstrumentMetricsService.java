package com.mintstack.finance.service.market;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.service.external.YahooFinanceClient;
import com.mintstack.finance.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
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

    private final PriceHistoryRepository priceHistoryRepository;
    private final YahooFinanceClient yahooFinanceClient;

    public InstrumentMetrics resolveMetrics(Instrument instrument) {
        Optional<PriceHistory> latestHistory = resolveLatestHistory(instrument);
        Long volume = resolveLatestVolume(instrument);
        LocalDate maturityDate = resolveMaturityDate(instrument);
        BigDecimal yieldRate = resolveYieldRate(instrument, maturityDate);
        BigDecimal totalValue = resolveTotalValue(instrument, volume);
        BigDecimal marketCap = resolveMarketCap(instrument, totalValue);
        PriceRange week52Range = resolveWeek52Range(instrument);
        BigDecimal openPrice = resolveOpenPrice(instrument, latestHistory);
        BigDecimal highPrice = resolveHighPrice(instrument, latestHistory);
        BigDecimal lowPrice = resolveLowPrice(instrument, latestHistory);

        return new InstrumentMetrics(
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

    private Optional<PriceHistory> resolveLatestHistory(Instrument instrument) {
        if (instrument == null || instrument.getId() == null) {
            return Optional.empty();
        }
        Optional<PriceHistory> latest = priceHistoryRepository.findTopByInstrumentIdOrderByPriceDateDesc(instrument.getId());
        return latest != null ? latest : Optional.empty();
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

    private Long resolveLatestVolume(Instrument instrument) {
        if (instrument == null) {
            return null;
        }

        if (instrument.getId() != null) {
            Optional<PriceHistory> latestHistory = priceHistoryRepository.findTopByInstrumentIdOrderByPriceDateDesc(instrument.getId());
            if (latestHistory != null && latestHistory.isPresent() && latestHistory.get().getVolume() != null) {
                return latestHistory.get().getVolume();
            }
        }

        if (instrument.getSymbol() != null && shouldQueryExternalVolume(instrument.getType())) {
            Long liveVolume = yahooFinanceClient.getLatestVolume(instrument.getSymbol());
            if (liveVolume != null && liveVolume > 0) {
                return liveVolume;
            }
        }
        return resolveSyntheticVolume(instrument);
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

    private BigDecimal resolveYieldRate(Instrument instrument, LocalDate maturityDate) {
        if (instrument == null || instrument.getType() != InstrumentType.BOND) {
            return null;
        }

        BigDecimal price = instrument.getCurrentPrice();
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

    private BigDecimal resolveTotalValue(Instrument instrument, Long volume) {
        if (instrument == null || instrument.getCurrentPrice() == null) {
            return null;
        }

        Long effectiveVolume = volume;
        if (effectiveVolume == null || effectiveVolume <= 0) {
            effectiveVolume = resolveSyntheticVolume(instrument);
        }
        if (effectiveVolume == null || effectiveVolume <= 0) {
            return null;
        }

        return instrument.getCurrentPrice()
            .multiply(BigDecimal.valueOf(effectiveVolume))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveMarketCap(Instrument instrument, BigDecimal totalValue) {
        if (instrument == null || instrument.getType() != InstrumentType.STOCK) {
            return null;
        }
        if (totalValue != null && totalValue.compareTo(BigDecimal.ZERO) > 0) {
            return totalValue.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal base = firstPositive(instrument.getCurrentPrice(), instrument.getPreviousClose());
        if (base == null) {
            return null;
        }
        return base.multiply(new BigDecimal("2500000")).setScale(2, RoundingMode.HALF_UP);
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

        BigDecimal reference = firstPositive(instrument.getCurrentPrice(), instrument.getPreviousClose());
        if (high == null && reference != null) {
            high = reference.multiply(new BigDecimal("1.15"));
        }
        if (low == null && reference != null) {
            low = reference.multiply(new BigDecimal("0.85"));
        }
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

    private Long resolveSyntheticVolume(Instrument instrument) {
        if (instrument == null || instrument.getType() == null) {
            return null;
        }

        if (instrument.getType() == InstrumentType.STOCK) {
            String symbol = instrument.getSymbol();
            if (symbol == null) {
                return 2_500_000L;
            }
            return switch (symbol) {
                case "THYAO" -> 12_000_000L;
                case "GARAN" -> 9_500_000L;
                case "AKBNK" -> 8_200_000L;
                case "SISE" -> 7_400_000L;
                case "ASELS" -> 6_800_000L;
                default -> 2_500_000L;
            };
        }

        if (instrument.getType() == InstrumentType.FUND) {
            String symbol = instrument.getSymbol();
            if (symbol == null) {
                return 500_000L;
            }
            return switch (symbol) {
                case "MAC" -> 1_250_000L;
                case "TCD" -> 980_000L;
                case "TI2" -> 2_400_000L;
                case "AFT" -> 760_000L;
                case "GSP" -> 420_000L;
                default -> 500_000L;
            };
        }

        if (instrument.getType() == InstrumentType.BOND) {
            return 150_000L;
        }

        return null;
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
