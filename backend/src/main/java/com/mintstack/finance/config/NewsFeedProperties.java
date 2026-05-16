package com.mintstack.finance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.news")
public class NewsFeedProperties {

    private boolean fetchWhenSimulationEnabled = true;
    private boolean simulationNewsEnabled = false;
    private int maxItemsPerFeed = 10;
    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 10000;
    private boolean articlePreviewEnabled = true;
    private int articlePreviewConnectTimeoutMs = 4000;
    private int articlePreviewReadTimeoutMs = 7000;
    private int articlePreviewMaxBytes = 350000;
    private List<Feed> feeds = new ArrayList<>();
    private Llm llm = new Llm();

    @Data
    public static class Feed {
        private String code;
        private String url;
        private String categorySlug;
        private String sourceName;
        private boolean enabled = true;
        private int priority = 100;
    }

    @Data
    public static class Llm {
        private boolean enabled = false;
        private String baseUrl = "";
        private String endpoint = "/v1/chat/completions";
        private String model = "gpt-4.1-mini";
        private String apiKey = "";
        private String apiKeyHeader = "Authorization";
        private String apiKeyPrefix = "Bearer ";
        private int timeoutMs = 15000;
        private int maxInputChars = 4000;
        private double temperature = 0.2d;
    }
}
