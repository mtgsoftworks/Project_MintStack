package com.mintstack.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.external-api.fintables.enabled:false}")
    private boolean fintablesEnabled;

    // Default URLs for each provider - user doesn't need to enter these
    private static final Map<ApiProvider, String> DEFAULT_URLS = new HashMap<>() {{
        put(ApiProvider.ALPHA_VANTAGE, "https://www.alphavantage.co/query");
        put(ApiProvider.YAHOO_FINANCE, "https://query1.finance.yahoo.com/v8/finance");
        put(ApiProvider.FINNHUB, "https://finnhub.io/api/v1");
        put(ApiProvider.TCMB, "https://www.tcmb.gov.tr/kurlar");
        put(ApiProvider.TEFAS, "https://www.tefas.gov.tr/api/funds");
        put(ApiProvider.FINTABLES, "https://api.fintables.com");
        put(ApiProvider.RSS, null);
        put(ApiProvider.LLM_ENRICHMENT, "https://models.github.ai/inference");
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
        String effectiveUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : getDefaultUrl(provider);

        if (provider == ApiProvider.YAHOO_FINANCE) {
            if (effectiveUrl == null || effectiveUrl.isBlank()) {
                return new ValidationResult(false, "Yahoo Finance için geçerli bir base URL gerekli");
            }
            return validateYahooFinance(effectiveUrl);
        }

        // Check for empty key (allowed for public/no-key providers)
        if (apiKey == null || apiKey.trim().isEmpty()) {
            if (provider == ApiProvider.TCMB
                || provider == ApiProvider.TEFAS
                || provider == ApiProvider.RSS) {
                return new ValidationResult(true, provider + " - public/no-key usage enabled");
            }
            return new ValidationResult(false, "API anahtari bos olamaz");
        }

        if (effectiveUrl == null) {
            return new ValidationResult(false, "Bu provider için URL belirtmeniz gerekiyor");
        }

        try {
            switch (provider) {
                case ALPHA_VANTAGE:
                    return validateAlphaVantage(apiKey, effectiveUrl);
                case FINNHUB:
                    return validateFinnhub(apiKey, effectiveUrl);
                case TCMB:
                    return new ValidationResult(true, "TCMB - public API, validation not required");
                case TEFAS:
                    return new ValidationResult(true, "TEFAS - public API, validation not required");
                case RSS:
                    return new ValidationResult(true, "RSS - feed URLs are validated during ingestion");
                case FINTABLES:
                    return validateFintables(apiKey, effectiveUrl);
                case LLM_ENRICHMENT:
                    return new ValidationResult(true, "LLM key accepted; model provider runtime validation will run during enrichment");
                case OTHER:
                    return new ValidationResult(true, "Custom API - test atlandi");
                default:
                    return new ValidationResult(false, "Bilinmeyen provider");
            }
        } catch (Exception e) {
            log.error("API key validation failed for {}: {}", provider, e.getMessage());
            return new ValidationResult(false, "Baglanti hatasi: " + e.getMessage());
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
            return new ValidationResult(false, "GeÃ§ersiz API anahtarÄ± - gerÃ§ek bir anahtar girin");
        }
        
        // Check format: real keys are typically 16 chars, alphanumeric only
        if (trimmedKey.length() != 16) {
            log.warn("Alpha Vantage key has incorrect length: {} (expected 16)", trimmedKey.length());
            return new ValidationResult(false, "GeÃ§ersiz API anahtarÄ± formatÄ± - 16 karakter olmalÄ±");
        }
        
        if (!trimmedKey.matches("^[A-Z0-9]+$")) {
            log.warn("Alpha Vantage key contains invalid characters");
            return new ValidationResult(false, "GeÃ§ersiz API anahtarÄ± formatÄ± - sadece bÃ¼yÃ¼k harf ve rakam olmalÄ±");
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
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || response.trim().isEmpty()) {
                log.warn("Alpha Vantage returned empty response");
                return new ValidationResult(false, "Sunucudan yanÄ±t alÄ±namadÄ±");
            }

            log.debug("Alpha Vantage response: {}", response.substring(0, Math.min(200, response.length())));
            
            JsonNode root = objectMapper.readTree(response);

            // Check for explicit error message
            if (root.has("Error Message")) {
                log.warn("Alpha Vantage error: {}", root.get("Error Message").asText());
                return new ValidationResult(false, "GeÃ§ersiz API anahtarÄ± veya sembol");
            }
            
            // Check for API rate limit (Note field)
            if (root.has("Note")) {
                String note = root.get("Note").asText();
                log.warn("Alpha Vantage note: {}", note);
                if (note.contains("API call frequency") || note.contains("rate limit")) {
                    return new ValidationResult(false, "API limit aÅŸÄ±ldÄ± - 1 dakika bekleyip tekrar deneyin");
                }
            }

            // Check for demo/information message (usually means key is limited)
            if (root.has("Information")) {
                String info = root.get("Information").asText();
                log.warn("Alpha Vantage info: {}", info);
                if (info.contains("demo") || info.contains("premium")) {
                    return new ValidationResult(false, "Demo/Ã¼cretsiz anahtar - limitli kullanÄ±m");
                }
                return new ValidationResult(false, "API anahtarÄ± geÃ§ersiz: " + info);
            }

            // Check if we got valid Global Quote data
            if (root.has("Global Quote")) {
                JsonNode globalQuote = root.get("Global Quote");
                
                // Check if Global Quote is empty - this happens with invalid keys!
                if (globalQuote == null || globalQuote.isEmpty() || !globalQuote.has("01. symbol")) {
                    log.warn("Alpha Vantage returned empty Global Quote - invalid API key");
                    return new ValidationResult(false, "GeÃ§ersiz API anahtarÄ± (boÅŸ yanÄ±t)");
                }
                
                // Valid response with data
                String symbol = globalQuote.get("01. symbol").asText();
                log.info("Alpha Vantage validation successful - got data for: {}", symbol);
                return new ValidationResult(true, "API anahtarÄ± geÃ§erli âœ“");
            }

            log.warn("Alpha Vantage returned unexpected format: {}", response.substring(0, Math.min(100, response.length())));
            return new ValidationResult(false, "Beklenmeyen yanÄ±t formatÄ±");
            
        } catch (Exception e) {
            log.error("Alpha Vantage validation exception: {}", e.getMessage(), e);
            return new ValidationResult(false, "BaÄŸlantÄ±/doÄŸrulama hatasÄ±: " + e.getMessage());
        }
    }

    private ValidationResult validateYahooFinance(String baseUrl) {
        try {
            WebClient client = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();

            String response = client.get()
                    .uri("/chart/AAPL?interval=1d&range=1d")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null || response.trim().isEmpty()) {
                return new ValidationResult(false, "Sunucudan yanit alinamadi");
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode chart = root.path("chart");
            JsonNode result = chart.path("result");
            JsonNode error = chart.path("error");

            if (!result.isMissingNode() && result.isArray() && result.size() > 0) {
                return new ValidationResult(true, "Yahoo Finance public endpoint erisilebilir");
            }

            if (!error.isMissingNode() && !error.isNull()) {
                return new ValidationResult(false, "Yahoo Finance chart endpoint hatasi");
            }

            return new ValidationResult(false, "Beklenmeyen yanit formati");

        } catch (Exception e) {
            log.warn("Yahoo Finance validation error: {}", e.getMessage());
            return new ValidationResult(false, "Baglanti hatasi: " + e.getMessage());
        }
    }

    private ValidationResult validateFinnhub(String apiKey, String baseUrl) {
        try {
            WebClient client = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            String stockResponse = client.get()
                    .uri("/quote?symbol=AAPL&token=" + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (stockResponse == null) {
                return new ValidationResult(false, "Sunucudan yanÄ±t alÄ±namadÄ±");
            }

            JsonNode stockRoot = objectMapper.readTree(stockResponse);
            if (stockRoot.has("error")) {
                return new ValidationResult(false, "GeÃ§ersiz API anahtarÄ±");
            }

            if (!stockRoot.has("c") || stockRoot.get("c").asDouble() <= 0) {
                return new ValidationResult(false, "Beklenmeyen yanÄ±t formatÄ±");
            }

            String forexResponse = client.get()
                    .uri("/forex/rates?base=USD&token=" + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (forexResponse != null) {
                JsonNode forexRoot = objectMapper.readTree(forexResponse);
                if (forexRoot.has("error")) {
                    String error = forexRoot.get("error").asText("").toLowerCase();
                    if (error.contains("don't have access") || error.contains("not have access")) {
                        return new ValidationResult(true,
                            "API anahtarÄ± geÃ§erli âœ“, ancak Forex endpoint eriÅŸimi planÄ±nÄ±zda kapalÄ± olabilir");
                    }
                }
            }

            return new ValidationResult(true, "API anahtarÄ± geÃ§erli âœ“");

        } catch (Exception e) {
            log.warn("Finnhub validation error: {}", e.getMessage());
            return new ValidationResult(false, "BaÄŸlantÄ± hatasÄ±: " + e.getMessage());
        }
    }

    private ValidationResult validateFintables(String apiKey, String baseUrl) {
        if (!fintablesEnabled) {
            return new ValidationResult(false,
                "Fintables provider policy geregi pasif. APP_EXTERNAL_API_FINTABLES_ENABLED=true olmadan aktif edilemez.");
        }
        String trimmedKey = apiKey == null ? "" : apiKey.trim();
        if (trimmedKey.length() < 16) {
            return new ValidationResult(false, "Fintables API anahtari en az 16 karakter olmali");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return new ValidationResult(false, "Fintables base URL zorunludur");
        }
        return new ValidationResult(true,
            "Fintables key format accepted; live validation is disabled until endpoint contract is configured");
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



