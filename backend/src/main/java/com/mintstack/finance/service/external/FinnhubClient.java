package com.mintstack.finance.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.CurrencyRate.RateSource;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.exception.ExternalApiException;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Observed(name = "external.finnhub", contextualName = "finnhub-client")
public class FinnhubClient {

    private final WebClient finnhubWebClient;
    private final ObjectMapper objectMapper;
    private final UserApiConfigRepository userApiConfigRepository;
    private final InstrumentRepository instrumentRepository;
    private final CurrencyRateRepository currencyRateRepository;

    /**
     * Fetch stock quote for a symbol
     */
    @CircuitBreaker(name = "finnhubApi", fallbackMethod = "fetchStockQuoteFallback")
    @Retry(name = "externalApi")
    @Observed(name = "external.finnhub.fetch-stock-quote", contextualName = "fetch-finnhub-stock-quote")
    public BigDecimal fetchStockQuote(String symbol) {
        List<UserApiConfig> configs = userApiConfigRepository.findByProviderAndIsActiveTrue(ApiProvider.FINNHUB);
        if (configs.isEmpty()) {
            throw new ExternalApiException("Finnhub", "Finnhub API yapılandırması bulunamadı");
        }
        
        String apiKey = configs.get(0).getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ExternalApiException("Finnhub", "API key gerekli");
        }
        
