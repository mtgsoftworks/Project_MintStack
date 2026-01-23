package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.entity.SimulationConfig;
import com.mintstack.finance.entity.SimulationConfig.MarketTrend;
import com.mintstack.finance.entity.SimulationConfig.VolatilityLevel;
import com.mintstack.finance.scheduler.SimulationScheduler;
import com.mintstack.finance.service.simulation.SimulationDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
@Tag(name = "Simulation", description = "Piyasa simülasyonu yönetimi")
public class SimulationController {

    private final SimulationDataService simulationDataService;
    private final SimulationScheduler simulationScheduler;

    @GetMapping("/config")
    @Operation(summary = "Simülasyon ayarlarını getir")
    public ResponseEntity<ApiResponse<SimulationConfigResponse>> getConfig() {
        SimulationConfig config = simulationDataService.getConfig();
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(config)));
    }

    @PostMapping("/config")
    @Operation(summary = "Simülasyon ayarlarını güncelle")
    public ResponseEntity<ApiResponse<SimulationConfigResponse>> updateConfig(
            @RequestBody SimulationConfigRequest request) {
        
        SimulationConfig config = simulationDataService.updateConfig(
                request.enabled(),
                request.volatilityLevel() != null ? VolatilityLevel.valueOf(request.volatilityLevel()) : null,
                request.updateIntervalSeconds(),
                request.marketTrend() != null ? MarketTrend.valueOf(request.marketTrend()) : null,
                request.enableRandomEvents(),
                request.enableMarketHours()
        );
        
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(config), 
                config.getIsEnabled() ? "Simülasyon modu aktif" : "Simülasyon modu kapalı"));
    }

    @PostMapping("/toggle")
    @Operation(summary = "Simülasyonu aç/kapat")
    public ResponseEntity<ApiResponse<SimulationConfigResponse>> toggle() {
        SimulationConfig current = simulationDataService.getConfig();
        SimulationConfig config = simulationDataService.updateConfig(
                !current.getIsEnabled(), null, null, null, null, null);
        
        String message = config.getIsEnabled() 
                ? "🎮 Simülasyon modu aktif edildi" 
                : "Simülasyon modu kapatıldı";
        
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(config), message));
    }

    @PostMapping("/reset")
    @Operation(summary = "Simülasyonu sıfırla - tüm fiyatları başlangıç değerlerine döndür")
    public ResponseEntity<ApiResponse<String>> reset() {
        simulationDataService.resetSimulation();
        simulationScheduler.resetTickCount();
        return ResponseEntity.ok(ApiResponse.success("Simülasyon sıfırlandı", 
                "Tüm fiyatlar başlangıç değerlerine döndürüldü"));
    }

    @GetMapping("/status")
    @Operation(summary = "Simülasyon durumunu getir")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        SimulationConfig config = simulationDataService.getConfig();
        
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", config.getIsEnabled());
        status.put("volatilityLevel", config.getVolatilityLevel().name());
        status.put("marketTrend", config.getMarketTrend().name());
        status.put("updateIntervalSeconds", config.getUpdateIntervalSeconds());
        status.put("enableRandomEvents", config.getEnableRandomEvents());
        status.put("tickCount", simulationScheduler.getTickCount());
        status.put("stockCount", simulationDataService.getStocks().size());
        status.put("currencyCount", simulationDataService.getCurrencies().size());
        status.put("indexCount", simulationDataService.getIndices().size());
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/stocks")
    @Operation(summary = "Simüle edilen hisse senetlerini getir")
    public ResponseEntity<ApiResponse<Map<String, StockResponse>>> getStocks() {
        Map<String, StockResponse> stocks = new HashMap<>();
        
        simulationDataService.getStocks().forEach((symbol, stock) -> {
            stocks.put(symbol, new StockResponse(
                    symbol,
                    stock.getName(),
                    stock.getExchange(),
                    stock.getCurrentPrice().doubleValue(),
                    stock.getPreviousClose().doubleValue(),
                    stock.getChangePercent().doubleValue(),
                    stock.getBaseVolatility()
            ));
        });
        
        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    @GetMapping("/currencies")
    @Operation(summary = "Simüle edilen döviz kurlarını getir")
    public ResponseEntity<ApiResponse<Map<String, CurrencyResponse>>> getCurrencies() {
        Map<String, CurrencyResponse> currencies = new HashMap<>();
        
        simulationDataService.getCurrencies().forEach((code, currency) -> {
            currencies.put(code, new CurrencyResponse(
                    code,
                    currency.getName(),
                    currency.getBuyingRate().doubleValue(),
                    currency.getSellingRate().doubleValue(),
                    currency.getMidRate().doubleValue(),
                    currency.getBaseVolatility()
            ));
        });
        
        return ResponseEntity.ok(ApiResponse.success(currencies));
    }

    @GetMapping("/indices")
    @Operation(summary = "Simüle edilen endeksleri getir")
    public ResponseEntity<ApiResponse<Map<String, IndexResponse>>> getIndices() {
        Map<String, IndexResponse> indices = new HashMap<>();
        
        simulationDataService.getIndices().forEach((symbol, index) -> {
            indices.put(symbol, new IndexResponse(
                    symbol,
                    index.getName(),
                    index.getCurrentValue().doubleValue(),
                    index.getPreviousClose().doubleValue(),
                    index.getChangePercent().doubleValue(),
                    index.getBaseVolatility()
            ));
        });
        
        return ResponseEntity.ok(ApiResponse.success(indices));
    }

    // DTO Records
    public record SimulationConfigRequest(
            Boolean enabled,
            String volatilityLevel,
            Integer updateIntervalSeconds,
            String marketTrend,
            Boolean enableRandomEvents,
            Boolean enableMarketHours
    ) {}

    public record SimulationConfigResponse(
            boolean enabled,
            String volatilityLevel,
            int updateIntervalSeconds,
            String marketTrend,
            boolean enableRandomEvents,
            boolean enableMarketHours,
            String[] availableVolatilityLevels,
            String[] availableMarketTrends
    ) {}

    public record StockResponse(
            String symbol,
            String name,
            String exchange,
            double currentPrice,
            double previousClose,
            double changePercent,
            double baseVolatility
    ) {}

    public record CurrencyResponse(
            String code,
            String name,
            double buyingRate,
            double sellingRate,
            double midRate,
            double baseVolatility
    ) {}

    public record IndexResponse(
            String symbol,
            String name,
            double currentValue,
            double previousClose,
            double changePercent,
            double baseVolatility
    ) {}

    private SimulationConfigResponse mapToResponse(SimulationConfig config) {
        return new SimulationConfigResponse(
                config.getIsEnabled(),
                config.getVolatilityLevel().name(),
                config.getUpdateIntervalSeconds(),
                config.getMarketTrend().name(),
                config.getEnableRandomEvents(),
                config.getEnableMarketHours(),
                new String[]{"LOW", "MEDIUM", "HIGH", "EXTREME"},
                new String[]{"BULLISH", "NEUTRAL", "BEARISH"}
        );
    }
}
