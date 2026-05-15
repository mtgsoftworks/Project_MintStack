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
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.repository.UserDataPreferenceRepository;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.service.market.InstrumentMetricsService;
import com.mintstack.finance.service.market.MarketDataMaintenanceService;
import com.mintstack.finance.service.simulation.SimulatedIndex;
import com.mintstack.finance.service.simulation.SimulatedStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@io.micrometer.observation.annotation.Observed(name = "market.data.service", contextualName = "market-data-operations")
public class MarketDataService {

    private final InstrumentRepository instrumentRepository;
    private final CurrencyRateRepository currencyRateRepository;

    private final PriceHistoryRepository priceHistoryRepository;
    private final UserApiConfigRepository userApiConfigRepository;
    private final UserDataPreferenceRepository userDataPreferenceRepository;
    private final com.mintstack.finance.service.external.YahooFinanceClient yahooFinanceClient;
    private final com.mintstack.finance.service.simulation.SimulationDataService simulationDataService;
    private final MarketDataMaintenanceService marketDataMaintenanceService;
    private final InstrumentMetricsService instrumentMetricsService;

    // Currency Rates
    @Cacheable(value = "currencyRates", key = "'latest-' + @simulationDataService.isSimulationEnabled()")
    @Transactional(readOnly = true)
    public List<CurrencyRateResponse> getLatestCurrencyRates() {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        RateSource preferredSource = resolveCurrencyRateSource(isSimulation);
        List<CurrencyRate> rates = currencyRateRepository.findLatestBySource(preferredSource);
        if (rates.isEmpty() && preferredSource != RateSource.TCMB) {
            rates = currencyRateRepository.findLatestBySource(RateSource.TCMB);
        }
        return rates.stream()
            .map(this::mapToRateResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CurrencyRateResponse getCurrencyRate(String currencyCode) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        RateSource source = resolveCurrencyRateSource(isSimulation);

        CurrencyRate rate = currencyRateRepository
            .findTopByCurrencyCodeAndSourceOrderByFetchedAtDesc(currencyCode, source)
            .orElseGet(() -> resolveFallbackCurrencyRate(currencyCode, source));

        if (rate == null) {
            throw new ResourceNotFoundException("Kur", "kod", currencyCode);
        }
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

    private RateSource resolveCurrencyRateSource(boolean simulationEnabled) {
        if (simulationEnabled) {
            return RateSource.MANUAL;
        }
        return userDataPreferenceRepository
            .findFirstByDataTypeAndIsEnabledTrueOrderByUpdatedAtDesc(DataType.CURRENCY_RATES)
            .map(preference -> mapProviderToRateSource(preference.getProvider()))
            .orElse(RateSource.TCMB);
    }

    private CurrencyRate resolveFallbackCurrencyRate(String currencyCode, RateSource preferredSource) {
        if (preferredSource != RateSource.TCMB) {
            CurrencyRate fallbackTcmb = currencyRateRepository
                .findTopByCurrencyCodeAndSourceOrderByFetchedAtDesc(currencyCode, RateSource.TCMB)
                .orElse(null);
            if (fallbackTcmb != null) {
                return fallbackTcmb;
            }
        }
        return currencyRateRepository.findTopByCurrencyCodeOrderByFetchedAtDesc(currencyCode).orElse(null);
    }

    private RateSource mapProviderToRateSource(ApiProvider provider) {
        if (provider == null) {
            return RateSource.TCMB;
        }
        return switch (provider) {
            case TCMB -> RateSource.TCMB;
            case YAHOO_FINANCE -> RateSource.YAHOO_FINANCE;
            case ALPHA_VANTAGE -> RateSource.ALPHA_VANTAGE;
            case FINNHUB -> RateSource.FINNHUB;
            default -> RateSource.TCMB;
        };
    }

    // Instruments
    @Cacheable(value = "instruments", key = "#type.name() + '-' + @simulationDataService.isSimulationEnabled()")
    @Transactional(readOnly = true)
    public List<InstrumentResponse> getInstrumentsByType(InstrumentType type) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        List<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(type, isSimulation);

        if (isSimulation && instruments.isEmpty()) {
            List<InstrumentResponse> simulated = getSimulatedInstrumentsFromCache(type);
            if (!simulated.isEmpty()) {
                return simulated;
            }
        }

        if (shouldFallbackToRealInstruments(type, isSimulation, instruments.isEmpty())) {
            instruments = instrumentRepository.findByTypeAndIsActiveTrue(type);
        }

        if (shouldFallbackToInactiveCatalog(type, isSimulation, instruments.isEmpty())) {
            instruments = instrumentRepository.findRealByType(type);
        }

        return instruments.stream()
            .map(this::mapToInstrumentResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> getInstrumentsByType(InstrumentType type, Pageable pageable) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        Page<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulated(type, isSimulation, pageable);

        if (isSimulation && instruments.isEmpty()) {
            List<InstrumentResponse> simulated = getSimulatedInstrumentsFromCache(type);
            if (!simulated.isEmpty()) {
                return paginateResponses(simulated, pageable);
            }
        }

        if (shouldFallbackToRealInstruments(type, isSimulation, instruments.isEmpty())) {
            instruments = instrumentRepository.findByTypeAndIsActiveTrue(type, pageable);
        }

        if (shouldFallbackToInactiveCatalog(type, isSimulation, instruments.isEmpty())) {
            instruments = instrumentRepository.findRealByType(type, pageable);
        }

        return instruments.map(this::mapToInstrumentResponse);
    }

    @Transactional(readOnly = true)
    public InstrumentResponse getInstrument(String symbol) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        String normalizedSymbol = symbol != null ? symbol.toUpperCase() : "";
        Instrument instrument = instrumentRepository.findBySymbolAndIsSimulated(normalizedSymbol, isSimulation)
            .orElseGet(() -> instrumentRepository.findBySymbol(normalizedSymbol).orElse(null));

        if (instrument != null) {
            return mapToInstrumentResponse(instrument);
        }

        if (isSimulation) {
            Instrument cachedInstrument = resolveSimulatedInstrumentBySymbol(normalizedSymbol);
            if (cachedInstrument != null) {
                return mapToInstrumentResponse(cachedInstrument);
            }
        }

        throw new ResourceNotFoundException("Enstrüman", "sembol", symbol);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> searchInstruments(String query, Pageable pageable) {
        Page<Instrument> instruments = instrumentRepository.searchBySymbolOrName(query, pageable);
        return instruments.map(this::mapToInstrumentResponse);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> searchInstruments(InstrumentType type, String query, Pageable pageable) {
        Page<Instrument> instruments = instrumentRepository.searchByTypeAndQuery(type, query, pageable);

        if (simulationDataService.isSimulationEnabled() && instruments.isEmpty()) {
            List<InstrumentResponse> filtered = getSimulatedInstrumentsFromCache(type).stream()
                .filter(item -> matchesInstrumentQuery(item, query))
                .toList();
            if (!filtered.isEmpty()) {
                return paginateResponses(filtered, pageable);
            }
        }

        if (shouldFallbackToInactiveCatalog(type, simulationDataService.isSimulationEnabled(), instruments.isEmpty())) {
            instruments = instrumentRepository.searchRealByTypeAndQuery(type, query, pageable);
        }

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
        List<String> symbolCandidates = resolveIndexSymbolCandidates(symbol);

        if (simulationDataService.isSimulationEnabled()) {
            InstrumentResponse simulatedIndex = getSimulatedIndexResponse(symbol);
            if (simulatedIndex != null) {
                return simulatedIndex;
            }
        }

        // Try DB with alias fallback first (e.g. XU100.IS <-> XU100)
        Instrument instrument = findFirstActiveIndexInstrument(symbolCandidates);
        
        // If instrument exists and has recent price, return it
        if (instrument != null && instrument.getCurrentPrice() != null) {
            return mapToInstrumentResponse(instrument);
        }

        // Try Yahoo Finance direct fallback even without explicit API config.
        UserApiConfig config = getActiveYahooConfig();
        String apiKey = config != null ? config.getApiKey() : null;
        String baseUrl = config != null ? config.getBaseUrl() : null;
        BigDecimal price = null;

        for (String candidate : symbolCandidates) {
            try {
                price = yahooFinanceClient.fetchStockPrice(candidate, apiKey, baseUrl);
                break;
            } catch (Exception e) {
                log.debug("Failed to fetch market index {} via candidate {}: {}", symbol, candidate, e.getMessage());
            }
        }

        if (price != null) {
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
        }

        // Return existing instrument data only when it has a price.
        if (instrument != null && instrument.getCurrentPrice() != null) {
            return mapToInstrumentResponse(instrument);
        }

        log.info("No market index data available for {}. Candidates checked: {}", symbol, symbolCandidates);
        throw new ResourceNotFoundException("Endeks", "sembol", symbol);
    }

    private boolean shouldFallbackToRealInstruments(InstrumentType type, boolean simulationEnabled, boolean isEmpty) {
        if (!simulationEnabled || !isEmpty) {
            return false;
        }
        return type == InstrumentType.BOND || type == InstrumentType.FUND || type == InstrumentType.VIOP;
    }

    private boolean shouldFallbackToInactiveCatalog(InstrumentType type, boolean simulationEnabled, boolean isEmpty) {
        if (simulationEnabled || !isEmpty) {
            return false;
        }
        return type == InstrumentType.BOND
            || type == InstrumentType.VIOP
            || type == InstrumentType.INDEX;
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

    private String toDotIsSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "";
        }
        if (symbol.endsWith(".IS")) {
            return symbol;
        }
        return symbol + ".IS";
    }

    private List<String> resolveIndexSymbolCandidates(String symbol) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (symbol != null && !symbol.isBlank()) {
            candidates.add(symbol);
        }

        String normalized = normalizeIndexSymbol(symbol);
        if (!normalized.isBlank()) {
            candidates.add(normalized);
        }

        String dotIs = toDotIsSymbol(symbol);
        if (!dotIs.isBlank()) {
            candidates.add(dotIs);
        }

        return new ArrayList<>(candidates);
    }

    private Instrument findFirstActiveIndexInstrument(List<String> symbolCandidates) {
        for (String candidate : symbolCandidates) {
            Instrument instrument = instrumentRepository.findBySymbol(candidate).orElse(null);
            if (instrument != null && Boolean.TRUE.equals(instrument.getIsActive())) {
                return instrument;
            }
        }
        return null;
    }

    private List<InstrumentResponse> getSimulatedInstrumentsFromCache(InstrumentType type) {
        return resolveSimulationCache(type).entrySet().stream()
            .map(entry -> mapToInstrumentResponse(buildSimulatedInstrument(type, entry.getKey(), entry.getValue())))
            .sorted(Comparator.comparing(InstrumentResponse::getSymbol))
            .collect(Collectors.toList());
    }

    private Map<String, SimulatedStock> resolveSimulationCache(InstrumentType type) {
        if (type == null) {
            return Map.of();
        }
        return switch (type) {
            case STOCK -> safeSimulationMap(simulationDataService.getStocks());
            case BOND -> safeSimulationMap(simulationDataService.getBonds());
            case FUND -> safeSimulationMap(simulationDataService.getFunds());
            case VIOP -> safeSimulationMap(simulationDataService.getViop());
            default -> Map.of();
        };
    }

    private Map<String, SimulatedStock> safeSimulationMap(Map<String, SimulatedStock> source) {
        return source != null ? source : Map.of();
    }

    private Instrument resolveSimulatedInstrumentBySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }

