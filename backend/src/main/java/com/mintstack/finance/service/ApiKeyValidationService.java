package com.mintstack.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetAddress;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        put(ApiProvider.BIST_DATASTORE, "https://www.borsaistanbul.com");
        put(ApiProvider.FINTABLES, "https://api.fintables.com");
        put(ApiProvider.RSS, null);
        put(ApiProvider.LLM_ENRICHMENT, "https://models.github.ai/inference");
        put(ApiProvider.OTHER, null);
    }};

    // SSRF Protection: Blocked hosts
    private static final Set<String> BLOCKED_HOSTS = Set.of(
        "127.0.0.1", "localhost", "0.0.0.0", "::1", "169.254.169.254",
        "metadata.google.internal", "metadata", "metadata.google"
    );

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
     *
     * @return ValidationResult with success status and message
     */
    public ValidationResult validateApiKey(ApiProvider provider, String apiKey, String baseUrl) {
        String effectiveUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : getDefaultUrl(provider);
        String normalizedApiKey = apiKey == null ? null : apiKey.trim();

        if (provider == ApiProvider.YAHOO_FINANCE) {
            if (effectiveUrl == null || effectiveUrl.isBlank()) {
                return new ValidationResult(false, "Yahoo Finance icin gecerli bir base URL gerekli");
            }
            return validateYahooFinance(effectiveUrl);
        }

        // Check for empty key (allowed for public/no-key providers)
        if (normalizedApiKey == null || normalizedApiKey.isEmpty()) {
            if (provider == ApiProvider.TCMB
                || provider == ApiProvider.TEFAS
                || provider == ApiProvider.BIST_DATASTORE
                || provider == ApiProvider.RSS) {
                return new ValidationResult(true, provider + " - public/no-key usage enabled");
            }
            return new ValidationResult(false, "API anahtari bos olamaz");
        }

        if (effectiveUrl == null) {
            return new ValidationResult(false, "Bu provider icin URL belirtmeniz gerekiyor");
        }

        try {
            return switch (provider) {
                case ALPHA_VANTAGE -> validateAlphaVantage(normalizedApiKey, effectiveUrl);
                case FINNHUB -> validateFinnhub(normalizedApiKey, effectiveUrl);
                case TCMB -> new ValidationResult(true, "TCMB - public API, validation not required");
                case TEFAS -> new ValidationResult(true, "TEFAS - public API, validation not required");
                case BIST_DATASTORE -> validateBistDataStore(effectiveUrl);
                case RSS -> new ValidationResult(true, "RSS - feed URLs are validated during ingestion");
                case FINTABLES -> validateFintables(normalizedApiKey, effectiveUrl);
                case LLM_ENRICHMENT -> new ValidationResult(true, "LLM key accepted; runtime validation will run during enrichment");
                case OTHER -> new ValidationResult(true, "Custom API - test atlandi");
                default -> new ValidationResult(false, "Bilinmeyen provider");
            };
        } catch (Exception e) {
            log.error("API key validation failed for {}: {}", provider, e.getMessage());
            return new ValidationResult(false, "Baglanti hatasi: " + e.getMessage());
        }
    }

    private ValidationResult validateAlphaVantage(String apiKey, String baseUrl) {
        try {
            validateUrl(baseUrl); // SSRF check
            String normalizedBaseUrl = normalizeAlphaVantageBaseUrl(baseUrl);
            String requestUrl = UriComponentsBuilder.fromHttpUrl(normalizedBaseUrl)
                .queryParam("function", "GLOBAL_QUOTE")
                .queryParam("symbol", "IBM")
                .queryParam("apikey", apiKey)
                .build(true)
                .toUriString();

            String response = WebClient.create()
                .get()
                .uri(requestUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(20))
                .block();

            if (response == null || response.trim().isEmpty()) {
                return new ValidationResult(false, "Sunucudan yanit alinamadi");
            }

            JsonNode root = objectMapper.readTree(response);

            if (root.has("Error Message")) {
                return new ValidationResult(false, "Gecersiz API anahtari veya endpoint hatasi");
            }

            JsonNode globalQuote = root.path("Global Quote");
            if (!globalQuote.isMissingNode() && globalQuote.has("01. symbol")) {
                return new ValidationResult(true, "API anahtari gecerli");
            }

            if (root.has("Note")) {
                return new ValidationResult(true, "API yaniti alindi (rate limit notu olabilir), anahtar kaydedilebilir");
            }

            if (root.has("Information")) {
                return new ValidationResult(true, "API bilgi notu dondu, anahtar kaydedilebilir");
            }

            return new ValidationResult(false, "Beklenmeyen yanit formati");

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                return new ValidationResult(true, "Rate limit yaniti alindi, anahtar kaydedilebilir");
            }
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                return new ValidationResult(false, "Gecersiz API anahtari");
            }
            log.warn("Alpha Vantage HTTP validation error: {}", e.getMessage());
            return new ValidationResult(false, "HTTP hatasi: " + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Alpha Vantage validation error: {}", e.getMessage());
            return new ValidationResult(false, "Baglanti/dogrulama hatasi: " + e.getMessage());
        }
    }

    private ValidationResult validateYahooFinance(String baseUrl) {
        try {
            validateUrl(baseUrl); // SSRF check
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
            validateUrl(baseUrl); // SSRF check
            String normalizedBaseUrl = normalizeFinnhubBaseUrl(baseUrl);
            String stockRequestUrl = UriComponentsBuilder.fromHttpUrl(normalizedBaseUrl)
                .path("/quote")
                .queryParam("symbol", "AAPL")
                .queryParam("token", apiKey)
                .build(true)
                .toUriString();

            String stockResponse = WebClient.create()
                .get()
                .uri(stockRequestUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();

            if (stockResponse == null || stockResponse.trim().isEmpty()) {
                return new ValidationResult(false, "Sunucudan yanit alinamadi");
            }

            JsonNode stockRoot = objectMapper.readTree(stockResponse);
            if (stockRoot.has("error")) {
                return new ValidationResult(false, "Gecersiz Finnhub API anahtari");
            }

            if (!stockRoot.has("c")) {
                return new ValidationResult(false, "Beklenmeyen Finnhub yanit formati");
            }

            String forexRequestUrl = UriComponentsBuilder.fromHttpUrl(normalizedBaseUrl)
                .path("/forex/rates")
                .queryParam("base", "USD")
                .queryParam("token", apiKey)
                .build(true)
                .toUriString();
            try {
                String forexResponse = WebClient.create()
                    .get()
                    .uri(forexRequestUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

                if (forexResponse != null && !forexResponse.isBlank()) {
                    JsonNode forexRoot = objectMapper.readTree(forexResponse);
                    if (forexRoot.has("error")) {
                        String error = forexRoot.get("error").asText("").toLowerCase();
                        if (error.contains("don't have access") || error.contains("not have access")) {
                            return new ValidationResult(true,
                                "API anahtari gecerli, ancak Forex endpoint erisimi planinizda kapali olabilir");
                        }
                    }
                }
            } catch (Exception forexError) {
                log.debug("Finnhub forex validation step skipped: {}", forexError.getMessage());
            }

            return new ValidationResult(true, "API anahtari gecerli");

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                return new ValidationResult(true, "Rate limit yaniti alindi, anahtar kaydedilebilir");
            }
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                return new ValidationResult(false, "Gecersiz Finnhub API anahtari");
            }
            log.warn("Finnhub HTTP validation error: {}", e.getMessage());
            return new ValidationResult(false, "HTTP hatasi: " + e.getStatusCode().value());
        } catch (Exception e) {
            log.warn("Finnhub validation error: {}", e.getMessage());
            return new ValidationResult(false, "Baglanti hatasi: " + e.getMessage());
        }
    }

    private ValidationResult validateFintables(String apiKey, String baseUrl) {
        validateUrl(baseUrl); // SSRF check
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

    private ValidationResult validateBistDataStore(String baseUrl) {
        try {
            validateUrl(baseUrl); // SSRF check
            String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
            if (normalizedBaseUrl.isBlank()) {
                return new ValidationResult(false, "BIST DataStore base URL bos olamaz");
            }
            byte[] response = WebClient.builder()
                .baseUrl(normalizedBaseUrl)
                .defaultHeader("User-Agent", "MintStack-Finance-Portal/1.0")
                .build()
                .get()
                .uri("/files/datafilepaths_viop.zip")
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(10))
                .block();

            if (response == null || response.length == 0) {
                return new ValidationResult(false, "BIST DataStore yaniti bos");
            }
            return new ValidationResult(true, "BIST DataStore public dosya kaynagi erisilebilir");
        } catch (Exception error) {
            log.warn("BIST DataStore validation error: {}", error.getMessage());
            return new ValidationResult(false, "BIST DataStore baglanti hatasi: " + error.getMessage());
        }
    }

    /**
     * SSRF Protection: Validate URL before making HTTP requests.
     * Prevents attacks via internal hosts, localhost, or metadata endpoints.
     *
     * @param url The URL to validate
     * @throws SecurityException if the URL points to a blocked/internal host
     */
    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new SecurityException("URL cannot be null or empty");
        }

        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost().toLowerCase();

            // Check explicit blocklist
            if (BLOCKED_HOSTS.contains(host)) {
                throw new SecurityException("Blocked host: " + host);
            }

            // Resolve hostname to IP and check for internal addresses
            InetAddress address = InetAddress.getByName(parsedUrl.getHost());
            byte[] bytes = address.getAddress();

            // Block loopback
            if (address.isLoopbackAddress()) {
                throw new SecurityException("Loopback address blocked: " + host);
            }

            // Block link-local (169.254.x.x - AWS metadata, etc.)
            if (address.isLinkLocalAddress()) {
                throw new SecurityException("Link-local address blocked: " + host);
            }

            // Block private IP ranges: 10.x.x.x, 172.16-31.x.x, 192.168.x.x
            if (bytes.length == 4) {
                int first = bytes[0] & 0xFF;
                int second = bytes[1] & 0xFF;

                if (first == 10) {
                    throw new SecurityException("Private IP blocked (10.x.x.x): " + host);
                }
                if (first == 172 && (second >= 16 && second <= 31)) {
                    throw new SecurityException("Private IP blocked (172.16-31.x.x): " + host);
                }
                if (first == 192 && second == 168) {
                    throw new SecurityException("Private IP blocked (192.168.x.x): " + host);
                }
            }

            // Block IPv6 internal addresses
            if (bytes.length == 16) {
                // Site-local (fc00::/7)
                if ((bytes[0] & 0xFE) == 0xFC) {
                    throw new SecurityException("IPv6 site-local blocked: " + host);
                }
            }

            // Require HTTPS for external URLs (except localhost in dev)
            String protocol = parsedUrl.getProtocol().toLowerCase();
            if (!protocol.equals("https") && !protocol.equals("http")) {
                throw new SecurityException("Only HTTP/HTTPS protocols allowed");
            }

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Invalid URL: " + url, e);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }

        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeAlphaVantageBaseUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        String lowercase = normalized.toLowerCase();

        if (lowercase.contains("alphavantage.co")
            && !lowercase.endsWith("/query")
            && !lowercase.contains("/query?")) {
            return normalized + "/query";
        }

        return normalized;
    }

    private String normalizeFinnhubBaseUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        String lowercase = normalized.toLowerCase();

        if (!lowercase.contains("finnhub.io")) {
            return normalized;
        }

        if (lowercase.endsWith("/api/v1") || lowercase.contains("/api/v1?")) {
            return normalized;
        }

        if (lowercase.endsWith("/api")) {
            return normalized + "/v1";
        }

        return normalized + "/api/v1";
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
