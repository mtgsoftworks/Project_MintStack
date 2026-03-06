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
    private List<Feed> feeds = new ArrayList<>();

    @Data
    public static class Feed {
        private String code;
        private String url;
        private String categorySlug;
        private String sourceName;
        private boolean enabled = true;
        private int priority = 100;
    }
}

