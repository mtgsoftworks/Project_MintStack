package com.mintstack.finance.service.external;

import com.mintstack.finance.exception.ExternalApiException;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Observed(name = "external.bist-datastore", contextualName = "bist-datastore-client")
public class BistDataStoreClient {

    private static final Charset TURKISH_CHARSET = Charset.forName("windows-1254");
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final WebClient bistDataStoreWebClient;

    @Value("${app.external-api.bist-datastore.timeout:30000}")
    private int timeoutMs;

    @Value("${app.external-api.bist-datastore.bond-daily-bulletin-path-template:/data/ttb/{yyyy}/{MM}/ttb{yyyyMMdd}3.zip}")
    private String bondDailyBulletinPathTemplate;

    @Value("${app.external-api.bist-datastore.viop-daily-bulletin-path-template:/data/vadeli/viop_{yyyyMMdd}.csv}")
    private String viopDailyBulletinPathTemplate;

    @Value("${app.external-api.bist-datastore.health-path:/files/datafilepaths_viop.zip}")
    private String healthPath;

    public boolean isReachable() {
        try {
            byte[] body = fetchBytes(healthPath);
            return body != null && body.length > 0;
        } catch (Exception error) {
            log.warn("BIST DataStore reachability check failed: {}", error.getMessage());
            return false;
        }
    }

    public List<BistBondPrice> fetchBondPrices(LocalDate date) {
        String path = resolveDatedPath(bondDailyBulletinPathTemplate, date);
        try {
            byte[] zipped = fetchBytes(path);
            String csv = unzipFirstTextFile(zipped);
            return parseBondCsv(csv);
        } catch (WebClientResponseException.NotFound notFound) {
            log.debug("BIST DataStore bond bulletin not found for {} at {}", date, path);
            return List.of();
        } catch (Exception error) {
            throw new ExternalApiException("BIST DataStore", "Bond bulletin could not be fetched: " + date, error);
        }
    }

    public List<BistViopPrice> fetchViopPrices(LocalDate date) {
        String path = resolveDatedPath(viopDailyBulletinPathTemplate, date);
        try {
            byte[] csvBytes = fetchBytes(path);
            String csv = new String(csvBytes, TURKISH_CHARSET);
            return parseViopCsv(csv);
        } catch (WebClientResponseException.NotFound notFound) {
            log.debug("BIST DataStore VIOP bulletin not found for {} at {}", date, path);
            return List.of();
        } catch (Exception error) {
            throw new ExternalApiException("BIST DataStore", "VIOP bulletin could not be fetched: " + date, error);
        }
    }

