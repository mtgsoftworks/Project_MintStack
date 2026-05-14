package com.mintstack.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.NewsFeedProperties;
import com.mintstack.finance.entity.News;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.repository.UserApiConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsEnrichmentService {

    private static final String DEFAULT_MODEL = "openai/gpt-5";
    private static final String DEFAULT_ENDPOINT = "/v1/chat/completions";

    private final NewsFeedProperties newsFeedProperties;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final UserApiConfigRepository userApiConfigRepository;

    public News enrichIfEnabled(News news) {
        if (news == null || !StringUtils.hasText(news.getTitle())) {
            return news;
        }

        LlmRuntimeConfig llm = resolveRuntimeConfig();
        if (!llm.enabled()) {
            return news;
        }
        if (!StringUtils.hasText(llm.baseUrl())) {
            log.debug("LLM enrichment is enabled but app.news.llm.base-url is empty. Skipping enrichment.");
            return news;
        }

        try {
            EnrichmentResult result = requestEnrichment(news, llm);
            if (result == null) {
                return news;
            }
            applyEnrichment(news, result, llm);
        } catch (Exception e) {
            log.warn("LLM enrichment failed for news '{}': {}", news.getTitle(), e.getMessage());
        }

        return news;
    }

    private LlmRuntimeConfig resolveRuntimeConfig() {
        NewsFeedProperties.Llm configured = newsFeedProperties.getLlm() != null
            ? newsFeedProperties.getLlm()
            : new NewsFeedProperties.Llm();

        Optional<UserApiConfig> activeConfig = userApiConfigRepository
            .findTopByProviderAndIsActiveTrueOrderByUpdatedAtDesc(UserApiConfig.ApiProvider.LLM_ENRICHMENT);

        boolean enabled = configured.isEnabled() || activeConfig.isPresent();
        String baseUrl = trimToNull(configured.getBaseUrl());
        String model = trimToNull(configured.getModel());
        String apiKey = trimToNull(configured.getApiKey());

        if (activeConfig.isPresent()) {
            UserApiConfig llmConfig = activeConfig.get();
            if (StringUtils.hasText(llmConfig.getBaseUrl())) {
                baseUrl = llmConfig.getBaseUrl().trim();
            }
            if (StringUtils.hasText(llmConfig.getApiKey())) {
                apiKey = llmConfig.getApiKey().trim();
            }
            if (StringUtils.hasText(llmConfig.getSecretKey())) {
                model = llmConfig.getSecretKey().trim();
            }
        }

        String endpoint = resolveEndpoint(baseUrl, configured.getEndpoint());
        return new LlmRuntimeConfig(
            enabled,
            baseUrl,
            endpoint,
            defaultIfBlank(model, DEFAULT_MODEL),
            apiKey,
            defaultIfBlank(configured.getApiKeyHeader(), "Authorization"),
            defaultIfBlank(configured.getApiKeyPrefix(), "Bearer "),
            Math.max(configured.getTimeoutMs(), 3000),
            configured.getMaxInputChars(),
            configured.getTemperature()
        );
    }

    private String resolveEndpoint(String baseUrl, String configuredEndpoint) {
        String endpoint = defaultIfBlank(configuredEndpoint, DEFAULT_ENDPOINT);
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        // GitHub Models OpenAI-compatible endpoint uses /chat/completions (without /v1).
        if (isGitHubModelsBaseUrl(baseUrl) && DEFAULT_ENDPOINT.equals(endpoint)) {
            return "/chat/completions";
        }
        return endpoint;
    }

    private boolean isGitHubModelsBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return false;
        }
        return baseUrl.trim().toLowerCase(Locale.ROOT).contains("models.github.ai/inference");
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private EnrichmentResult requestEnrichment(News news, LlmRuntimeConfig llm) {
        String promptInput = buildPromptInput(news, llm.maxInputChars());

        Map<String, Object> request = Map.of(
            "model", llm.model(),
            "temperature", llm.temperature(),
            "messages", List.of(
                Map.of(
                    "role", "system",
                    "content", """
                        You are a finance news enrichment engine.
                        Return only JSON object with keys:
                        summary (max 280 chars, Turkish),
                        sentiment (POSITIVE|NEGATIVE|NEUTRAL),
                        keywords (array of max 6 short Turkish keywords),
                        importance (LOW|MEDIUM|HIGH),
                        reason (max 200 chars, Turkish).
                        """
                ),
                Map.of("role", "user", "content", promptInput)
            )
        );

        WebClient client = webClientBuilder
            .baseUrl(llm.baseUrl().trim())
            .defaultHeader("Accept", "application/json")
            .build();

        WebClient.RequestBodySpec requestSpec = client.post()
            .uri(llm.endpoint())
            .contentType(MediaType.APPLICATION_JSON);

        if (StringUtils.hasText(llm.apiKey())) {
            String headerName = defaultIfBlank(llm.apiKeyHeader(), "Authorization");
            String prefix = defaultIfBlank(llm.apiKeyPrefix(), "Bearer ");
            String rawKey = llm.apiKey().trim();
            String headerValue = rawKey.startsWith(prefix) ? rawKey : prefix + rawKey;
            requestSpec = requestSpec.header(headerName, headerValue);
        }

        JsonNode response = requestSpec
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(Duration.ofMillis(llm.timeoutMs()))
            .block();

        if (response == null) {
            return null;
        }

        String messageContent = extractMessageContent(response);
        if (!StringUtils.hasText(messageContent)) {
            return null;
        }

        return parseResult(messageContent);
    }

    private String buildPromptInput(News news, int maxChars) {
        StringBuilder builder = new StringBuilder(512);
        builder.append("Title: ").append(defaultIfBlank(news.getTitle(), "")).append('\n');
        builder.append("Source: ").append(defaultIfBlank(news.getSourceName(), "")).append('\n');
        builder.append("Summary: ").append(defaultIfBlank(news.getSummary(), "")).append('\n');
        builder.append("Content: ").append(defaultIfBlank(news.getContent(), ""));

        String text = builder.toString().trim();
        if (text.length() <= maxChars || maxChars <= 64) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    private String extractMessageContent(JsonNode response) {
        JsonNode choices = response.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            String value = choices.get(0).path("message").path("content").asText(null);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return response.path("content").asText(null);
    }

    private EnrichmentResult parseResult(String llmContent) {
        try {
            String jsonText = extractJsonObject(llmContent);
            JsonNode node = objectMapper.readTree(jsonText);
            String summary = node.path("summary").asText(null);
            String sentiment = node.path("sentiment").asText(null);
            String importance = node.path("importance").asText(null);
            String reason = node.path("reason").asText(null);
            String keywords = normalizeKeywords(node.path("keywords"));
            return new EnrichmentResult(summary, sentiment, keywords, importance, reason);
        } catch (Exception e) {
            log.debug("LLM content parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractJsonObject(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned
                .replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("\\s*```$", "");
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String normalizeKeywords(JsonNode keywordsNode) {
        Set<String> values = new LinkedHashSet<>();
        if (keywordsNode == null || keywordsNode.isNull()) {
            return null;
        }

        if (keywordsNode.isArray()) {
            for (JsonNode node : keywordsNode) {
                String value = node.asText("").trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        } else {
            String raw = keywordsNode.asText("");
            for (String token : raw.split(",")) {
                String value = token.trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        }

        if (values.isEmpty()) {
            return null;
        }

        List<String> ordered = new ArrayList<>(values);
        if (ordered.size() > 6) {
            ordered = ordered.subList(0, 6);
        }
        return String.join(", ", ordered);
    }

    private void applyEnrichment(News news, EnrichmentResult result, LlmRuntimeConfig llm) {
        String llmSummary = shrink(result.summary(), 1000);
        String llmSentiment = normalizeSentiment(result.sentiment());
        String llmKeywords = shrink(result.keywords(), 500);
        String llmModel = shrink(defaultIfBlank(llm.model(), DEFAULT_MODEL), 120);

        if (StringUtils.hasText(llmSummary)) {
            news.setLlmSummary(llmSummary);
            // Keep existing summary if available; otherwise expose enriched summary directly.
            if (!StringUtils.hasText(news.getSummary())) {
                news.setSummary(llmSummary);
            }
        }
        if (StringUtils.hasText(llmSentiment)) {
            news.setLlmSentiment(llmSentiment);
        }
        if (StringUtils.hasText(llmKeywords)) {
            news.setLlmKeywords(llmKeywords);
        }
        news.setLlmModel(llmModel);
        news.setLlmEnrichedAt(LocalDateTime.now());

        if (StringUtils.hasText(result.importance()) || StringUtils.hasText(result.reason())) {
            StringBuilder enrichmentNote = new StringBuilder();
            if (StringUtils.hasText(result.importance())) {
                enrichmentNote.append("Importance=").append(result.importance().trim());
            }
            if (StringUtils.hasText(result.reason())) {
                if (!enrichmentNote.isEmpty()) {
                    enrichmentNote.append(" | ");
                }
                enrichmentNote.append("Reason=").append(result.reason().trim());
            }

            String existing = defaultIfBlank(news.getContent(), "");
            String suffix = "\n\n[LLM_ENRICHMENT] " + shrink(enrichmentNote.toString(), 280);
            news.setContent(shrink(existing + suffix, 16000));
        }
    }

    private String normalizeSentiment(String sentiment) {
        if (!StringUtils.hasText(sentiment)) {
            return null;
        }
        String normalized = sentiment.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "POSITIVE", "NEGATIVE", "NEUTRAL" -> normalized;
            default -> "NEUTRAL";
        };
    }

    private String shrink(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record EnrichmentResult(
        String summary,
        String sentiment,
        String keywords,
        String importance,
        String reason
    ) {
    }

    private record LlmRuntimeConfig(
        boolean enabled,
        String baseUrl,
        String endpoint,
        String model,
        String apiKey,
        String apiKeyHeader,
        String apiKeyPrefix,
        int timeoutMs,
        int maxInputChars,
        double temperature
    ) {
    }
}
