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

    private final InstrumentRepository instrumentRepository;
    private final CurrencyRateRepository currencyRateRepository;

    private final PriceHistoryRepository priceHistoryRepository;
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
        
        currencyRateRepository.deleteAll();
        priceHistoryRepository.deleteAll();
        
        log.info("Deleted all market data: {} currency rates, {} price history records", 
            currencyCount, priceHistoryCount);
        
        return Map.of(
            "deletedCurrencyRates", currencyCount,
            "deletedPriceHistory", priceHistoryCount
        );
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
            .volume(resolveLatestVolume(instrument))
            .maturityDate(resolveMaturityDate(instrument))
            .isActive(instrument.getIsActive())
            .build();
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

        if (instrument.getSymbol() != null) {
            return yahooFinanceClient.getLatestVolume(instrument.getSymbol());
        }
        return null;
    }

    private LocalDate resolveMaturityDate(Instrument instrument) {
        if (instrument == null || instrument.getType() != InstrumentType.VIOP || instrument.getSymbol() == null) {
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