    List<BistBondPrice> parseBondCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }

        Map<String, BistBondPrice> bySymbol = new LinkedHashMap<>();
        for (String[] row : dataRows(csv)) {
            String market = value(row, 0);
            String symbol = normalizeSymbol(value(row, 5));
            if (!isKesMarket(market) || !looksLikeIsin(symbol)) {
                continue;
            }

            LocalDate tradeDate = parseDate(value(row, 1));
            BigDecimal closePrice = firstPositive(decimal(row, 13), decimal(row, 15), decimal(row, 14));
            if (tradeDate == null || closePrice == null) {
                continue;
            }

            BistBondPrice item = new BistBondPrice(
                symbol,
                symbol,
                tradeDate,
                value(row, 6).isBlank() ? "TRY" : value(row, 6),
                closePrice,
                firstPositive(decimal(row, 18), decimal(row, 17)),
                firstPositive(decimal(row, 10), closePrice),
                decimal(row, 11),
                decimal(row, 12),
                firstPositive(decimal(row, 39), decimal(row, 30)),
                firstPositive(decimal(row, 40), decimal(row, 31))
            );
            bySymbol.merge(symbol, item, this::chooseMoreLiquidBond);
        }
        return List.copyOf(bySymbol.values());
    }

    List<BistViopPrice> parseViopCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }

        Map<String, BistViopPrice> bySymbol = new LinkedHashMap<>();
        for (String[] row : dataRows(csv)) {
            String symbol = normalizeSymbol(value(row, 1));
            if (symbol.isBlank() || symbol.length() > 20) {
                continue;
            }

            LocalDate tradeDate = parseDate(value(row, 0));
            BigDecimal settlementPrice = decimal(row, 9);
            if (tradeDate == null || settlementPrice == null || settlementPrice.signum() <= 0) {
                continue;
            }

            BistViopPrice item = new BistViopPrice(
                symbol,
                value(row, 2).isBlank() ? symbol : value(row, 2),
                tradeDate,
                "TRY",
                settlementPrice,
                decimal(row, 10),
                firstPositive(decimal(row, 12), settlementPrice),
                decimal(row, 13),
                decimal(row, 14),
                firstPositive(decimal(row, 15), settlementPrice),
                decimal(row, 11),
                decimal(row, 19),
                decimal(row, 17)
            );
            bySymbol.putIfAbsent(symbol, item);
        }
        return List.copyOf(bySymbol.values());
    }

    private byte[] fetchBytes(String path) {
        byte[] response = bistDataStoreWebClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(byte[].class)
            .timeout(Duration.ofMillis(timeoutMs))
            .block();
        if (response == null || response.length == 0) {
            throw new ExternalApiException("BIST DataStore", "Empty response for " + path);
        }
        return response;
    }

    private String unzipFirstTextFile(byte[] zipped) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipped), TURKISH_CHARSET)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                zipInputStream.transferTo(output);
                return output.toString(TURKISH_CHARSET);
            }
        }
        throw new ExternalApiException("BIST DataStore", "Zip archive did not contain a readable file");
    }

    private List<String[]> dataRows(String csv) {
        List<String[]> rows = new ArrayList<>();
        String normalized = csv.replace("\uFEFF", "");
        String[] lines = normalized.split("\\R");
        for (int index = 2; index < lines.length; index++) {
            String line = lines[index];
            if (line == null || line.isBlank()) {
                continue;
            }
            rows.add(line.split(";", -1));
        }
        return rows;
    }

    private String resolveDatedPath(String template, LocalDate date) {
        String basic = date.format(BASIC_DATE);
        return template
            .replace("{yyyyMMdd}", basic)
            .replace("{yyyy}", basic.substring(0, 4))
            .replace("{MM}", basic.substring(4, 6))
            .replace("{dd}", basic.substring(6, 8));
    }

    private BistBondPrice chooseMoreLiquidBond(BistBondPrice left, BistBondPrice right) {
        BigDecimal leftValue = left.tradedValue() == null ? BigDecimal.ZERO : left.tradedValue();
        BigDecimal rightValue = right.tradedValue() == null ? BigDecimal.ZERO : right.tradedValue();
        return rightValue.compareTo(leftValue) > 0 ? right : left;
    }

    private boolean isKesMarket(String market) {
        String normalized = market == null ? "" : market.toUpperCase(Locale.ROOT);
        return normalized.contains("KES") && !normalized.contains("REPO");
    }

    private boolean looksLikeIsin(String symbol) {
        return symbol != null
            && symbol.length() == 12
            && symbol.startsWith("TR")
            && symbol.chars().allMatch(Character::isLetterOrDigit);
    }

    private String normalizeSymbol(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String value(String[] row, int index) {
        if (row == null || index < 0 || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index].trim();
    }

    private BigDecimal decimal(String[] row, int index) {
        String text = value(row, index)
            .replace("%", "")
            .replace(",", ".")
            .trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null && value.signum() > 0) {
                return value;
            }
        }
        return null;
    }

    public record BistBondPrice(
        String symbol,
        String name,
        LocalDate date,
        String currency,
        BigDecimal closePrice,
        BigDecimal previousClose,
        BigDecimal openPrice,
        BigDecimal lowPrice,
        BigDecimal highPrice,
        BigDecimal quantity,
        BigDecimal tradedValue
    ) {
    }

    public record BistViopPrice(
        String symbol,
        String name,
        LocalDate date,
        String currency,
        BigDecimal settlementPrice,
        BigDecimal previousSettlementPrice,
        BigDecimal openPrice,
        BigDecimal lowPrice,
        BigDecimal highPrice,
        BigDecimal closingPrice,
        BigDecimal changePercent,
        BigDecimal tradeVolume,
        BigDecimal tradedValue
    ) {
    }
}
