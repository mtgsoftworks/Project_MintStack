package com.mintstack.finance.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${app.external-api.tcmb.base-url}")
    private String tcmbBaseUrl;

    @Value("${app.external-api.tcmb.timeout}")
    private int tcmbTimeout;

    @Value("${app.external-api.yahoo-finance.base-url}")
    private String yahooFinanceBaseUrl;

    @Value("${app.external-api.yahoo-finance.timeout}")
    private int yahooFinanceTimeout;

    @Value("${app.external-api.alpha-vantage.base-url}")
    private String alphaVantageBaseUrl;

    @Value("${app.external-api.alpha-vantage.timeout}")
    private int alphaVantageTimeout;

    @Bean
    public WebClient tcmbWebClient() {
        return createWebClient(tcmbBaseUrl, tcmbTimeout);
    }

    @Bean
    public WebClient yahooFinanceWebClient() {
        return createWebClient(yahooFinanceBaseUrl, yahooFinanceTimeout);
    }

    @Bean
    public WebClient alphaVantageWebClient() {
        return createWebClient(alphaVantageBaseUrl, alphaVantageTimeout);
    }

    private WebClient createWebClient(String baseUrl, int timeoutMs) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
            .responseTimeout(Duration.ofMillis(timeoutMs))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(timeoutMs, TimeUnit.MILLISECONDS)));

        // Increase buffer size for large responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(16 * 1024 * 1024)) // 16MB
            .build();

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .defaultHeader("Accept", "application/json")
            .defaultHeader("User-Agent", "MintStack-Finance-Portal/1.0")
            .build();
    }
}
