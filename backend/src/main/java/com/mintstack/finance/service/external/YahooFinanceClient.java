package com.mintstack.finance.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.exception.ExternalApiException;
import com.mintstack.finance.repository.InstrumentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class YahooFinanceClient {

    private final WebClient yahooFinanceWebClient;
    private final InstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, Long> latestVolumes = new ConcurrentHashMap<>();

    // Direct Yahoo Finance URL (no API key needed)
    private static final String DIRECT_YAHOO_URL = "https://query1.finance.yahoo.com/v8";
    
    /**
     * Fetch stock quote for BIST stocks (symbol.IS format)
     * Supports both RapidAPI (with key) and direct Yahoo Finance (no key)
     */
    @CircuitBreaker(name = "yahooFinanceApi", fallbackMethod = "fetchStockPriceFallback")
    @Retry(name = "externalApi")
    public BigDecimal fetchStockPrice(String symbol, String apiKey, String baseUrl) {
        try {
            String yahooSymbol = symbol;
            if (!yahooSymbol.contains(".")) {
                yahooSymbol = symbol + ".IS"; // BIST stocks on Yahoo Finance
            }
            String url = "/chart/" + yahooSymbol + "?interval=1d&range=1d";
            
            log.debug("Fetching Yahoo Finance quote: {}", yahooSymbol);
            
            // Try with provided config first, then fallback to direct Yahoo
            String response = null;
            Exception lastError = null;
            
            // Try RapidAPI if key provided
            if (apiKey != null && !apiKey.isEmpty()) {
                try {
                    WebClient client = getClient(apiKey, baseUrl);
                    response = client.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                } catch (Exception e) {
                    log.warn("RapidAPI fetch failed for {}, trying direct Yahoo...", symbol);
                    lastError = e;
                }
            }
            
            // Fallback to direct Yahoo Finance (no API key needed)
            if (response == null) {
                try {
                    WebClient directClient = WebClient.builder()
                        .baseUrl(DIRECT_YAHOO_URL)
                        .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build();
                    
                    response = directClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                    
                    log.debug("Direct Yahoo Finance fetch successful for {}", symbol);
                } catch (Exception e) {
                    log.error("Direct Yahoo Finance also failed for {}", symbol, e);
                    throw lastError != null ? lastError : e;
                }
            }
            
            if (response == null) {
                throw new ExternalApiException("Yahoo Finance", "Boş yanıt");
            }
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("chart").path("result").get(0);
            JsonNode meta = result.path("meta");
            
            BigDecimal price = new BigDecimal(meta.path("regularMarketPrice").asText());
            JsonNode volumeNode = meta.path("regularMarketVolume");
            if (!volumeNode.isMissingNode() && !volumeNode.isNull()) {
                long volume = volumeNode.asLong();
                latestVolumes.put(symbol, volume);
                latestVolumes.put(yahooSymbol, volume);
            }
            log.debug("Fetched price for {}: {}", symbol, price);
            
            return price;
        } catch (Exception e) {
            log.error("Error fetching Yahoo Finance data for {}", symbol, e);
            throw new ExternalApiException("Yahoo Finance", "Fiyat bilgisi alınamadı: " + symbol, e);
        }
    }

    /**
     * Fallback for fetchStockPrice - returns last known price from DB
     */
    public BigDecimal fetchStockPriceFallback(String symbol, String apiKey, String baseUrl, Exception e) {
        log.warn("Yahoo Finance stock price fallback for {}: {}", symbol, e.getMessage());
        return instrumentRepository.findBySymbol(symbol)
            .map(Instrument::getCurrentPrice)
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Fetch historical data for a stock
     */
    @CircuitBreaker(name = "yahooFinanceApi", fallbackMethod = "fetchHistoricalDataFallback")
    public List<PriceHistory> fetchHistoricalData(String symbol, LocalDate startDate, LocalDate endDate, String apiKey, String baseUrl) {
        List<PriceHistory> history = new ArrayList<>();
        
        try {
            String yahooSymbol = symbol;
            if (!yahooSymbol.contains(".")) {
                yahooSymbol = symbol + ".IS";
            }
            long period1 = startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            long period2 = endDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            
            String url = "/chart/" + yahooSymbol + "?period1=" + period1 + "&period2=" + period2 + "&interval=1d";
            
            log.info("Fetching Yahoo Finance historical data: {}", yahooSymbol);
            
            WebClient client = getClient(apiKey, baseUrl);
            
            String response = client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (response == null) {
                throw new ExternalApiException("Yahoo Finance", "Boş yanıt");
            }
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("chart").path("result").get(0);
            JsonNode timestamps = result.path("timestamp");
            JsonNode indicators = result.path("indicators").path("quote").get(0);
            JsonNode adjClose = result.path("indicators").path("adjclose");
            
            Instrument instrument = instrumentRepository.findBySymbol(symbol).orElse(null);
            if (instrument == null) {
                log.warn("Instrument not found: {}", symbol);
                return history;
            }
            
            for (int i = 0; i < timestamps.size(); i++) {
                // Skip entries with null close price (market holidays, etc.)
                BigDecimal closePrice = getBigDecimal(indicators.path("close"), i);
                if (closePrice == null) {
                    continue;
                }
                
                long timestamp = timestamps.get(i).asLong();
                LocalDate date = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
                
                PriceHistory priceHistory = PriceHistory.builder()
                    .instrument(instrument)
                    .priceDate(date)
                    .openPrice(getBigDecimal(indicators.path("open"), i))
                    .highPrice(getBigDecimal(indicators.path("high"), i))
                    .lowPrice(getBigDecimal(indicators.path("low"), i))
                    .closePrice(closePrice)
                    .volume(indicators.path("volume").get(i) != null && !indicators.path("volume").get(i).isNull() 
                        ? indicators.path("volume").get(i).asLong() : null)
                    .adjustedClose(adjClose != null && adjClose.size() > 0 
                        ? getBigDecimal(adjClose.get(0).path("adjclose"), i) : null)
                    .build();
                
                history.add(priceHistory);
            }
            
            log.info("Fetched {} historical data points for {}", history.size(), symbol);
            return history;
        } catch (Exception e) {
            log.error("Error fetching Yahoo Finance historical data for {}", symbol, e);
            throw new ExternalApiException("Yahoo Finance", "Geçmiş veri alınamadı: " + symbol, e);
        }
    }

    /**
     * Fallback for fetchHistoricalData - returns empty list
     */
    public List<PriceHistory> fetchHistoricalDataFallback(String symbol, LocalDate startDate, LocalDate endDate, String apiKey, String baseUrl, Exception e) {
        log.warn("Yahoo Finance historical data fallback for {}: {}", symbol, e.getMessage());
        return Collections.emptyList();
    }

    /**
     * Fetch multiple stock prices
     */
    public void updateStockPrices(List<String> symbols, String apiKey, String baseUrl) {
        for (String symbol : symbols) {
            try {
                BigDecimal price = fetchStockPrice(symbol, apiKey, baseUrl);
                instrumentRepository.findBySymbol(symbol).ifPresent(instrument -> {
                    instrument.setPreviousClose(instrument.getCurrentPrice());
                    instrument.setCurrentPrice(price);
                    instrumentRepository.save(instrument);
                });
            } catch (Exception e) {
                log.error("Failed to update price for {}: {}", symbol, e.getMessage());
            }
        }
    }

    private BigDecimal getBigDecimal(JsonNode node, int index) {
        if (node == null || node.get(index) == null || node.get(index).isNull()) {
            return null;
        }
        return new BigDecimal(node.get(index).asText());
    }
    private WebClient getClient(String apiKey, String baseUrl) {
        WebClient client = yahooFinanceWebClient;
        if (baseUrl != null && !baseUrl.isEmpty()) {
            client = client.mutate().baseUrl(baseUrl).build();
        }
        
        if (apiKey != null && !apiKey.isEmpty()) {
            // RapidAPI headers
            client = client.mutate()
                .defaultHeader("X-RapidAPI-Key", apiKey)
                .build();
                
            if (baseUrl != null && !baseUrl.isEmpty()) {
                try {
                    URI uri = URI.create(baseUrl);
                    String host = uri.getHost();
                    if (host != null) {
                         client = client.mutate().defaultHeader("X-RapidAPI-Host", host).build();
                    }
                } catch (Exception e) {
                    log.warn("Could not parse host from base URL: {}", baseUrl);
                }
            }
        }
        return client;
    }

    public Long getLatestVolume(String symbol) {
        return latestVolumes.get(symbol);
    }
}
