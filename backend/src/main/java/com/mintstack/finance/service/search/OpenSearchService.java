package com.mintstack.finance.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchService {

    private final RestHighLevelClient client;

    private static final String LOGS_INDEX_PREFIX = "mintstack-logs-";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public void indexLog(String level, String message, String logger, String traceId, Map<String, Object> context) {
        String indexName = LOGS_INDEX_PREFIX + LocalDateTime.now().format(DATE_FORMAT);
        
        Map<String, Object> document = new HashMap<>();
        document.put("@timestamp", LocalDateTime.now().toString());
        document.put("level", level);
        document.put("message", message);
        document.put("logger", logger);
        document.put("traceId", traceId);
        document.put("service", "finance-portal");
        if (context != null) {
            document.put("context", context);
        }

        try {
            IndexRequest request = new IndexRequest(indexName)
                    .source(document, XContentType.JSON);
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            log.debug("Indexed log to {} with id {}", indexName, response.getId());
        } catch (IOException e) {
            log.error("Failed to index log: {}", e.getMessage());
        }
    }

    public void indexMarketData(String symbol, String type, Object data) {
        String indexName = "mintstack-market-data-" + LocalDateTime.now().format(DATE_FORMAT);
        
        Map<String, Object> document = new HashMap<>();
        document.put("@timestamp", LocalDateTime.now().toString());
        document.put("symbol", symbol);
        document.put("type", type);
        document.put("data", data);

        try {
            IndexRequest request = new IndexRequest(indexName)
                    .source(document, XContentType.JSON);
            client.index(request, RequestOptions.DEFAULT);
            log.debug("Indexed market data for {}", symbol);
        } catch (IOException e) {
            log.error("Failed to index market data: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> searchLogs(String query, String level, int size) {
        String indexPattern = LOGS_INDEX_PREFIX + "*";
        
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (query != null && !query.isEmpty()) {
            boolQuery.must(QueryBuilders.multiMatchQuery(query, "message", "logger", "context.*"));
        }

        if (level != null && !level.isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("level", level.toUpperCase()));
        }

        sourceBuilder.query(boolQuery)
                .size(size)
                .sort("@timestamp", SortOrder.DESC);

        SearchRequest searchRequest = new SearchRequest(indexPattern);
        searchRequest.source(sourceBuilder);

        return executeSearch(searchRequest);
    }

    public List<Map<String, Object>> searchByTraceId(String traceId) {
        String indexPattern = LOGS_INDEX_PREFIX + "*";
        
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("traceId", traceId))
                .size(100)
                .sort("@timestamp", SortOrder.ASC);

        SearchRequest searchRequest = new SearchRequest(indexPattern);
        searchRequest.source(sourceBuilder);

        return executeSearch(searchRequest);
    }

    public List<Map<String, Object>> getRecentLogs(int size) {
        return searchLogs(null, null, size);
    }

    public Map<String, Long> getLogLevelCounts() {
        String indexPattern = LOGS_INDEX_PREFIX + "*";
        Map<String, Long> counts = new HashMap<>();
        
        for (String level : List.of("DEBUG", "INFO", "WARN", "ERROR")) {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.termQuery("level", level))
                    .size(0);
            
            SearchRequest request = new SearchRequest(indexPattern);
            request.source(sourceBuilder);
            
            try {
                SearchResponse response = client.search(request, RequestOptions.DEFAULT);
                counts.put(level, response.getHits().getTotalHits().value);
            } catch (IOException e) {
                log.error("Failed to get count for level {}: {}", level, e.getMessage());
                counts.put(level, 0L);
            }
        }
        
        return counts;
    }

    private List<Map<String, Object>> executeSearch(SearchRequest request) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            for (SearchHit hit : response.getHits().getHits()) {
                Map<String, Object> source = hit.getSourceAsMap();
                source.put("_id", hit.getId());
                source.put("_index", hit.getIndex());
                results.add(source);
            }
        } catch (IOException e) {
            log.error("Failed to execute search: {}", e.getMessage());
        }
        
        return results;
    }
}
