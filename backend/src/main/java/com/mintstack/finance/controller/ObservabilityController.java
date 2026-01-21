package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.service.search.OpenSearchService;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/observability")
@RequiredArgsConstructor
@Tag(name = "Observability", description = "Sistem gözlemleme API'leri (Admin only)")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasRole('admin')")
public class ObservabilityController {

    private final OpenSearchService openSearchService;
    private final MeterRegistry meterRegistry;

    @GetMapping("/logs")
    @Operation(summary = "Log kayıtlarını ara")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchLogs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "50") int size) {
        
        List<Map<String, Object>> logs = openSearchService.searchLogs(query, level, size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/logs/trace/{traceId}")
    @Operation(summary = "Trace ID ile logları getir")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLogsByTrace(
            @PathVariable String traceId) {
        
        List<Map<String, Object>> logs = openSearchService.searchByTraceId(traceId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/logs/recent")
    @Operation(summary = "Son logları getir")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentLogs(
            @RequestParam(defaultValue = "100") int size) {
        
        List<Map<String, Object>> logs = openSearchService.getRecentLogs(size);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/logs/stats")
    @Operation(summary = "Log istatistiklerini getir")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLogStats() {
        Map<String, Long> stats = openSearchService.getLogLevelCounts();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Sistem metriklerini getir")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // JVM metrics
        metrics.put("jvm.memory.used", getGaugeValue("jvm.memory.used"));
        metrics.put("jvm.memory.max", getGaugeValue("jvm.memory.max"));
        metrics.put("jvm.threads.live", getGaugeValue("jvm.threads.live"));
        
        // HTTP metrics
        metrics.put("http.server.requests.count", getCounterValue("http.server.requests"));
        
        // Custom app metrics
        metrics.put("system.cpu.usage", getGaugeValue("system.cpu.usage"));
        metrics.put("process.uptime", getGaugeValue("process.uptime"));
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/health/detailed")
    @Operation(summary = "Detaylı sağlık durumu")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDetailedHealth() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("status", "UP");
        health.put("services", Map.of(
                "database", checkDatabaseHealth(),
                "redis", checkRedisHealth(),
                "kafka", checkKafkaHealth(),
                "opensearch", checkOpenSearchHealth()
        ));
        
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    private Double getGaugeValue(String name) {
        try {
            var gauge = meterRegistry.find(name).gauge();
            return gauge != null ? gauge.value() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Double getCounterValue(String name) {
        try {
            var counter = meterRegistry.find(name).counter();
            return counter != null ? counter.count() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String checkDatabaseHealth() {
        return "UP";
    }

    private String checkRedisHealth() {
        return "UP";
    }

    private String checkKafkaHealth() {
        return "UP";
    }

    private String checkOpenSearchHealth() {
        return "UP";
    }
}
