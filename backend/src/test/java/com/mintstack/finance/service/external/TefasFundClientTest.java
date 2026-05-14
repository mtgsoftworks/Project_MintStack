package com.mintstack.finance.service.external;

import com.mintstack.finance.exception.ExternalApiException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TefasFundClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    void fetchFundPrices_ShouldParseAndNormalizeRows() throws Exception {
        startServer(exchange -> writeJson(exchange, 200, """
            {
              "errorCode": null,
              "errorMessage": null,
              "resultList": [
                {
                  "fonKodu": "aal",
                  "fonUnvan": "ATA PORTFOY MONEY MARKET FUND",
                  "tarih": "14.05.2026",
                  "fiyat": "3,230941",
                  "tedPaySayisi": "970422172",
                  "kisiSayisi": 4850,
                  "portfoyBuyukluk": 3135376383.22,
                  "borsaBultenFiyat": null
                },
                {
                  "fonKodu": "SKIP",
                  "fonUnvan": "INVALID FUND",
                  "tarih": "2026-05-14",
                  "fiyat": 0
                }
              ]
            }
            """));

        TefasFundClient client = createClient("/fonGnlBlgSiraliGetir", "YAT", "");
        List<TefasFundClient.TefasFundPrice> prices = client.fetchFundPrices(LocalDate.of(2026, 5, 14));

        assertThat(prices).hasSize(1);
        TefasFundClient.TefasFundPrice price = prices.get(0);
        assertThat(price.fundCode()).isEqualTo("AAL");
        assertThat(price.fundName()).isEqualTo("ATA PORTFOY MONEY MARKET FUND");
        assertThat(price.fundKind()).isEqualTo("YAT");
        assertThat(price.date()).isEqualTo(LocalDate.of(2026, 5, 14));
        assertThat(price.price()).isEqualByComparingTo("3.230941");
        assertThat(price.sharesOutstanding()).isEqualByComparingTo("970422172");
        assertThat(price.investorCount()).isEqualByComparingTo("4850");
        assertThat(price.portfolioSize()).isEqualByComparingTo("3135376383.22");
        assertThat(price.exchangeBulletinPrice()).isNull();
    }

    @Test
    void fetchFundPrices_ShouldReturnEmpty_WhenTefasRespondsWithKnownOutOfBoundsError() throws Exception {
        startServer(exchange -> writeJson(exchange, 200, """
            {
              "errorCode": null,
              "errorMessage": "Index 0 out of bounds for length 0",
              "resultList": []
            }
            """));

        TefasFundClient client = createClient("/fonGnlBlgSiraliGetir", "YAT", "");
        List<TefasFundClient.TefasFundPrice> prices = client.fetchFundPrices(LocalDate.of(2026, 5, 14));

        assertThat(prices).isEmpty();
    }

    @Test
    void fetchFundPrices_ShouldUseConfiguredEndpoint_WhenEndpointHasNoLeadingSlash() throws Exception {
        AtomicInteger hitCounter = new AtomicInteger(0);
        startServer(exchange -> {
            hitCounter.incrementAndGet();
            writeJson(exchange, 200, """
                {
                  "errorCode": null,
                  "errorMessage": null,
                  "resultList": []
                }
                """);
        });

        TefasFundClient client = createClient("fonGnlBlgSiraliGetir", "YAT", "");
        client.fetchFundPrices(LocalDate.of(2026, 5, 14));

        assertThat(hitCounter.get()).isEqualTo(1);
    }

    @Test
    void fetchLatestFundPrices_ShouldFallbackToPreviousDay_WhenCurrentDayIsEmpty() {
        TefasFundClient client = spy(createClientWithoutServer());
        LocalDate today = LocalDate.now();
        LocalDate previousDay = today.minusDays(1);
        TefasFundClient.TefasFundPrice previousDayPrice = samplePrice(previousDay, new BigDecimal("5.10"));

        doReturn(List.of()).when(client).fetchFundPrices(any(LocalDate.class));
        doReturn(List.of()).when(client).fetchFundPrices(today);
        doReturn(List.of(previousDayPrice)).when(client).fetchFundPrices(previousDay);

        List<TefasFundClient.TefasFundPrice> result = client.fetchLatestFundPrices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).date()).isEqualTo(previousDay);
        InOrder order = inOrder(client);
        order.verify(client).fetchFundPrices(today);
        order.verify(client).fetchFundPrices(previousDay);
    }

    @Test
    void fetchLatestFundPrices_ShouldContinue_WhenSingleAttemptThrowsException() {
        TefasFundClient client = spy(createClientWithoutServer());
        LocalDate today = LocalDate.now();
        LocalDate previousDay = today.minusDays(1);
        TefasFundClient.TefasFundPrice previousDayPrice = samplePrice(previousDay, new BigDecimal("7.25"));

        doReturn(List.of()).when(client).fetchFundPrices(any(LocalDate.class));
        doThrow(new ExternalApiException("TEFAS", "temporary error", new RuntimeException("timeout")))
            .when(client).fetchFundPrices(today);
        doReturn(List.of(previousDayPrice)).when(client).fetchFundPrices(previousDay);

        List<TefasFundClient.TefasFundPrice> result = client.fetchLatestFundPrices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).date()).isEqualTo(previousDay);
    }

    @Test
    void fetchLatestFundPrices_ShouldReturnEmpty_AfterFiveAttemptsWithoutData() {
        TefasFundClient client = spy(createClientWithoutServer());
        LocalDate startDate = LocalDate.now();
        doReturn(List.of()).when(client).fetchFundPrices(any(LocalDate.class));

        List<TefasFundClient.TefasFundPrice> result = client.fetchLatestFundPrices();

        assertThat(result).isEmpty();
        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(client, times(5)).fetchFundPrices(captor.capture());
        List<LocalDate> attemptedDates = captor.getAllValues();
        assertThat(attemptedDates.get(0)).isEqualTo(startDate);
        assertThat(attemptedDates.get(4)).isEqualTo(startDate.minusDays(4));
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/funds/fonGnlBlgSiraliGetir", handler);
        server.start();
    }

    private TefasFundClient createClient(String endpoint, String fundKinds, String fundCodes) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/funds";
        return createClient(baseUrl, endpoint, fundKinds, fundCodes);
    }

    private TefasFundClient createClientWithoutServer() {
        return createClient("http://localhost:65535/api/funds", "/fonGnlBlgSiraliGetir", "YAT", "");
    }

    private TefasFundClient createClient(String baseUrl, String endpoint, String fundKinds, String fundCodes) {
        WebClient webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
        TefasFundClient client = new TefasFundClient(webClient);
        ReflectionTestUtils.setField(client, "defaultFundKinds", fundKinds);
        ReflectionTestUtils.setField(client, "fundCodes", fundCodes);
        ReflectionTestUtils.setField(client, "fundListEndpoint", endpoint);
        return client;
    }

    private TefasFundClient.TefasFundPrice samplePrice(LocalDate date, BigDecimal value) {
        return new TefasFundClient.TefasFundPrice(
            "AAA",
            "AAA FUND",
            "YAT",
            date,
            value,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.TEN,
            null
        );
    }

    private void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(response);
        } finally {
            exchange.close();
        }
    }
}
