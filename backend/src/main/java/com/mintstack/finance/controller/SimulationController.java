package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.HealthStatus;
import com.mintstack.finance.dto.response.SimulationMetrics;
import com.mintstack.finance.dto.simulation.MarketEvent;
import com.mintstack.finance.dto.simulation.MarketEvent.EventType;
import com.mintstack.finance.entity.SimulationConfig;
import com.mintstack.finance.entity.SimulationConfig.MarketTrend;
import com.mintstack.finance.entity.SimulationConfig.VolatilityLevel;
import com.mintstack.finance.scheduler.SimulationScheduler;
import com.mintstack.finance.service.PriceCacheService;
import com.mintstack.finance.service.simulation.MarketEventEngine;
import com.mintstack.finance.service.simulation.PriceSimulationEngine;
import com.mintstack.finance.service.simulation.SimulationDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
@Tag(name = "Simulation", description = "Piyasa simülasyonu yönetimi")
@PreAuthorize("hasRole('ADMIN')")
public class SimulationController {

    private final SimulationDataService simulationDataService;
    private final SimulationScheduler simulationScheduler;
    private final PriceSimulationEngine priceEngine;
    private final MarketEventEngine marketEventEngine;
    private final PriceCacheService priceCacheService;
    
    @Getter
    private LocalDateTime startTime;
    private volatile LocalDateTime lastUpdateTime;
    
    @PostConstruct
    public void init() {
        this.startTime = LocalDateTime.now();
        this.lastUpdateTime = LocalDateTime.now();
    }

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
        status.put("bondCount", simulationDataService.getBonds().size());
        status.put("fundCount", simulationDataService.getFunds().size());
        status.put("viopCount", simulationDataService.getViop().size());
        status.put("currencyCount", simulationDataService.getCurrencies().size());
        status.put("indexCount", simulationDataService.getIndices().size());
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Detaylı simülasyon metriklerini getir")
    public ResponseEntity<ApiResponse<SimulationMetrics>> getMetrics() {
        this.lastUpdateTime = LocalDateTime.now();
        
        Map<String, Long> volatilityStats = new HashMap<>();
        priceEngine.getRegimeDistribution().forEach((regime, count) -> {
            volatilityStats.put(regime, count);
        });
        
        SimulationMetrics metrics = SimulationMetrics.builder()
            .tickCount(simulationScheduler.getTickCount())
            .uptime(Duration.between(startTime, LocalDateTime.now()))
            .stocks(simulationDataService.getStocks().size())
            .bonds(simulationDataService.getBonds().size())
            .funds(simulationDataService.getFunds().size())
            .viop(simulationDataService.getViop().size())
            .currencies(simulationDataService.getCurrencies().size())
            .indices(simulationDataService.getIndices().size())
            .cryptos(0)
            .activeEvents(marketEventEngine.getActiveEvents().size())
            .volatilityStats(volatilityStats)
            .cacheHitRatio(priceCacheService.isRedisAvailable() ? 1.0 : 0.0)
            .lastUpdateTime(lastUpdateTime)
            .avgTickDurationMs(0.0)
            .totalEventsGenerated(0L)
            .totalNewsGenerated(0L)
            .build();
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/health")
    @Operation(summary = "Simülasyon sağlık kontrolü")
    public ResponseEntity<ApiResponse<HealthStatus>> getHealth() {
        boolean simulationEnabled = simulationDataService.isSimulationEnabled();
        boolean redisConnected = priceCacheService.isRedisAvailable();
        boolean websocketConnected = true;
        boolean schedulerRunning = true;
        
        String status = HealthStatus.calculateStatus(
            simulationEnabled, redisConnected, websocketConnected, schedulerRunning
        );
        
        HealthStatus health = HealthStatus.builder()
            .simulationEnabled(simulationEnabled)
            .redisConnected(redisConnected)
            .websocketConnected(websocketConnected)
            .schedulerRunning(schedulerRunning)
            .status(status)
            .build();
        
        return ResponseEntity.ok(ApiResponse.success(health));
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

    @GetMapping("/bonds")
    @Operation(summary = "Simule edilen tahvil ve bonolari getir")
    public ResponseEntity<ApiResponse<Map<String, QuoteResponse>>> getBonds() {
        return ResponseEntity.ok(ApiResponse.success(mapQuoteResponses(simulationDataService.getBonds())));
    }

    @GetMapping("/funds")
    @Operation(summary = "Simule edilen yatirim fonlarini getir")
    public ResponseEntity<ApiResponse<Map<String, QuoteResponse>>> getFunds() {
        return ResponseEntity.ok(ApiResponse.success(mapQuoteResponses(simulationDataService.getFunds())));
    }

    @GetMapping("/viop")
    @Operation(summary = "Simule edilen VIOP kontratlarini getir")
    public ResponseEntity<ApiResponse<Map<String, QuoteResponse>>> getViop() {
        return ResponseEntity.ok(ApiResponse.success(mapQuoteResponses(simulationDataService.getViop())));
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

    @GetMapping("/volatility")
    @Operation(summary = "Volatilite istatistiklerini getir")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVolatilityStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("currentVolatility", priceEngine.getCurrentVolatility());
        stats.put("regimeDistribution", priceEngine.getRegimeDistribution());
        
        Map<String, Map<String, Object>> symbolDetails = new HashMap<>();
        priceEngine.getCurrentVolatility().forEach((symbol, vol) -> {
            Map<String, Object> details = new HashMap<>();
            details.put("currentVolatility", vol);
            details.put("regime", priceEngine.getVolatilityRegime(symbol).name());
            details.put("regimeMultiplier", priceEngine.getRegimeMultiplier(symbol));
            details.put("longTermVolatility", priceEngine.getLongTermVolatility(symbol));
            symbolDetails.put(symbol, details);
        });
        stats.put("symbolDetails", symbolDetails);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    @PostMapping("/volatility/burst")
    @Operation(summary = "Volatilite patlaması tetikle")
    public ResponseEntity<ApiResponse<String>> triggerVolatilityBurst(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "2.0") double multiplier,
            @RequestParam(defaultValue = "10") int durationTicks) {
        
        priceEngine.triggerVolatilityBurst(symbol, multiplier, durationTicks);
        
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Volatility burst triggered for %s", symbol),
                String.format("Multiplier: %.2fx, Duration: %d ticks", multiplier, durationTicks)));
    }
    
