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
public class AlphaVantageClient {

    private final WebClient alphaVantageWebClient;
    private final ObjectMapper objectMapper;
    private final UserApiConfigRepository userApiConfigRepository;

    /**
     * Fetch currency exchange rate
     */
    public CurrencyRate fetchExchangeRate(String fromCurrency, String toCurrency) {
        List<UserApiConfig> configs = userApiConfigRepository.findByProviderAndIsActiveTrue(ApiProvider.ALPHA_VANTAGE);
        if (configs.isEmpty()) {
            throw new ExternalApiException("Alpha Vantage", "Alpha Vantage API yapılandırması bulunamadı");
        }
        
        String apiKey = configs.get(0).getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ExternalApiException("Alpha Vantage", "API key gerekli");
        }
        
        try {
            String url = "?function=CURRENCY_EXCHANGE_RATE&from_currency=" + fromCurrency 
                + "&to_currency=" + toCurrency + "&apikey=" + apiKey;
            
            log.debug("Fetching Alpha Vantage rate: {}/{}", fromCurrency, toCurrency);
            
            String response = alphaVantageWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (response == null) {
                throw new ExternalApiException("Alpha Vantage", "Boş yanıt");
            }
            
            JsonNode root = objectMapper.readTree(response);
            
            // Check for API limit error
            if (root.has("Note") || root.has("Information")) {
                String message = root.has("Note") ? root.get("Note").asText() : root.get("Information").asText();
                log.warn("Alpha Vantage API limit: {}", message);
                throw new ExternalApiException("Alpha Vantage", "API limiti aşıldı");
            }
            
            JsonNode rateData = root.path("Realtime Currency Exchange Rate");
            
            if (rateData.isMissingNode()) {
                throw new ExternalApiException("Alpha Vantage", "Kur bilgisi bulunamadı");
            }
            
            BigDecimal exchangeRate = new BigDecimal(rateData.path("5. Exchange Rate").asText());
            BigDecimal bidPrice = rateData.has("8. Bid Price") 
                ? new BigDecimal(rateData.path("8. Bid Price").asText()) 
                : exchangeRate;
            BigDecimal askPrice = rateData.has("9. Ask Price") 
                ? new BigDecimal(rateData.path("9. Ask Price").asText()) 
                : exchangeRate;
            
            return CurrencyRate.builder()
                .currencyCode(fromCurrency)
                .currencyName(rateData.path("2. From_Currency Name").asText())
                .buyingRate(bidPrice)
                .sellingRate(askPrice)
                .source(RateSource.ALPHA_VANTAGE)
                .fetchedAt(LocalDateTime.now())
                .build();
            
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching Alpha Vantage data", e);
            throw new ExternalApiException("Alpha Vantage", "Kur bilgisi alinamadi", e);
        }
    }

    /**
     * Fetch global quote for a stock
     */
    public BigDecimal fetchGlobalQuote(String symbol) {
        List<UserApiConfig> configs = userApiConfigRepository.findByProviderAndIsActiveTrue(ApiProvider.ALPHA_VANTAGE);
        if (configs.isEmpty()) {
            throw new ExternalApiException("Alpha Vantage", "Alpha Vantage API yapılandırması bulunamadı");
        }
        
        String apiKey = configs.get(0).getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ExternalApiException("Alpha Vantage", "API key gerekli");
        }
        
        try {
            String url = "?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey;
            
            log.debug("Fetching Alpha Vantage quote: {}", symbol);
            
            String response = alphaVantageWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (response == null) {
                throw new ExternalApiException("Alpha Vantage", "Boş yanıt");
            }
            
            JsonNode root = objectMapper.readTree(response);
            
            // Check for API limit error
            if (root.has("Note") || root.has("Information")) {
                log.warn("Alpha Vantage API limit reached");
                throw new ExternalApiException("Alpha Vantage", "API limiti aşıldı");
            }
            
            JsonNode quote = root.path("Global Quote");
            
            if (quote.isMissingNode() || !quote.has("05. price")) {
                throw new ExternalApiException("Alpha Vantage", "Fiyat bilgisi bulunamadı: " + symbol);
            }
            
            return new BigDecimal(quote.path("05. price").asText());
            
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching Alpha Vantage quote for {}", symbol, e);
            throw new ExternalApiException("Alpha Vantage", "Fiyat bilgisi alınamadı: " + symbol, e);
        }
    }
}
