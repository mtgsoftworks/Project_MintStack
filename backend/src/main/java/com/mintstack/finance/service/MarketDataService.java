package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.CurrencyRateResponse;
import com.mintstack.finance.dto.response.InstrumentResponse;
import com.mintstack.finance.dto.response.PriceHistoryResponse;
import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.CurrencyRate.RateSource;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.NewsRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.service.simulation.SimulatedIndex;
import com.mintstack.finance.service.simulation.SimulatedStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@io.micrometer.observation.annotation.Observed(name = "market.data.service", contextualName = "market-data-operations")
public class MarketDataService {

    private static final Pattern VIOP_MATURITY_PATTERN = Pattern.compile("(\\d{4})$");
    private static final Pattern BOND_MATURITY_PATTERN = Pattern.compile("^TRT(\\d{2})(\\d{2})(\\d{2})T\\d{2}$");

    private final InstrumentRepository instrumentRepository;
    private final CurrencyRateRepository currencyRateRepository;

    private final PriceHistoryRepository priceHistoryRepository;
    private final NewsRepository newsRepository;
    private final UserApiConfigRepository userApiConfigRepository;
    private final com.mintstack.finance.service.external.YahooFinanceClient yahooFinanceClient;
    private final com.mintstack.finance.service.simulation.SimulationDataService simulationDataService;

    // Currency Rates
    @Cacheable(value = "currencyRates", key = "'latest-' + @simulationDataService.isSimulationEnabled()")
    @Transactional(readOnly = true)
    public List<CurrencyRateResponse> getLatestCurrencyRates() {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        RateSource source = isSimulation ? RateSource.MANUAL : RateSource.TCMB;
        List<CurrencyRate> rates = currencyRateRepository.findLatestBySource(source);
        return rates.stream()
            .map(this::mapToRateResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CurrencyRateResponse getCurrencyRate(String currencyCode) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        RateSource source = isSimulation ? RateSource.MANUAL : RateSource.TCMB;
        
        CurrencyRate rate = currencyRateRepository
            .findTopByCurrencyCodeAndSourceOrderByFetchedAtDesc(currencyCode, source)
            .orElseThrow(() -> new ResourceNotFoundException("Kur", "kod", currencyCode));
        return mapToRateResponse(rate);
    }

    @Transactional(readOnly = true)
    public List<CurrencyRateResponse> getCurrencyHistory(String currencyCode, 
                                                          LocalDate startDate, 
                                                          LocalDate endDate) {
        List<CurrencyRate> rates = currencyRateRepository.findHistoryByCurrencyCode(
            currencyCode,
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59)
        );
        return rates.stream()
            .map(this::mapToRateResponse)
            .collect(Collectors.toList());
    }

    // Instruments
    @Cacheable(value = "instruments", key = "#type.name() + '-' + @simulationDataService.isSimulationEnabled()")
    @Transactional(readOnly = true)
    public List<InstrumentResponse> getInstrumentsByType(InstrumentType type) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        List<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(type, isSimulation);

        if (shouldFallbackToRealInstruments(type, isSimulation, instruments.isEmpty())) {
            instruments = instrumentRepository.findByTypeAndIsActiveTrue(type);
        }

        return instruments.stream()
            .map(this::mapToInstrumentResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> getInstrumentsByType(InstrumentType type, Pageable pageable) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        Page<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(type, isSimulation, pageable);

        if (shouldFallbackToRealInstruments(type, isSimulation, instruments.isEmpty())) {
            instruments = instrumentRepository.findByTypeAndIsActiveTrue(type, pageable);
        }

        return instruments.map(this::mapToInstrumentResponse);
    }