        try {
            String url = "?symbol=" + symbol + "&token=" + apiKey;
            
            log.debug("Fetching Finnhub quote: {}", symbol);
            
            String response = finnhubWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (response == null) {
                throw new ExternalApiException("Finnhub", "Boş yanıt");
            }
            
            log.debug("Finnhub response for {}: {}", symbol, response.substring(0, Math.min(200, response.length())));
            
            JsonNode root = objectMapper.readTree(response);
            
            // Check for error
            if (root.has("error")) {
                String error = root.get("error").asText();
                log.warn("Finnhub error: {}", error);
                throw new ExternalApiException("Finnhub", error);
            }
            
            if (!root.has("c")) {
                throw new ExternalApiException("Finnhub", "Fiyat bilgisi bulunamadı: " + symbol);
            }
            
            // c = current price
            return new BigDecimal(root.get("c").asText());
            
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching Finnhub quote for {}", symbol, e);
            throw new ExternalApiException("Finnhub", "Fiyat bilgisi alınamadı: " + symbol, e);
        }
    }

    /**
     * Fallback for fetchStockQuote - returns last known price from DB
     */
    public BigDecimal fetchStockQuoteFallback(String symbol, Exception e) {
        log.warn("Finnhub stock quote fallback for {}: {}", symbol, e.getMessage());
        return instrumentRepository.findBySymbol(symbol)
            .map(Instrument::getCurrentPrice)
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Fetch forex rate
     */
    @CircuitBreaker(name = "finnhubApi", fallbackMethod = "fetchForexRateFallback")
    @Retry(name = "externalApi")
    @Observed(name = "external.finnhub.fetch-forex-rate", contextualName = "fetch-finnhub-forex-rate")
    public CurrencyRate fetchForexRate(String fromCurrency, String toCurrency) {
        List<UserApiConfig> configs = userApiConfigRepository.findByProviderAndIsActiveTrue(ApiProvider.FINNHUB);
        if (configs.isEmpty()) {
            throw new ExternalApiException("Finnhub", "Finnhub API yapılandırması bulunamadı");
        }
        
        String apiKey = configs.get(0).getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ExternalApiException("Finnhub", "API key gerekli");
        }
        
        try {
            String symbol = fromCurrency + toCurrency;
            String url = "?symbol=" + symbol + "&token=" + apiKey;
            
            log.debug("Fetching Finnhub forex rate: {}/{}", fromCurrency, toCurrency);
            
            String response = finnhubWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (response == null) {
                throw new ExternalApiException("Finnhub", "Boş yanıt");
            }
            
            JsonNode root = objectMapper.readTree(response);
            
            // Check for error
            if (root.has("error")) {
                String error = root.get("error").asText();
                log.warn("Finnhub error: {}", error);
                throw new ExternalApiException("Finnhub", error);
            }
            
            if (!root.has("c")) {
                throw new ExternalApiException("Finnhub", "Kur bilgisi bulunamadı");
            }
            
            BigDecimal exchangeRate = new BigDecimal(root.get("c").asText());
            
            return CurrencyRate.builder()
                .currencyCode(fromCurrency)
                .currencyName(fromCurrency + "/" + toCurrency)
                .buyingRate(exchangeRate)
                .sellingRate(exchangeRate)
                .source(RateSource.FINNHUB)
                .fetchedAt(LocalDateTime.now())
                .build();
            
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching Finnhub forex rate", e);
            throw new ExternalApiException("Finnhub", "Kur bilgisi alınamadı", e);
        }
    }

    /**
     * Fallback for fetchForexRate - returns last known rate from DB
     */
    public CurrencyRate fetchForexRateFallback(String fromCurrency, String toCurrency, Exception e) {
        log.warn("Finnhub forex rate fallback for {}/{}: {}", fromCurrency, toCurrency, e.getMessage());
        return currencyRateRepository.findByCurrencyCodeAndSource(fromCurrency, RateSource.FINNHUB)
            .orElse(CurrencyRate.builder()
                .currencyCode(fromCurrency)
                .currencyName(fromCurrency + "/" + toCurrency)
                .buyingRate(BigDecimal.ZERO)
                .sellingRate(BigDecimal.ZERO)
                .source(RateSource.FINNHUB)
                .fetchedAt(LocalDateTime.now())
                .build());
    }

    /**
     * Fetch crypto price
     */
    @CircuitBreaker(name = "finnhubApi", fallbackMethod = "fetchCryptoPriceFallback")
    @Retry(name = "externalApi")
    @Observed(name = "external.finnhub.fetch-crypto-price", contextualName = "fetch-finnhub-crypto-price")
    public BigDecimal fetchCryptoPrice(String symbol) {
        List<UserApiConfig> configs = userApiConfigRepository.findByProviderAndIsActiveTrue(ApiProvider.FINNHUB);
        if (configs.isEmpty()) {
            throw new ExternalApiException("Finnhub", "Finnhub API yapılandırması bulunamadı");
        }
        
        String apiKey = configs.get(0).getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ExternalApiException("Finnhub", "API key gerekli");
        }
        
        try {
            String url = "?symbol=" + symbol + "&token=" + apiKey;
            
            log.debug("Fetching Finnhub crypto price: {}", symbol);
            
            String response = finnhubWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (response == null) {
                throw new ExternalApiException("Finnhub", "Boş yanıt");
            }
            
            JsonNode root = objectMapper.readTree(response);
            
            // Check for error
            if (root.has("error")) {
                String error = root.get("error").asText();
                log.warn("Finnhub error: {}", error);
                throw new ExternalApiException("Finnhub", error);
            }
            
            if (!root.has("c")) {
                throw new ExternalApiException("Finnhub", "Kripto fiyatı bulunamadı: " + symbol);
            }
            
            return new BigDecimal(root.get("c").asText());
            
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching Finnhub crypto price for {}", symbol, e);
            throw new ExternalApiException("Finnhub", "Kripto fiyatı alınamadı: " + symbol, e);
        }
    }

    /**
     * Fallback for fetchCryptoPrice - returns last known price from DB
     */
    public BigDecimal fetchCryptoPriceFallback(String symbol, Exception e) {
        log.warn("Finnhub crypto price fallback for {}: {}", symbol, e.getMessage());
        return instrumentRepository.findBySymbol(symbol)
            .map(Instrument::getCurrentPrice)
            .orElse(BigDecimal.ZERO);
    }
}
