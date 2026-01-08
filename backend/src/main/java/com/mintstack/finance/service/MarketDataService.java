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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final InstrumentRepository instrumentRepository;
    private final CurrencyRateRepository currencyRateRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    // Currency Rates
    @Cacheable(value = "currencyRates", key = "'tcmb-latest'")
    @Transactional(readOnly = true)
    public List<CurrencyRateResponse> getLatestCurrencyRates() {
        List<CurrencyRate> rates = currencyRateRepository.findLatestBySource(RateSource.TCMB);
        return rates.stream()
            .map(this::mapToRateResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CurrencyRateResponse getCurrencyRate(String currencyCode) {
        CurrencyRate rate = currencyRateRepository
            .findTopByCurrencyCodeOrderByFetchedAtDesc(currencyCode)
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
    @Cacheable(value = "instruments", key = "#type.name()")
    @Transactional(readOnly = true)
    public List<InstrumentResponse> getInstrumentsByType(InstrumentType type) {
        List<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrue(type);
        return instruments.stream()
            .map(this::mapToInstrumentResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> getInstrumentsByType(InstrumentType type, Pageable pageable) {
        Page<Instrument> instruments = instrumentRepository.findByTypeAndIsActiveTrue(type, pageable);
        return instruments.map(this::mapToInstrumentResponse);
    }

    @Transactional(readOnly = true)
    public InstrumentResponse getInstrument(String symbol) {
        Instrument instrument = instrumentRepository.findBySymbol(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Enstrüman", "sembol", symbol));
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

    // Save methods
    @Transactional
    public void saveCurrencyRates(List<CurrencyRate> rates) {
        currencyRateRepository.saveAll(rates);
        log.info("Saved {} currency rates", rates.size());
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
        if (!priceHistoryRepository.existsByInstrumentIdAndPriceDate(
                priceHistory.getInstrument().getId(), priceHistory.getPriceDate())) {
            priceHistoryRepository.save(priceHistory);
        }
    }

    // Mapping methods
    private CurrencyRateResponse mapToRateResponse(CurrencyRate rate) {
        return CurrencyRateResponse.builder()
            .id(rate.getId())
            .currencyCode(rate.getCurrencyCode())
            .currencyName(rate.getCurrencyName())
            .buyingRate(rate.getBuyingRate())
            .sellingRate(rate.getSellingRate())
            .averageRate(rate.getAverageRate())
            .source(rate.getSource().name())
            .fetchedAt(rate.getFetchedAt())
            .rateDate(rate.getRateDate())
            .build();
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
            .isActive(instrument.getIsActive())
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
