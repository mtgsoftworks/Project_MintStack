package com.mintstack.finance.service.external;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.CurrencyRate.RateSource;
import com.mintstack.finance.exception.ExternalApiException;
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

    /**
     * Fetch today's currency rates from TCMB
     */
    public List<CurrencyRate> fetchTodayRates() {
        return fetchRates(LocalDate.now());
    }

    /**
     * Fetch currency rates for a specific date from TCMB
     */
    public List<CurrencyRate> fetchRates(LocalDate date) {
        try {
            String url = buildUrl(date);
            log.info("Fetching TCMB rates from: {}", url);

            String xmlResponse = tcmbWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (xmlResponse == null || xmlResponse.isEmpty()) {
                throw new ExternalApiException("TCMB", "Boş yanıt alındı");
            }

            return parseXmlResponse(xmlResponse, date);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching TCMB rates", e);
            throw new ExternalApiException("TCMB", "Kur bilgisi alınamadı", e);
        }
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
