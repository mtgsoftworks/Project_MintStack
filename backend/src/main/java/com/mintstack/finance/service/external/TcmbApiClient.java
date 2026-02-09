package com.mintstack.finance.service.external;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.CurrencyRate.RateSource;
import com.mintstack.finance.exception.ExternalApiException;
import com.mintstack.finance.repository.CurrencyRateRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TcmbApiClient {

    private final WebClient tcmbWebClient;
    private final CurrencyRateRepository currencyRateRepository;

    /**
     * Fetch today's currency rates from TCMB
     * Falls back to previous business day if today's rates not available
     */
    @CircuitBreaker(name = "tcmbApi", fallbackMethod = "fetchTodayRatesFallback")
    @Retry(name = "externalApi")
    public List<CurrencyRate> fetchTodayRates() {
        return fetchRates(LocalDate.now());
    }

    /**
     * Fallback method for fetchTodayRates - returns cached rates from DB
     */
    public List<CurrencyRate> fetchTodayRatesFallback(Exception e) {
        log.warn("TCMB API fallback triggered: {}", e.getMessage());
        return currencyRateRepository.findBySourceOrderByFetchedAtDesc(RateSource.TCMB)
            .stream()
            .limit(50)
            .toList();
    }

    /**
     * Fetch currency rates for a specific date from TCMB
     * If 404, tries previous days (TCMB doesn't publish on weekends/holidays)
     */
    @CircuitBreaker(name = "tcmbApi", fallbackMethod = "fetchRatesFallback")
    public List<CurrencyRate> fetchRates(LocalDate date) {
        int maxRetries = 5; // Try up to 5 previous days
        LocalDate currentDate = date;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                String url = buildUrl(currentDate);
                log.info("Fetching TCMB rates from: {} (attempt {})", url, attempt + 1);

                String xmlResponse = tcmbWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

                if (xmlResponse == null || xmlResponse.isEmpty()) {
                    throw new ExternalApiException("TCMB", "Boş yanıt alındı");
                }

                return parseXmlResponse(xmlResponse, currentDate);
            } catch (Exception e) {
                // Check if it's a 404 error (weekend/holiday - no rates published)
                if (e.getMessage() != null && e.getMessage().contains("404")) {
                    log.debug("TCMB rates not available for {}, trying previous day", currentDate);
                    currentDate = currentDate.minusDays(1);
                    continue;
                }
                
                // For other errors, throw immediately
                if (e instanceof ExternalApiException) {
                    throw (ExternalApiException) e;
                }
                log.error("Error fetching TCMB rates", e);
                throw new ExternalApiException("TCMB", "Kur bilgisi alınamadı", e);
            }
        }
        
        throw new ExternalApiException("TCMB", "Son " + maxRetries + " gün için kur bilgisi bulunamadı");
    }

    /**
     * Fallback method for fetchRates - returns cached rates from DB
     */
    public List<CurrencyRate> fetchRatesFallback(LocalDate date, Exception e) {
        log.warn("TCMB API fallback triggered for date {}: {}", date, e.getMessage());
        return currencyRateRepository.findBySourceOrderByFetchedAtDesc(RateSource.TCMB)
            .stream()
            .limit(50)
            .toList();
    }

    private String buildUrl(LocalDate date) {
        // TCMB URL format: /kurlar/YYYYMM/DDMMYYYY.xml
        String yearMonth = date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String dateStr = date.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        return "/" + yearMonth + "/" + dateStr + ".xml";
    }

    private List<CurrencyRate> parseXmlResponse(String xml, LocalDate rateDate) {
        List<CurrencyRate> rates = new ArrayList<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            
            NodeList currencyNodes = document.getElementsByTagName("Currency");
            LocalDateTime fetchedAt = LocalDateTime.now();
            
            for (int i = 0; i < currencyNodes.getLength(); i++) {
                Element currency = (Element) currencyNodes.item(i);
                
                String currencyCode = currency.getAttribute("CurrencyCode");
                String currencyName = getElementText(currency, "CurrencyName");
                String forexBuying = getElementText(currency, "ForexBuying");
                String forexSelling = getElementText(currency, "ForexSelling");
                String banknoteBuying = getElementText(currency, "BanknoteBuying");
                String banknoteSelling = getElementText(currency, "BanknoteSelling");
                
                // Skip if no rates available
                if (forexBuying == null || forexBuying.isEmpty()) {
                    continue;
                }
                
                CurrencyRate rate = CurrencyRate.builder()
                    .currencyCode(currencyCode)
                    .currencyName(currencyName)
                    .buyingRate(parseBigDecimal(forexBuying))
                    .sellingRate(parseBigDecimal(forexSelling))
                    .effectiveBuyingRate(parseBigDecimal(banknoteBuying))
                    .effectiveSellingRate(parseBigDecimal(banknoteSelling))
                    .source(RateSource.TCMB)
                    .fetchedAt(fetchedAt)
                    .rateDate(rateDate.atStartOfDay())
                    .build();
                
                rates.add(rate);
                log.debug("Parsed rate: {} = {}/{}", currencyCode, forexBuying, forexSelling);
            }
            
            log.info("Parsed {} currency rates from TCMB", rates.size());
            return rates;
        } catch (Exception e) {
            log.error("Error parsing TCMB XML response", e);
            throw new ExternalApiException("TCMB", "XML yanıtı ayrıştırılamadı", e);
        }
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
