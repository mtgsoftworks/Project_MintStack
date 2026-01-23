package com.mintstack.finance.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.CurrencyRate.RateSource;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.exception.ExternalApiException;
import com.mintstack.finance.repository.UserApiConfigRepository;
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
public class FinnhubClient {

    private final WebClient finnhubWebClient;
    private final ObjectMapper objectMapper;
    private final UserApiConfigRepository userApiConfigRepository;

    /**
     * Fetch stock quote for a symbol
     */
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
     * Fetch forex rate
     */
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
     * Fetch crypto price
     */
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
}