        SimulatedStock stock = simulationDataService.getStock(symbol);
        if (stock != null) {
            return buildSimulatedInstrument(InstrumentType.STOCK, symbol, stock);
        }

        SimulatedStock bond = simulationDataService.getBond(symbol);
        if (bond != null) {
            return buildSimulatedInstrument(InstrumentType.BOND, symbol, bond);
        }

        SimulatedStock fund = simulationDataService.getFund(symbol);
        if (fund != null) {
            return buildSimulatedInstrument(InstrumentType.FUND, symbol, fund);
        }

        SimulatedStock viop = simulationDataService.getViopContract(symbol);
        if (viop != null) {
            return buildSimulatedInstrument(InstrumentType.VIOP, symbol, viop);
        }

        for (InstrumentType type : List.of(InstrumentType.STOCK, InstrumentType.BOND, InstrumentType.FUND, InstrumentType.VIOP)) {
            Map<String, SimulatedStock> cache = resolveSimulationCache(type);
            SimulatedStock simulated = cache.get(symbol);
            if (simulated != null) {
                return buildSimulatedInstrument(type, symbol, simulated);
            }
        }
        return null;
    }

    private Instrument buildSimulatedInstrument(InstrumentType type, String symbol, SimulatedStock simulatedStock) {
        return Instrument.builder()
            .symbol(symbol)
            .name(simulatedStock.getName())
            .type(type)
            .exchange(simulatedStock.getExchange())
            .currency("TRY")
            .currentPrice(simulatedStock.getCurrentPrice())
            .previousClose(simulatedStock.getPreviousClose())
            .isActive(true)
            .isSimulated(true)
            .build();
    }

    private Page<InstrumentResponse> paginateResponses(List<InstrumentResponse> responses, Pageable pageable) {
        int total = responses.size();
        int start = (int) pageable.getOffset();
        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        int end = Math.min(start + pageable.getPageSize(), total);
        return new PageImpl<>(responses.subList(start, end), pageable, total);
    }

    private boolean matchesInstrumentQuery(InstrumentResponse instrument, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedQuery = query.toLowerCase();
        String symbol = instrument.getSymbol() != null ? instrument.getSymbol().toLowerCase() : "";
        String name = instrument.getName() != null ? instrument.getName().toLowerCase() : "";
        return symbol.contains(normalizedQuery) || name.contains(normalizedQuery);
    }

    // Save methods
    @Transactional
    @CacheEvict(value = "currencyRates", allEntries = true)
    public void saveCurrencyRates(List<CurrencyRate> rates) {
        marketDataMaintenanceService.saveCurrencyRates(rates);
    }

    @Transactional
    public void updateInstrumentPrice(String symbol, BigDecimal price) {
        marketDataMaintenanceService.updateInstrumentPrice(symbol, price);
    }

    @Transactional
    public void savePriceHistory(PriceHistory priceHistory) {
        marketDataMaintenanceService.savePriceHistory(priceHistory);
    }

    @Transactional
    public Map<String, Object> deleteAllMarketData() {
        return marketDataMaintenanceService.deleteAllMarketData();
    }

    // Mapping methods
    private CurrencyRateResponse mapToRateResponse(CurrencyRate rate) {
        BigDecimal changePercent = calculateChangePercent(rate);
        BigDecimal buyingRate = positiveOrFallback(rate.getBuyingRate(), rate.getSellingRate());
        BigDecimal sellingRate = positiveOrFallback(rate.getSellingRate(), buyingRate);
        BigDecimal effectiveBuyingRate = positiveOrFallback(rate.getEffectiveBuyingRate(), buyingRate);
        BigDecimal effectiveSellingRate = positiveOrFallback(rate.getEffectiveSellingRate(), sellingRate);
        
        return CurrencyRateResponse.builder()
            .id(rate.getId())
            .currencyCode(rate.getCurrencyCode())
            .currencyName(rate.getCurrencyName())
            .buyingRate(buyingRate)
            .sellingRate(sellingRate)
            .effectiveBuyingRate(effectiveBuyingRate)
            .effectiveSellingRate(effectiveSellingRate)
            .averageRate(buyingRate.add(sellingRate).divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP))
            .changePercent(changePercent)
            .source(rate.getSource().name())
            .fetchedAt(rate.getFetchedAt())
            .rateDate(rate.getRateDate())
            .build();
    }
    
    private BigDecimal calculateChangePercent(CurrencyRate currentRate) {
        try {
            if (currentRate.getRateDate() == null) {
                return BigDecimal.ZERO;
            }
            List<CurrencyRate> previousRates = currencyRateRepository
                .findPreviousRatesByRateDate(
                    currentRate.getCurrencyCode(),
                    currentRate.getSource(),
                    currentRate.getRateDate(),
                    BigDecimal.ZERO,
                    PageRequest.of(0, 1)
                );
            if (previousRates.isEmpty()) {
                return BigDecimal.ZERO;
            }

            CurrencyRate previousRate = previousRates.get(0);
            BigDecimal current = positiveOrFallback(currentRate.getSellingRate(), currentRate.getBuyingRate());
            BigDecimal previous = positiveOrFallback(previousRate.getSellingRate(), previousRate.getBuyingRate());
            if (!isPositive(current) || !isPositive(previous)) {
                return BigDecimal.ZERO;
            }
            return current.subtract(previous)
                .divide(previous, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        } catch (Exception e) {
            log.warn("Error calculating change percent for {}: {}", 
                currentRate.getCurrencyCode(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal positiveOrFallback(BigDecimal value, BigDecimal fallback) {
        if (isPositive(value)) {
            return value;
        }
        if (isPositive(fallback)) {
            return fallback;
        }
        return BigDecimal.ZERO;
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private InstrumentResponse mapToInstrumentResponse(Instrument instrument) {
        BigDecimal change = null;
        BigDecimal changePercent = null;
        
        if (instrument.getCurrentPrice() != null && instrument.getPreviousClose() != null) {
            change = instrument.getCurrentPrice().subtract(instrument.getPreviousClose());
            changePercent = instrument.getChangePercent();
        }
        InstrumentMetricsService.InstrumentMetrics metrics = instrumentMetricsService.resolveMetrics(instrument);

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
            .openPrice(metrics.openPrice())
            .highPrice(metrics.highPrice())
            .lowPrice(metrics.lowPrice())
            .volume(metrics.volume())
            .yieldRate(metrics.yieldRate())
            .totalValue(metrics.totalValue())
            .marketCap(metrics.marketCap())
            .week52High(metrics.week52High())
            .week52Low(metrics.week52Low())
            .maturityDate(metrics.maturityDate())
            .isActive(instrument.getIsActive())
            .isSimulated(Boolean.TRUE.equals(instrument.getIsSimulated()))
            .build();
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
}





