package com.mintstack.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to validate API keys before saving and provide default URLs for each provider.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyValidationService {

    private final ObjectMapper objectMapper;

    // Default URLs for each provider - user doesn't need to enter these
    private static final Map<ApiProvider, String> DEFAULT_URLS = new HashMap<>() {{
        put(ApiProvider.ALPHA_VANTAGE, "https://www.alphavantage.co/query");
        put(ApiProvider.YAHOO_FINANCE, "https://query1.finance.yahoo.com/v8");
        put(ApiProvider.FINNHUB, "https://finnhub.io/api/v1");
        put(ApiProvider.TCMB, "https://www.tcmb.gov.tr/kurlar");
        put(ApiProvider.OTHER, null);
    }};

    /**
     * Get the default URL for a provider
     */
    public String getDefaultUrl(ApiProvider provider) {
        return DEFAULT_URLS.get(provider);
    }

    /**
     * Get all default URLs
     */
    public Map<ApiProvider, String> getAllDefaultUrls() {
        return new HashMap<>(DEFAULT_URLS);
    }

    /**
     * Validate an API key by making a test request to the provider
     * @return ValidationResult with success status and message
     */
    public ValidationResult validateApiKey(ApiProvider provider, String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return new ValidationResult(false, "API anahtarı boş olamaz");
        }

        String effectiveUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : getDefaultUrl(provider);
        
        if (effectiveUrl == null) {
            return new ValidationResult(false, "Bu provider için URL belirtmeniz gerekiyor");
        }

        try {
            switch (provider) {
                case ALPHA_VANTAGE:
                    return validateAlphaVantage(apiKey, effectiveUrl);
                case YAHOO_FINANCE:
                    return validateYahooFinance(apiKey, effectiveUrl);
                case FINNHUB:
                    return validateFinnhub(apiKey, effectiveUrl);
                case TCMB:
                    // TCMB is free public API, no key validation needed
                    return new ValidationResult(true, "TCMB - ücretsiz API, doğrulama gerekmiyor ✓");
                case OTHER:
                    return new ValidationResult(true, "Custom API - test atlandı");
                default:
                    return new ValidationResult(false, "Bilinmeyen provider");
            }
        } catch (Exception e) {
            log.error("API key validation failed for {}: {}", provider, e.getMessage());
            return new ValidationResult(false, "Bağlantı hatası: " + e.getMessage());
        }
    }

    private ValidationResult validateAlphaVantage(String apiKey, String baseUrl) {
        // 1. First, validate key format before making API call
        // Real Alpha Vantage keys are 16 characters, alphanumeric
        String trimmedKey = apiKey.trim();
        
        // Reject obviously fake/demo keys
        String lowerKey = trimmedKey.toLowerCase();
        if (lowerKey.equals("demo") || 
            lowerKey.equals("test") || 
            lowerKey.equals("apikey") ||
            lowerKey.contains("test") ||
            lowerKey.contains("fake") ||
            lowerKey.contains("sample") ||
            trimmedKey.length() < 10) {
            log.warn("Alpha Vantage key rejected - appears to be test/demo key: {}", 
                    trimmedKey.substring(0, Math.min(5, trimmedKey.length())) + "***");
            return new ValidationResult(false, "Geçersiz API anahtarı - gerçek bir anahtar girin");
        }
        
        // Check format: real keys are typically 16 chars, alphanumeric only
        if (trimmedKey.length() != 16) {
            log.warn("Alpha Vantage key has incorrect length: {} (expected 16)", trimmedKey.length());
            return new ValidationResult(false, "Geçersiz API anahtarı formatı - 16 karakter olmalı");
        }
        
        if (!trimmedKey.matches("^[A-Z0-9]+$")) {
            log.warn("Alpha Vantage key contains invalid characters");
            return new ValidationResult(false, "Geçersiz API anahtarı formatı - sadece büyük harf ve rakam olmalı");
        }
        
        log.info("Alpha Vantage key format valid, testing API...");
        
        try {
            WebClient client = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            String response = client.get()
                    .uri("?function=GLOBAL_QUOTE&symbol=IBM&apikey=" + apiKey)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> {
                                log.warn("Alpha Vantage returned HTTP error: {}", clientResponse.statusCode());
                                return clientResponse.createException();
                            })
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response == null || response.trim().isEmpty()) {
                log.warn("Alpha Vantage returned empty response");
                return new ValidationResult(false, "Sunucudan yanıt alınamadı");
            }

            log.debug("Alpha Vantage response: {}", response.substring(0, Math.min(200, response.length())));
            
            JsonNode root = objectMapper.readTree(response);

            // Check for explicit error message
            if (root.has("Error Message")) {
                log.warn("Alpha Vantage error: {}", root.get("Error Message").asText());
                return new ValidationResult(false, "Geçersiz API anahtarı veya sembol");
            }
            
            // Check for API rate limit (Note field)
            if (root.has("Note")) {
                String note = root.get("Note").asText();
                log.warn("Alpha Vantage note: {}", note);
                if (note.contains("API call frequency") || note.contains("rate limit")) {
                    return new ValidationResult(false, "API limit aşıldı - 1 dakika bekleyip tekrar deneyin");
                }
            }

            // Check for demo/information message (usually means key is limited)
            if (root.has("Information")) {
                String info = root.get("Information").asText();
                log.warn("Alpha Vantage info: {}", info);
                if (info.contains("demo") || info.contains("premium")) {
                    return new ValidationResult(false, "Demo/ücretsiz anahtar - limitli kullanım");
                }
                return new ValidationResult(false, "API anahtarı geçersiz: " + info);
            }

            // Check if we got valid Global Quote data
            if (root.has("Global Quote")) {
                JsonNode globalQuote = root.get("Global Quote");
                
                // Check if Global Quote is empty - this happens with invalid keys!
                if (globalQuote == null || globalQuote.isEmpty() || !globalQuote.has("01. symbol")) {
                    log.warn("Alpha Vantage returned empty Global Quote - invalid API key");
                    return new ValidationResult(false, "Geçersiz API anahtarı (boş yanıt)");
                }
                
                // Valid response with data
                String symbol = globalQuote.get("01. symbol").asText();
                log.info("Alpha Vantage validation successful - got data for: {}", symbol);
                return new ValidationResult(true, "API anahtarı geçerli ✓");
            }

            log.warn("Alpha Vantage returned unexpected format: {}", response.substring(0, Math.min(100, response.length())));
            return new ValidationResult(false, "Beklenmeyen yanıt formatı");
            
        } catch (Exception e) {
            log.error("Alpha Vantage validation exception: {}", e.getMessage(), e);
            return new ValidationResult(false, "Bağlantı/doğrulama hatası: " + e.getMessage());
        }
    }

    private ValidationResult validateYahooFinance(String apiKey, String baseUrl) {
        try {
            // Yahoo Finance via RapidAPI requires different validation
            WebClient client = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("X-RapidAPI-Key", apiKey)
                    .defaultHeader("X-RapidAPI-Host", "yahoo-finance15.p.rapidapi.com")
                    .build();

            String response = client.get()
                    .uri("/chart/AAPL?interval=1d&range=1d")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                return new ValidationResult(false, "Sunucudan yanıt alınamadı");
            }

            JsonNode root = objectMapper.readTree(response);

            if (root.has("chart") && root.get("chart").has("result")) {
                return new ValidationResult(true, "API anahtarı geçerli ✓");
            }

            if (root.has("message")) {
                String message = root.get("message").asText();
                if (message.contains("not subscribed") || message.contains("invalid")) {
                    return new ValidationResult(false, "Geçersiz API anahtarı");
                }
            }

            return new ValidationResult(false, "Beklenmeyen yanıt formatı");
            
        } catch (Exception e) {
            log.warn("Yahoo Finance validation error: {}", e.getMessage());
            return new ValidationResult(false, "Bağlantı hatası: " + e.getMessage());
        }
    }

    private ValidationResult validateFinnhub(String apiKey, String baseUrl) {
        try {
            WebClient client = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            String response = client.get()
                    .uri("/quote?symbol=AAPL&token=" + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                return new ValidationResult(false, "Sunucudan yanıt alınamadı");
            }

            JsonNode root = objectMapper.readTree(response);

            if (root.has("error")) {
                return new ValidationResult(false, "Geçersiz API anahtarı");
            }

            if (root.has("c") && root.get("c").asDouble() > 0) {
                return new ValidationResult(true, "API anahtarı geçerli ✓");
            }

            return new ValidationResult(false, "Beklenmeyen yanıt formatı");
            
        } catch (Exception e) {
            log.warn("Finnhub validation error: {}", e.getMessage());
            return new ValidationResult(false, "Bağlantı hatası: " + e.getMessage());
        }
    }

    /**
     * Result of API key validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