    @Transactional(readOnly = true)
    public InstrumentResponse getInstrument(String symbol) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        Instrument instrument = instrumentRepository.findBySymbolAndIsSimulated(symbol, isSimulation)
            .orElseGet(() -> instrumentRepository.findBySymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Enstrüman", "sembol", symbol)));
        return mapToInstrumentResponse(instrument);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> searchInstruments(String query, Pageable pageable) {
        Page<Instrument> instruments = instrumentRepository.searchBySymbolOrName(query, pageable);
        return instruments.map(this::mapToInstrumentResponse);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> searchInstruments(InstrumentType type, String query, Pageable pageable) {
        Page<Instrument> instruments = instrumentRepository.searchByTypeAndQuery(type, query, pageable);
        return instruments.map(this::mapToInstrumentResponse);
    }

    // Price History
    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(String symbol, LocalDate startDate, LocalDate endDate) {
        List<PriceHistory> history = priceHistoryRepository.findBySymbolAndDateRange(symbol, startDate, endDate);
        return history.stream()
            .map(this::mapToPriceHistoryResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getRecentPriceHistory(String symbol, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return getPriceHistory(symbol, startDate, endDate);
    }

    // Config
    @Transactional(readOnly = true)
    public UserApiConfig getActiveYahooConfig() {
        return userApiConfigRepository.findByProviderAndIsActiveTrue(ApiProvider.YAHOO_FINANCE)
            .stream()
            .findFirst()
            .orElse(null);
    }

    // Market Index (e.g. BIST 100)
    public InstrumentResponse getMarketIndex(String symbol) {
        if (simulationDataService.isSimulationEnabled()) {
            InstrumentResponse simulatedIndex = getSimulatedIndexResponse(symbol);
            if (simulatedIndex != null) {
                return simulatedIndex;
            }
        }

        // Try to find in DB first
        Instrument instrument = instrumentRepository.findBySymbol(symbol).orElse(null);
        if (instrument != null && !Boolean.TRUE.equals(instrument.getIsActive())) {
            instrument = null;
        }
        
        // If instrument exists and has recent price, return it
        if (instrument != null && instrument.getCurrentPrice() != null) {
            return mapToInstrumentResponse(instrument);
        }

        // Try to fetch from Yahoo Finance if configured
        UserApiConfig config = getActiveYahooConfig();
        if (config != null) {
            try {
                String apiKey = config.getApiKey();
                String baseUrl = config.getBaseUrl();
                
                BigDecimal price = yahooFinanceClient.fetchStockPrice(symbol, apiKey, baseUrl);
                
                // Create temporary instrument response if not in DB
                InstrumentResponse.InstrumentResponseBuilder builder = InstrumentResponse.builder()
                    .symbol(symbol)
                    .name(symbol) // We don't have name if not in DB
                    .currentPrice(price)
                    .type(InstrumentType.INDEX)
                    .currency("TRY");
                    
                if (instrument != null) {
                    builder.id(instrument.getId())
                           .name(instrument.getName())
                           .previousClose(instrument.getPreviousClose());
                }
                
                return builder.build();
                
            } catch (Exception e) {
                log.warn("Failed to fetch market index {} from Yahoo: {}", symbol, e.getMessage());
            }
        }

        // Return existing instrument data only when it has a price.
        if (instrument != null && instrument.getCurrentPrice() != null) {
            return mapToInstrumentResponse(instrument);
        }

        log.info("No market index data available for {}", symbol);
        throw new ResourceNotFoundException("Endeks", "sembol", symbol);
    }

    private boolean shouldFallbackToRealInstruments(InstrumentType type, boolean simulationEnabled, boolean isEmpty) {
        if (!simulationEnabled || !isEmpty) {
            return false;
        }
        return type == InstrumentType.BOND || type == InstrumentType.FUND || type == InstrumentType.VIOP;
    }

    private InstrumentResponse getSimulatedIndexResponse(String symbol) {
        SimulatedIndex index = simulationDataService.getIndex(symbol);
        String normalizedSymbol = normalizeIndexSymbol(symbol);

        if (index == null && !normalizedSymbol.equals(symbol)) {
            index = simulationDataService.getIndex(normalizedSymbol);
        }
        if (index != null) {
            Instrument instrument = Instrument.builder()
                .symbol(symbol)
                .name(index.getName())
                .type(InstrumentType.INDEX)
                .exchange("BIST")
                .currency("TRY")
                .currentPrice(index.getCurrentValue())
                .previousClose(index.getPreviousClose())
                .isActive(true)
                .isSimulated(true)
                .build();

            return mapToInstrumentResponse(instrument);
        }

        SimulatedStock stockBasedIndex = simulationDataService.getStock(normalizedSymbol);
        if (stockBasedIndex == null) {
            return null;
        }

        Instrument instrument = Instrument.builder()
            .symbol(symbol)
            .name(stockBasedIndex.getName())
            .type(InstrumentType.INDEX)
            .exchange("BIST")
            .currency("TRY")
            .currentPrice(stockBasedIndex.getCurrentPrice())
            .previousClose(stockBasedIndex.getPreviousClose())
            .isActive(true)
            .isSimulated(true)
            .build();

        return mapToInstrumentResponse(instrument);
    }

    private String normalizeIndexSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        if (symbol.endsWith(".IS")) {
            return symbol.substring(0, symbol.length() - 3);
        }
        return symbol;
    }

    // Save methods
    @Transactional
    @CacheEvict(value = "currencyRates", allEntries = true)
    public void saveCurrencyRates(List<CurrencyRate> rates) {
        currencyRateRepository.saveAll(rates);
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
            if (priceHistory.getOpenPrice() != null) {
                current.setOpenPrice(priceHistory.getOpenPrice());
            }
            if (priceHistory.getHighPrice() != null) {
                current.setHighPrice(priceHistory.getHighPrice());
            }
            if (priceHistory.getLowPrice() != null) {
                current.setLowPrice(priceHistory.getLowPrice());
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

    // Mapping methods
    private CurrencyRateResponse mapToRateResponse(CurrencyRate rate) {
        BigDecimal changePercent = calculateChangePercent(rate);
        
        return CurrencyRateResponse.builder()
            .id(rate.getId())
            .currencyCode(rate.getCurrencyCode())
            .currencyName(rate.getCurrencyName())
            .buyingRate(rate.getBuyingRate())
            .sellingRate(rate.getSellingRate())
            .effectiveBuyingRate(rate.getEffectiveBuyingRate())
            .effectiveSellingRate(rate.getEffectiveSellingRate())
            .averageRate(rate.getAverageRate())
            .changePercent(changePercent)
            .source(rate.getSource().name())
            .fetchedAt(rate.getFetchedAt())
            .rateDate(rate.getRateDate())
            .build();
    }
    
    private BigDecimal calculateChangePercent(CurrencyRate currentRate) {
        try {
            return currencyRateRepository.findPreviousRate(
                currentRate.getCurrencyCode(), 
                currentRate.getFetchedAt()
            ).map(previousRate -> {
                BigDecimal current = currentRate.getSellingRate();
                BigDecimal previous = previousRate.getSellingRate();
                if (previous.compareTo(BigDecimal.ZERO) == 0) {
                    return BigDecimal.ZERO;
                }
                return current.subtract(previous)
                    .divide(previous, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            }).orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            log.warn("Error calculating change percent for {}: {}", 
                currentRate.getCurrencyCode(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private InstrumentResponse mapToInstrumentResponse(Instrument instrument) {
        BigDecimal change = null;
        BigDecimal changePercent = null;
        
        if (instrument.getCurrentPrice() != null && instrument.getPreviousClose() != null) {
            change = instrument.getCurrentPrice().subtract(instrument.getPreviousClose());
            changePercent = instrument.getChangePercent();
        }

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
        
        return InstrumentResponse.builder()
            .id(instrument.getId())
            .symbol(instrument.getSymbol())
            .name(instrument.getName())
            .type(instrument.getType())
            .exchange(instrument.getExchange())
            .currency(instrument.getCurrency())
            .currentPrice(instrument.getCurrentPrice())
            .previousClose(instrument.getPreviousClose())
            .change(change)
            .changePercent(changePercent)
            .openPrice(openPrice)
            .highPrice(highPrice)
            .lowPrice(lowPrice)
            .volume(volume)
            .yieldRate(yieldRate)
            .totalValue(totalValue)
            .marketCap(marketCap)
            .week52High(week52Range.high())
            .week52Low(week52Range.low())
            .maturityDate(maturityDate)
            .isActive(instrument.getIsActive())
            .build();
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

    private PriceHistoryResponse mapToPriceHistoryResponse(PriceHistory history) {
        return PriceHistoryResponse.builder()
            .date(history.getPriceDate())
            .open(history.getOpenPrice())
            .high(history.getHighPrice())
            .low(history.getLowPrice())
            .close(history.getClosePrice())
            .adjustedClose(history.getAdjustedClose())
            .volume(history.getVolume())
            .build();
    }

    private record PriceRange(BigDecimal high, BigDecimal low) {
    }

    private record InstrumentDeactivationSummary(long total, long indices) {
    }
}