    @GetMapping("/events")
    @Operation(summary = "Aktif piyasa olaylarını getir")
    public ResponseEntity<ApiResponse<List<MarketEvent>>> getActiveEvents() {
        List<MarketEvent> events = marketEventEngine.getActiveEvents().values().stream().toList();
        return ResponseEntity.ok(ApiResponse.success(events));
    }
    
    @GetMapping("/events/types")
    @Operation(summary = "Kullanılabilir piyasa olayı tiplerini getir")
    public ResponseEntity<ApiResponse<String[]>> getEventTypes() {
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Arrays.stream(EventType.values()).map(Enum::name).toArray(String[]::new)
        ));
    }
    
    @PostMapping("/events/trigger")
    @Operation(summary = "Manuel piyasa olayı tetikle (admin)")
    public ResponseEntity<ApiResponse<MarketEvent>> triggerEvent(@RequestParam String eventType) {
        try {
            EventType type = EventType.valueOf(eventType.toUpperCase());
            Optional<MarketEvent> event = marketEventEngine.generateEventByType(type);
            
            if (event.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(event.get(), 
                        "Piyasa olayı tetiklendi: " + type));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Bu olay tipi manuel olarak tetiklenemez: " + type));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Geçersiz olay tipi: " + eventType));
        }
    }
    
    @GetMapping("/events/symbol/{symbol}")
    @Operation(summary = "Belirli bir hisseyi etkileyen aktif olayları getir")
    public ResponseEntity<ApiResponse<List<MarketEvent>>> getEventsForSymbol(@PathVariable String symbol) {
        List<MarketEvent> events = marketEventEngine.getActiveEventsForSymbol(symbol);
        return ResponseEntity.ok(ApiResponse.success(events));
    }
    
    @DeleteMapping("/events")
    @Operation(summary = "Tüm aktif olayları temizle")
    public ResponseEntity<ApiResponse<String>> clearAllEvents() {
        marketEventEngine.clearAllEvents();
        return ResponseEntity.ok(ApiResponse.success("Tüm piyasa olayları temizlendi"));
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

    public record QuoteResponse(
            String symbol,
            String name,
            String exchange,
            double currentPrice,
            double previousClose,
            double changePercent,
            long volume,
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

    private Map<String, QuoteResponse> mapQuoteResponses(Map<String, com.mintstack.finance.service.simulation.SimulatedStock> quotes) {
        Map<String, QuoteResponse> responses = new HashMap<>();
        quotes.forEach((symbol, quote) -> responses.put(symbol, new QuoteResponse(
            symbol,
            quote.getName(),
            quote.getExchange(),
            quote.getCurrentPrice().doubleValue(),
            quote.getPreviousClose().doubleValue(),
            quote.getChangePercent().doubleValue(),
            quote.getVolume(),
            quote.getBaseVolatility()
        )));
        return responses;
    }
}
