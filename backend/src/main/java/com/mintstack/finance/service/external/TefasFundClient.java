package com.mintstack.finance.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.mintstack.finance.exception.ExternalApiException;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Observed(name = "external.tefas", contextualName = "tefas-fund-client")
public class TefasFundClient {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TURKISH_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final WebClient tefasWebClient;

    @Value("${app.external-api.tefas.default-fund-kinds:YAT,BYF}")
    private String defaultFundKinds;

    @Value("${app.external-api.tefas.fund-codes:}")
    private String fundCodes;

    @Value("${app.external-api.tefas.fund-list-endpoint:/fonGnlBlgSiraliGetir}")
    private String fundListEndpoint;

    public List<TefasFundPrice> fetchLatestFundPrices() {
        LocalDate queryDate = LocalDate.now();
        for (int attempt = 0; attempt < 5; attempt++) {
            List<TefasFundPrice> prices;
            try {
                prices = fetchFundPrices(queryDate);
            } catch (ExternalApiException error) {
                log.warn("TEFAS fetch failed for date {}: {}", queryDate, error.getMessage());
                queryDate = queryDate.minusDays(1);
                continue;
            }
            if (!prices.isEmpty()) {
                return prices;
            }
            queryDate = queryDate.minusDays(1);
        }
        return List.of();
    }

    public List<TefasFundPrice> fetchFundPrices(LocalDate date) {
        List<String> kinds = splitCsv(defaultFundKinds, List.of("YAT", "BYF"));
        List<String> codes = splitCsv(fundCodes, List.of());
        List<TefasFundPrice> result = new ArrayList<>();

        for (String kind : kinds) {
            if (codes.isEmpty()) {
                try {
                    result.addAll(fetchSingle(date, kind, null));
                } catch (ExternalApiException error) {
                    log.debug("TEFAS kind {} skipped: {}", kind, error.getMessage());
                }
                continue;
            }
            for (String code : codes) {
                try {
                    result.addAll(fetchSingle(date, kind, code));
                } catch (ExternalApiException error) {
                    log.debug("TEFAS kind/code {}/{} skipped: {}", kind, code, error.getMessage());
                }
            }
        }
        return result;
    }

    private List<TefasFundPrice> fetchSingle(LocalDate date, String kind, String fundCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("fonTipi", kind);
        body.put("fonKodu", fundCode);
        body.put("aramaMetni", null);
        body.put("fonTurKod", null);
        body.put("fonGrubu", null);
        body.put("sfonTurKod", null);
        body.put("fonTurAciklama", null);
        body.put("kurucuKod", null);
        body.put("basTarih", date.format(BASIC_DATE));
        body.put("bitTarih", date.format(BASIC_DATE));
        body.put("basSira", 1);
        body.put("bitSira", 100000);
        body.put("dil", "TR");
        body.put("fonKod", "");
        body.put("fonGrup", "");
        body.put("fonUnvanTip", "");

        try {
            JsonNode response = tefasWebClient.post()
                .uri(resolveEndpointPath())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
            return parseResponse(response, kind, date);
        } catch (Exception error) {
            log.warn("TEFAS fund fetch failed for date={}, kind={}, fundCode={}: {}",
                date, kind, fundCode, error.getMessage());
            throw new ExternalApiException("TEFAS", "Fon fiyatlari alinamadi", error);
        }
    }

    private List<TefasFundPrice> parseResponse(JsonNode response, String kind, LocalDate fallbackDate) {
        if (response == null || response.isMissingNode()) {
            return List.of();
        }
        String errorMessage = response.path("errorMessage").asText("");
        if (!errorMessage.isBlank()) {
            String lower = errorMessage.toLowerCase(Locale.ROOT);
            if (lower.contains("out of bounds") || lower.contains("veri bulunamad")) {
                return List.of();
            }
            throw new ExternalApiException("TEFAS", "TEFAS API error: " + errorMessage);
        }

        JsonNode rows = response.path("resultList");
        if (!rows.isArray()) {
            return List.of();
        }

        List<TefasFundPrice> result = new ArrayList<>();
        for (JsonNode row : rows) {
            String code = row.path("fonKodu").asText("").trim().toUpperCase(Locale.ROOT);
            BigDecimal price = decimal(row.path("fiyat"));
            if (code.isBlank() || price == null || price.signum() <= 0) {
                continue;
            }
            result.add(new TefasFundPrice(
                code,
                row.path("fonUnvan").asText(code),
                kind,
                parseDate(row.path("tarih"), fallbackDate),
                price,
                decimal(row.path("tedPaySayisi")),
                decimal(row.path("kisiSayisi")),
                decimal(row.path("portfoyBuyukluk")),
                decimal(row.path("borsaBultenFiyat"))
            ));
        }
        return result;
    }

    private BigDecimal decimal(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText("").replace(",", ".").trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDate parseDate(JsonNode value, LocalDate fallbackDate) {
        String text = value == null ? "" : value.asText("").trim();
        if (text.isBlank()) {
            return fallbackDate;
        }
        for (DateTimeFormatter formatter : List.of(BASIC_DATE, ISO_DATE, TURKISH_DATE)) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next known TEFAS date representation.
            }
        }
        return fallbackDate;
    }

    private List<String> splitCsv(String csv, List<String> fallback) {
        if (csv == null || csv.isBlank()) {
            return fallback;
        }
        List<String> values = java.util.Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(value -> value.toUpperCase(Locale.ROOT))
            .distinct()
            .toList();
        return values.isEmpty() ? fallback : values;
    }

    private String resolveEndpointPath() {
        if (fundListEndpoint == null || fundListEndpoint.isBlank()) {
            return "/fonGnlBlgSiraliGetir";
        }
        return fundListEndpoint.startsWith("/") ? fundListEndpoint : "/" + fundListEndpoint;
    }

    public record TefasFundPrice(
        String fundCode,
        String fundName,
        String fundKind,
        LocalDate date,
        BigDecimal price,
        BigDecimal sharesOutstanding,
        BigDecimal investorCount,
        BigDecimal portfolioSize,
        BigDecimal exchangeBulletinPrice
    ) {
    }
}
