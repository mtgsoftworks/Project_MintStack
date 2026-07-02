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
import com.mintstack.finance.service.simulation.SimulatedCurrency;
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
import java.util.UUID;
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
    @Transactional(readOnly = true)
    public List<CurrencyRateResponse> getLatestCurrencyRates() {
        return getLatestCurrencyRates(null, null);
    }

    @Cacheable(value = "currencyRates", key = "'latest-' + @simulationDataService.isSimulationEnabled() + '-' + #changeStartDate + '-' + #changeEndDate")
    @Transactional(readOnly = true)
    public List<CurrencyRateResponse> getLatestCurrencyRates(LocalDate changeStartDate, LocalDate changeEndDate) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        RateSource preferredSource = resolveCurrencyRateSource(isSimulation);
        ChangeDateRange changeRange = normalizeChangeRange(changeStartDate, changeEndDate);
        List<CurrencyRate> rates = currencyRateRepository.findLatestBySource(preferredSource);
        if (isSimulation && rates.isEmpty()) {
            return simulationDataService.getCurrencies().entrySet().stream()
                .map(entry -> mapToSimulatedRateResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        }
        if (rates.isEmpty() && preferredSource != RateSource.TCMB) {
            rates = currencyRateRepository.findLatestBySource(RateSource.TCMB);
        }
        return rates.stream()
            .map(rate -> mapToRateResponse(rate, changeRange))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CurrencyRateResponse getCurrencyRate(String currencyCode) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        RateSource source = resolveCurrencyRateSource(isSimulation);
        String normalizedCode = currencyCode != null ? currencyCode.toUpperCase() : "";

        CurrencyRate rate = currencyRateRepository
            .findTopByCurrencyCodeAndSourceOrderByFetchedAtDesc(normalizedCode, source)
            .orElseGet(() -> resolveFallbackCurrencyRate(normalizedCode, source));

        if (rate == null) {
            if (isSimulation) {
                SimulatedCurrency simulated = simulationDataService.getCurrency(normalizedCode);
                if (simulated != null) {
                    return mapToSimulatedRateResponse(normalizedCode, simulated);
                }
            }
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
        if (preferredSource == RateSource.MANUAL && simulationDataService.isSimulationEnabled()) {
            return null;
        }
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

    private CurrencyRateResponse mapToSimulatedRateResponse(String code, SimulatedCurrency currency) {
        return CurrencyRateResponse.builder()
            .currencyCode(code)
            .currencyName(currency.getName())
            .buyingRate(currency.getBuyingRate())
            .sellingRate(currency.getSellingRate())
            .effectiveBuyingRate(currency.getBuyingRate())
            .effectiveSellingRate(currency.getSellingRate())
            .averageRate(currency.getMidRate())
            .changePercent(BigDecimal.ZERO)
            .source(RateSource.MANUAL.name())
            .fetchedAt(java.time.LocalDateTime.now())
            .rateDate(java.time.LocalDateTime.now())
            .build();
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
        return getInstrumentsByType(type, null, null);
    }

    @Transactional(readOnly = true)
    public List<InstrumentResponse> getInstrumentsByType(InstrumentType type, LocalDate changeStartDate, LocalDate changeEndDate) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        ChangeDateRange changeRange = normalizeChangeRange(changeStartDate, changeEndDate);
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

        return mapInstrumentResponses(instruments, changeRange);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> getInstrumentsByType(InstrumentType type, Pageable pageable) {
        return getInstrumentsByType(type, pageable, null, null);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> getInstrumentsByType(
            InstrumentType type,
            Pageable pageable,
            LocalDate changeStartDate,
            LocalDate changeEndDate) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        ChangeDateRange changeRange = normalizeChangeRange(changeStartDate, changeEndDate);
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

        return mapInstrumentResponses(instruments, changeRange);
    }

    @Transactional(readOnly = true)
    public InstrumentResponse getInstrument(String symbol) {
        return getInstrument(symbol, null, null);
    }

    @Transactional(readOnly = true)
    public InstrumentResponse getInstrument(String symbol, LocalDate changeStartDate, LocalDate changeEndDate) {
        ChangeDateRange changeRange = normalizeChangeRange(changeStartDate, changeEndDate);
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        String normalizedSymbol = symbol != null ? symbol.toUpperCase() : "";
        Instrument instrument = instrumentRepository.findBySymbolAndIsSimulated(normalizedSymbol, isSimulation)
            .orElseGet(() -> instrumentRepository.findBySymbol(normalizedSymbol).orElse(null));

        if (instrument != null) {
            return mapToInstrumentResponse(instrument, changeRange);
        }

        if (isSimulation) {
            Instrument cachedInstrument = resolveSimulatedInstrumentBySymbol(normalizedSymbol);
            if (cachedInstrument != null) {
                return mapToInstrumentResponse(cachedInstrument, changeRange);
            }
        }

        throw new ResourceNotFoundException("Enstrüman", "sembol", symbol);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> searchInstruments(String query, Pageable pageable) {
        return searchInstruments(query, pageable, null, null);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> searchInstruments(
            String query,
            Pageable pageable,
            LocalDate changeStartDate,
            LocalDate changeEndDate) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        ChangeDateRange changeRange = normalizeChangeRange(changeStartDate, changeEndDate);
        Page<Instrument> instruments = isSimulation
            ? instrumentRepository.searchBySymbolOrNameAndSimulationMode(query, true, pageable)
            : instrumentRepository.searchBySymbolOrName(query, pageable);

        if (isSimulation && instruments.isEmpty()) {
            List<InstrumentResponse> filtered = List.of(
                    InstrumentType.STOCK,
                    InstrumentType.BOND,
                    InstrumentType.FUND,
                    InstrumentType.VIOP
                ).stream()
                .flatMap(type -> getSimulatedInstrumentsFromCache(type).stream())
                .filter(item -> matchesInstrumentQuery(item, query))
                .sorted(Comparator.comparing(InstrumentResponse::getSymbol))
                .toList();
            if (!filtered.isEmpty()) {
                return paginateResponses(filtered, pageable);
            }
        }

        return mapInstrumentResponses(instruments, changeRange);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> searchInstruments(InstrumentType type, String query, Pageable pageable) {
        return searchInstruments(type, query, pageable, null, null);
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> searchInstruments(
            InstrumentType type,
            String query,
            Pageable pageable,
            LocalDate changeStartDate,
            LocalDate changeEndDate) {
        boolean isSimulation = simulationDataService.isSimulationEnabled();
        ChangeDateRange changeRange = normalizeChangeRange(changeStartDate, changeEndDate);
        Page<Instrument> instruments = isSimulation
            ? instrumentRepository.searchByTypeAndQueryAndSimulationMode(type, query, true, pageable)
            : instrumentRepository.searchByTypeAndQuery(type, query, pageable);

        if (isSimulation && instruments.isEmpty()) {
            List<InstrumentResponse> filtered = getSimulatedInstrumentsFromCache(type).stream()
                .filter(item -> matchesInstrumentQuery(item, query))
                .toList();
            if (!filtered.isEmpty()) {
                return paginateResponses(filtered, pageable);
            }
        }

        if (shouldFallbackToInactiveCatalog(type, isSimulation, instruments.isEmpty())) {
            instruments = instrumentRepository.searchRealByTypeAndQuery(type, query, pageable);
        }

        return mapInstrumentResponses(instruments, changeRange);
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
        return getMarketIndex(symbol, null, null);
    }

    public InstrumentResponse getMarketIndex(String symbol, LocalDate changeStartDate, LocalDate changeEndDate) {
        ChangeDateRange changeRange = normalizeChangeRange(changeStartDate, changeEndDate);
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
            return mapToInstrumentResponse(instrument, changeRange);
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
            return mapToInstrumentResponse(instrument, changeRange);
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
        return mapToRateResponse(rate, null);
    }

    private CurrencyRateResponse mapToRateResponse(CurrencyRate rate, ChangeDateRange changeRange) {
        CurrencyRateChange rateChange = changeRange != null
            ? calculateCurrencyRangeChange(rate, changeRange)
            : calculateCurrencyDefaultChange(rate);
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
            .changePercent(rateChange.changePercent())
            .changeBaseRate(rateChange.baseRate())
            .changeStartAt(rateChange.startAt())
            .changeEndAt(rateChange.endAt())
            .source(rate.getSource().name())
            .fetchedAt(rate.getFetchedAt())
            .rateDate(rate.getRateDate())
            .build();
    }
    
    private CurrencyRateChange calculateCurrencyDefaultChange(CurrencyRate currentRate) {
        try {
            if (currentRate.getRateDate() == null) {
                return new CurrencyRateChange(BigDecimal.ZERO, null, null, null);
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
                return new CurrencyRateChange(BigDecimal.ZERO, null, null, null);
            }

            CurrencyRate previousRate = previousRates.get(0);
            BigDecimal current = positiveOrFallback(currentRate.getSellingRate(), currentRate.getBuyingRate());
            BigDecimal previous = positiveOrFallback(previousRate.getSellingRate(), previousRate.getBuyingRate());
            if (!isPositive(current) || !isPositive(previous)) {
                return new CurrencyRateChange(BigDecimal.ZERO, previous, previousRate.getFetchedAt(), currentRate.getFetchedAt());
            }
            BigDecimal changePercent = current.subtract(previous)
                .divide(previous, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            return new CurrencyRateChange(changePercent, previous, previousRate.getFetchedAt(), currentRate.getFetchedAt());
        } catch (Exception e) {
            log.warn("Error calculating change percent for {}: {}", 
                currentRate.getCurrencyCode(), e.getMessage());
            return new CurrencyRateChange(BigDecimal.ZERO, null, null, null);
        }
    }

    private CurrencyRateChange calculateCurrencyRangeChange(CurrencyRate currentRate, ChangeDateRange changeRange) {
        if (currentRate == null || changeRange == null || currentRate.getSource() == null) {
            return new CurrencyRateChange(null, null, null, null);
        }

        CurrencyRate startRate = findCurrencyRateAtOrBefore(
            currentRate.getCurrencyCode(),
            currentRate.getSource(),
            changeRange.startDate().atStartOfDay()
        );
        CurrencyRate endRate = changeRange.endDate().isBefore(LocalDate.now())
            ? findCurrencyRateAtOrBefore(
                currentRate.getCurrencyCode(),
                currentRate.getSource(),
                changeRange.endDate().atTime(23, 59, 59)
            )
            : currentRate;

        if (startRate == null || endRate == null) {
            return new CurrencyRateChange(null, null, null, null);
        }

        BigDecimal start = positiveOrFallback(startRate.getSellingRate(), startRate.getBuyingRate());
        BigDecimal end = positiveOrFallback(endRate.getSellingRate(), endRate.getBuyingRate());
        if (!isPositive(start) || !isPositive(end)) {
            return new CurrencyRateChange(null, start, startRate.getFetchedAt(), endRate.getFetchedAt());
        }

        BigDecimal changePercent = end.subtract(start)
            .divide(start, 6, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        return new CurrencyRateChange(changePercent, start, startRate.getFetchedAt(), endRate.getFetchedAt());
    }

    private CurrencyRate findCurrencyRateAtOrBefore(String currencyCode, RateSource source, java.time.LocalDateTime at) {
        List<CurrencyRate> rates = currencyRateRepository.findLatestAtOrBefore(
            currencyCode,
            source,
            at,
            BigDecimal.ZERO,
            PageRequest.of(0, 1)
        );
        return rates.isEmpty() ? null : rates.get(0);
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
        return mapToInstrumentResponse(instrument, null);
    }

    private InstrumentResponse mapToInstrumentResponse(Instrument instrument, ChangeDateRange changeRange) {
        return mapToInstrumentResponse(
                instrument,
                changeRange,
                instrumentMetricsService.resolveMetrics(instrument)
        );
    }

    private List<InstrumentResponse> mapInstrumentResponses(
            List<Instrument> instruments,
            ChangeDateRange changeRange) {
        Map<UUID, InstrumentMetricsService.InstrumentMetrics> metricsByInstrument =
                instrumentMetricsService.resolveMetricsBatch(instruments);
        Map<UUID, InstrumentRangeChange> rangeChanges =
                resolveBatchRangeChanges(instruments, metricsByInstrument, changeRange);
        return instruments.stream()
                .map(instrument -> mapToInstrumentResponse(
                        instrument,
                        changeRange,
                        resolveBatchMetric(instrument, metricsByInstrument),
                        rangeChanges.get(instrument.getId())
                ))
                .toList();
    }

    private Page<InstrumentResponse> mapInstrumentResponses(
            Page<Instrument> instruments,
            ChangeDateRange changeRange) {
        Map<UUID, InstrumentMetricsService.InstrumentMetrics> metricsByInstrument =
                instrumentMetricsService.resolveMetricsBatch(instruments.getContent());
        Map<UUID, InstrumentRangeChange> rangeChanges =
                resolveBatchRangeChanges(instruments.getContent(), metricsByInstrument, changeRange);
        return instruments.map(instrument -> mapToInstrumentResponse(
                instrument,
                changeRange,
                resolveBatchMetric(instrument, metricsByInstrument),
                rangeChanges.get(instrument.getId())
        ));
    }

    private InstrumentMetricsService.InstrumentMetrics resolveBatchMetric(
            Instrument instrument,
            Map<UUID, InstrumentMetricsService.InstrumentMetrics> metricsByInstrument) {
        InstrumentMetricsService.InstrumentMetrics metrics = metricsByInstrument.get(instrument.getId());
        return metrics != null ? metrics : instrumentMetricsService.resolveMetrics(instrument);
    }

    private InstrumentResponse mapToInstrumentResponse(
            Instrument instrument,
            ChangeDateRange changeRange,
            InstrumentMetricsService.InstrumentMetrics metrics) {
        return mapToInstrumentResponse(instrument, changeRange, metrics, null);
    }

    private InstrumentResponse mapToInstrumentResponse(
            Instrument instrument,
            ChangeDateRange changeRange,
            InstrumentMetricsService.InstrumentMetrics metrics,
            InstrumentRangeChange precomputedRangeChange) {
        BigDecimal currentPrice = metrics.currentPrice();
        BigDecimal previousClose = metrics.previousClose();
        BigDecimal change = null;
        BigDecimal changePercent = null;
        BigDecimal changeBasePrice = previousClose;
        LocalDate changeStartDate = null;
        LocalDate changeEndDate = null;

        if (currentPrice != null && previousClose != null) {
            change = currentPrice.subtract(previousClose);
            if (previousClose.compareTo(BigDecimal.ZERO) != 0) {
                changePercent = change
                    .divide(previousClose, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            } else {
                changePercent = BigDecimal.ZERO;
            }
        }

        if (changeRange != null) {
            InstrumentRangeChange rangeChange = precomputedRangeChange != null
                    ? precomputedRangeChange
                    : calculateInstrumentRangeChange(
                            instrument,
                            currentPrice,
                            positiveOrFallback(instrument.getPreviousClose(), metrics.previousClose()),
                            changeRange
                    );
            change = rangeChange.change();
            changePercent = rangeChange.changePercent();
            changeBasePrice = rangeChange.basePrice();
            changeStartDate = rangeChange.startDate();
            changeEndDate = rangeChange.endDate();
        }

        return InstrumentResponse.builder()
            .id(instrument.getId())
            .symbol(instrument.getSymbol())
            .name(instrument.getName())
            .type(instrument.getType())
            .exchange(instrument.getExchange())
            .currency(instrument.getCurrency())
            .currentPrice(currentPrice)
            .previousClose(previousClose)
            .change(change)
            .changePercent(changePercent)
            .changeBasePrice(changeBasePrice)
            .changeStartDate(changeStartDate)
            .changeEndDate(changeEndDate)
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

    private Map<UUID, InstrumentRangeChange> resolveBatchRangeChanges(
            List<Instrument> instruments,
            Map<UUID, InstrumentMetricsService.InstrumentMetrics> metricsByInstrument,
            ChangeDateRange changeRange) {
        if (changeRange == null || instruments.isEmpty()) {
            return Map.of();
        }

        List<UUID> instrumentIds = instruments.stream()
                .map(Instrument::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (instrumentIds.isEmpty()) {
            return Map.of();
        }

        LocalDate today = LocalDate.now();
        Map<UUID, PricePoint> startPoints = new java.util.HashMap<>();
        if (changeRange.startDate().isEqual(today)) {
            instruments.forEach(instrument -> {
                InstrumentMetricsService.InstrumentMetrics metrics =
                        metricsByInstrument.get(instrument.getId());
                BigDecimal basePrice = positiveOrFallback(
                        instrument.getPreviousClose(),
                        metrics != null ? metrics.previousClose() : null
                );
                if (isPositive(basePrice)) {
                    startPoints.put(instrument.getId(), new PricePoint(basePrice, today));
                }
            });
        } else {
            priceHistoryRepository.findLatestAtOrBeforeByInstrumentIds(
                            instrumentIds,
                            changeRange.startDate()
                    )
                    .forEach(history -> startPoints.put(
                            history.getInstrument().getId(),
                            toPricePoint(history)
                    ));
        }

        Map<UUID, PricePoint> endPoints = new java.util.HashMap<>();
        if (changeRange.endDate().isBefore(today)) {
            priceHistoryRepository.findLatestAtOrBeforeByInstrumentIds(
                            instrumentIds,
                            changeRange.endDate()
                    )
                    .forEach(history -> endPoints.put(
                            history.getInstrument().getId(),
                            toPricePoint(history)
                    ));
        } else {
            instruments.forEach(instrument -> {
                InstrumentMetricsService.InstrumentMetrics metrics =
                        metricsByInstrument.get(instrument.getId());
                if (metrics != null && metrics.currentPrice() != null) {
                    endPoints.put(
                            instrument.getId(),
                            new PricePoint(metrics.currentPrice(), today)
                    );
                }
            });
        }

        Map<UUID, InstrumentRangeChange> result = new java.util.HashMap<>();
        instrumentIds.forEach(instrumentId -> result.put(
                instrumentId,
                calculateRangeChange(startPoints.get(instrumentId), endPoints.get(instrumentId))
        ));
        return result;
    }

    private PricePoint toPricePoint(PriceHistory history) {
        return new PricePoint(
                positiveOrFallback(history.getAdjustedClose(), history.getClosePrice()),
                history.getPriceDate()
        );
    }

    private InstrumentRangeChange calculateRangeChange(PricePoint startPoint, PricePoint endPoint) {
        if (startPoint == null || endPoint == null) {
            return InstrumentRangeChange.empty();
        }
        if (!isPositive(startPoint.price()) || endPoint.price() == null) {
            return new InstrumentRangeChange(
                    null,
                    null,
                    startPoint.price(),
                    startPoint.date(),
                    endPoint.date()
            );
        }

        BigDecimal change = endPoint.price().subtract(startPoint.price());
        BigDecimal changePercent = change
                .divide(startPoint.price(), 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        return new InstrumentRangeChange(
                change,
                changePercent,
                startPoint.price(),
                startPoint.date(),
                endPoint.date()
        );
    }

    private InstrumentRangeChange calculateInstrumentRangeChange(
            Instrument instrument,
            BigDecimal currentPrice,
            BigDecimal openingBasePrice,
            ChangeDateRange changeRange) {
        if (instrument == null || instrument.getId() == null || changeRange == null) {
            return InstrumentRangeChange.empty();
        }

        LocalDate today = LocalDate.now();
        PricePoint startPoint = changeRange.startDate().isEqual(today)
            ? findOpeningPricePoint(instrument.getId(), today, openingBasePrice)
            : findPricePointAtOrBefore(instrument.getId(), changeRange.startDate());
        PricePoint endPoint = changeRange.endDate().isBefore(today)
            ? findPricePointAtOrBefore(instrument.getId(), changeRange.endDate())
            : new PricePoint(currentPrice, today);

        if (startPoint == null || endPoint == null) {
            return InstrumentRangeChange.empty();
        }
        if (!isPositive(startPoint.price()) || endPoint.price() == null) {
            return new InstrumentRangeChange(null, null, startPoint.price(), startPoint.date(), endPoint.date());
        }

        BigDecimal change = endPoint.price().subtract(startPoint.price());
        BigDecimal changePercent = change
            .divide(startPoint.price(), 6, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        return new InstrumentRangeChange(
            change,
            changePercent,
            startPoint.price(),
            startPoint.date(),
            endPoint.date()
        );
    }

    private PricePoint findOpeningPricePoint(UUID instrumentId, LocalDate date, BigDecimal fallbackOpenPrice) {
        if (instrumentId == null || date == null) {
            return null;
        }

        PricePoint previousClosePoint = findPricePointAtOrBefore(instrumentId, date.minusDays(1));
        PriceHistory todayHistory = priceHistoryRepository.findByInstrumentIdAndPriceDate(instrumentId, date).orElse(null);
        BigDecimal historyOpen = todayHistory != null
            ? positiveOrFallback(todayHistory.getOpenPrice(), todayHistory.getClosePrice())
            : null;
        if (isPositive(historyOpen) && !isSinglePriceHistory(todayHistory)) {
            return new PricePoint(historyOpen, date);
        }

        if (isSinglePriceHistory(todayHistory)) {
            PricePoint latestSessionOpen = findLatestSessionOpenAtOrBefore(instrumentId, date.minusDays(1));
            if (latestSessionOpen != null && isPositive(latestSessionOpen.price())) {
                return latestSessionOpen;
            }
        }

        if (previousClosePoint != null && isPositive(previousClosePoint.price())) {
            return new PricePoint(previousClosePoint.price(), date);
        }

        if (isPositive(fallbackOpenPrice)) {
            return new PricePoint(fallbackOpenPrice, date);
        }

        if (isPositive(historyOpen)) {
            return new PricePoint(historyOpen, date);
        }

        return null;
    }

    private PricePoint findLatestSessionOpenAtOrBefore(UUID instrumentId, LocalDate date) {
        if (instrumentId == null || date == null) {
            return null;
        }
        List<PriceHistory> history = priceHistoryRepository
            .findByInstrumentIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                instrumentId,
                date,
                PageRequest.of(0, 20)
            );
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (PriceHistory point : history) {
            if (point == null || isSinglePriceHistory(point)) {
                continue;
            }
            BigDecimal open = positiveOrFallback(point.getOpenPrice(), point.getClosePrice());
            if (isPositive(open)) {
                return new PricePoint(open, point.getPriceDate());
            }
        }
        return null;
    }

    private boolean isSinglePriceHistory(PriceHistory history) {
        if (history == null) {
            return false;
        }
        BigDecimal close = positiveOrFallback(history.getAdjustedClose(), history.getClosePrice());
        if (!isPositive(close)) {
            return false;
        }
        return matchesOrMissing(history.getOpenPrice(), close)
            && matchesOrMissing(history.getHighPrice(), close)
            && matchesOrMissing(history.getLowPrice(), close);
    }

    private boolean matchesOrMissing(BigDecimal value, BigDecimal reference) {
        return value == null || value.compareTo(reference) == 0;
    }

    private PricePoint findPricePointAtOrBefore(UUID instrumentId, LocalDate date) {
        if (instrumentId == null || date == null) {
            return null;
        }
        List<PriceHistory> history = priceHistoryRepository
            .findByInstrumentIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                instrumentId,
                date,
                PageRequest.of(0, 1)
            );
        if (history == null || history.isEmpty()) {
            return null;
        }
        PriceHistory point = history.get(0);
        BigDecimal price = positiveOrFallback(point.getAdjustedClose(), point.getClosePrice());
        return price != null ? new PricePoint(price, point.getPriceDate()) : null;
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

    private ChangeDateRange normalizeChangeRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }

        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStart = startDate != null ? startDate : resolvedEnd.minusDays(1);

        if (resolvedStart.isAfter(resolvedEnd)) {
            LocalDate temp = resolvedStart;
            resolvedStart = resolvedEnd;
            resolvedEnd = temp;
        }

        return new ChangeDateRange(resolvedStart, resolvedEnd);
    }

    private record ChangeDateRange(LocalDate startDate, LocalDate endDate) {
    }

    private record CurrencyRateChange(
        BigDecimal changePercent,
        BigDecimal baseRate,
        java.time.LocalDateTime startAt,
        java.time.LocalDateTime endAt
    ) {
    }

    private record PricePoint(BigDecimal price, LocalDate date) {
    }

    private record InstrumentRangeChange(
        BigDecimal change,
        BigDecimal changePercent,
        BigDecimal basePrice,
        LocalDate startDate,
        LocalDate endDate
    ) {
        static InstrumentRangeChange empty() {
            return new InstrumentRangeChange(null, null, null, null, null);
        }
    }
}





