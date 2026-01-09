package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.CompareInstrumentsRequest;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    /**
     * Calculate Simple Moving Average (SMA)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMovingAverage(String symbol, int period, LocalDate endDate) {
        LocalDate startDate = endDate.minusDays(period + 50); // Extra data for calculation
        
        List<PriceHistory> history = priceHistoryRepository.findBySymbolAndDateRange(symbol, startDate, endDate);
        
        if (history.isEmpty()) {
            throw new ResourceNotFoundException("Fiyat geçmişi", "sembol", symbol);
        }
        
        List<BigDecimal> closePrices = history.stream()
            .map(PriceHistory::getClosePrice)
            .collect(Collectors.toList());
        
        List<Map<String, Object>> maData = new ArrayList<>();
        
        for (int i = period - 1; i < history.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                sum = sum.add(closePrices.get(j));
            }
            BigDecimal ma = sum.divide(BigDecimal.valueOf(period), 6, RoundingMode.HALF_UP);
            
            Map<String, Object> point = new HashMap<>();
            point.put("date", history.get(i).getPriceDate());
            point.put("price", closePrices.get(i));
            point.put("ma", ma);
            maData.add(point);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("period", period);
        result.put("data", maData);
        
        return result;
    }

    /**
     * Get multiple moving averages (MA7, MA25, MA99)
     */
    @Cacheable(value = "historicalData", key = "'ma-' + #symbol + '-' + #endDate")
    @Transactional(readOnly = true)
    public Map<String, Object> getMultipleMovingAverages(String symbol, LocalDate endDate) {
        LocalDate startDate = endDate.minusDays(150);
        
        List<PriceHistory> history = priceHistoryRepository.findBySymbolAndDateRange(symbol, startDate, endDate);
        
        if (history.size() < 99) {
            throw new ResourceNotFoundException("Yeterli fiyat geçmişi yok", "sembol", symbol);
        }
        
        List<BigDecimal> closePrices = history.stream()
            .map(PriceHistory::getClosePrice)
            .collect(Collectors.toList());
        
        List<Map<String, Object>> maData = new ArrayList<>();
        
        for (int i = 98; i < history.size(); i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", history.get(i).getPriceDate());
            point.put("price", closePrices.get(i));
            point.put("ma7", calculateMA(closePrices, i, 7));
            point.put("ma25", calculateMA(closePrices, i, 25));
            point.put("ma99", calculateMA(closePrices, i, 99));
            maData.add(point);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("data", maData);
        
        return result;
    }

    /**
     * Analyze trend (uptrend, downtrend, sideways)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTrendAnalysis(String symbol, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        List<PriceHistory> history = priceHistoryRepository.findBySymbolAndDateRange(symbol, startDate, endDate);
        
        if (history.isEmpty()) {
            throw new ResourceNotFoundException("Fiyat geçmişi", "sembol", symbol);
        }
        
        BigDecimal firstPrice = history.get(0).getClosePrice();
        BigDecimal lastPrice = history.get(history.size() - 1).getClosePrice();
        
        BigDecimal changePercent = lastPrice.subtract(firstPrice)
            .divide(firstPrice, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        // Calculate volatility (standard deviation)
        BigDecimal avgPrice = history.stream()
            .map(PriceHistory::getClosePrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(history.size()), 6, RoundingMode.HALF_UP);
        
        BigDecimal variance = history.stream()
            .map(h -> h.getClosePrice().subtract(avgPrice).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(history.size()), 6, RoundingMode.HALF_UP);
        
        double volatility = Math.sqrt(variance.doubleValue());
        
        // Determine trend
        String trend;
        String trendStrength;
        
        double changePct = changePercent.doubleValue();
        if (changePct > 5) {
            trend = "UPTREND";
            trendStrength = changePct > 15 ? "STRONG" : "MODERATE";
        } else if (changePct < -5) {
            trend = "DOWNTREND";
            trendStrength = changePct < -15 ? "STRONG" : "MODERATE";
        } else {
            trend = "SIDEWAYS";
            trendStrength = "WEAK";
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("period", days);
        result.put("startPrice", firstPrice);
        result.put("endPrice", lastPrice);
        result.put("changePercent", changePercent);
        result.put("trend", trend);
        result.put("trendStrength", trendStrength);
        result.put("volatility", BigDecimal.valueOf(volatility).setScale(4, RoundingMode.HALF_UP));
        result.put("highPrice", history.stream().map(PriceHistory::getHighPrice).filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(null));
        result.put("lowPrice", history.stream().map(PriceHistory::getLowPrice).filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null));
        
        // Add chart data
        List<Map<String, Object>> chartData = history.stream()
            .map(h -> {
                Map<String, Object> point = new HashMap<>();
                point.put("date", h.getPriceDate());
                point.put("price", h.getClosePrice());
                return point;
            })
            .collect(Collectors.toList());
        result.put("data", chartData);
        
        return result;
    }

    /**
     * Compare multiple instruments
     */
    @Transactional(readOnly = true)
    public Map<String, Object> compareInstruments(CompareInstrumentsRequest request) {
        List<Map<String, Object>> comparisonData = new ArrayList<>();
        
        for (String symbol : request.getSymbols()) {
            List<PriceHistory> history = priceHistoryRepository.findBySymbolAndDateRange(
                symbol, request.getStartDate(), request.getEndDate());
            
            if (history.isEmpty()) {
                log.warn("No price history found for symbol: {}", symbol);
                continue;
            }
            
            BigDecimal firstPrice = history.get(0).getClosePrice();
            
            // Normalize prices as percentage change from start
            List<Map<String, Object>> normalizedPrices = history.stream()
                .map(h -> {
                    BigDecimal normalizedValue = h.getClosePrice()
                        .subtract(firstPrice)
                        .divide(firstPrice, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", h.getPriceDate());
                    point.put("value", normalizedValue);
                    point.put("price", h.getClosePrice());
                    return point;
                })
                .collect(Collectors.toList());
            
            Instrument instrument = instrumentRepository.findBySymbol(symbol).orElse(null);
            
            Map<String, Object> instrumentData = new HashMap<>();
            instrumentData.put("symbol", symbol);
            instrumentData.put("name", instrument != null ? instrument.getName() : symbol);
            instrumentData.put("data", normalizedPrices);
            
            comparisonData.add(instrumentData);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", request.getStartDate());
        result.put("endDate", request.getEndDate());
        result.put("instruments", comparisonData);
        
        return result;
    }

    private BigDecimal calculateMA(List<BigDecimal> prices, int currentIndex, int period) {
        if (currentIndex < period - 1) {
            return null;
        }
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            sum = sum.add(prices.get(i));
        }
        return sum.divide(BigDecimal.valueOf(period), 6, RoundingMode.HALF_UP);
    }
}
