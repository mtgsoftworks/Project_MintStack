package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.BacktestResult;
import com.mintstack.finance.service.BacktestingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Backtesting API Controller
 */
@RestController
@RequestMapping("/api/v1/backtest")
@RequiredArgsConstructor
@Tag(name = "Backtesting", description = "Trading stratejisi backtesting")
@SecurityRequirement(name = "bearer")
public class BacktestingController {

    private final BacktestingService backtestingService;

    @PostMapping("/run")
    @Operation(summary = "Backtest çalıştır", 
               description = "Belirtilen stratejiyi geçmiş veriler üzerinde test eder")
    public ResponseEntity<ApiResponse<BacktestResult>> runBacktest(
            @RequestBody BacktestRequest request) {
        
        BigDecimal initialCapital = request.initialCapital() != null 
                ? request.initialCapital() 
                : BigDecimal.valueOf(10000);
        
        BacktestResult result = backtestingService.runBacktest(
                request.strategy(),
                request.symbol(),
                request.startDate(),
                request.endDate(),
                initialCapital
        );
        
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.error("Backtest çalıştırılamadı - yeterli veri yok veya geçersiz strateji"));
        }
        
        String message = String.format("Toplam Getiri: %.2f%%, Sharpe: %.2f, Win Rate: %.1f%%",
                result.totalReturnPercent(), result.sharpeRatio(), result.winRatePercent());
        
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @GetMapping("/strategies")
    @Operation(summary = "Mevcut stratejileri listele", description = "Kullanılabilir trading stratejilerini döndürür")
    public ResponseEntity<ApiResponse<List<BacktestingService.StrategyInfo>>> getStrategies() {
        List<BacktestingService.StrategyInfo> strategies = backtestingService.getAvailableStrategies();
        return ResponseEntity.ok(ApiResponse.success(strategies, strategies.size() + " strateji mevcut"));
    }

    @GetMapping("/quick/{symbol}")
    @Operation(summary = "Hızlı backtest", description = "Son 1 yıl için varsayılan ayarlarla backtest yapar")
    public ResponseEntity<ApiResponse<BacktestResult>> quickBacktest(
            @PathVariable @Parameter(description = "Enstrüman sembolü") String symbol,
            @RequestParam(defaultValue = "MA_CROSSOVER") @Parameter(description = "Strateji adı") String strategy) {
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        
        BacktestResult result = backtestingService.runBacktest(
                strategy,
                symbol,
                startDate,
                endDate,
                BigDecimal.valueOf(10000)
        );
        
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.error("Backtest çalıştırılamadı"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(result, "1 yıllık backtest tamamlandı"));
    }

    @GetMapping("/compare/{symbol}")
    @Operation(summary = "Strateji karşılaştırma", 
               description = "Aynı enstrüman için birden fazla stratejiyi karşılaştırır")
    public ResponseEntity<ApiResponse<List<BacktestResult>>> compareStrategies(
            @PathVariable @Parameter(description = "Enstrüman sembolü") String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusYears(1);
        
        List<String> strategyNames = List.of("MA_CROSSOVER", "MA_CROSSOVER_20_50", "RSI", "RSI_CONSERVATIVE");
        List<BacktestResult> results = new java.util.ArrayList<>();
        
        for (String strategyName : strategyNames) {
            BacktestResult result = backtestingService.runBacktest(
                    strategyName, symbol, startDate, endDate, BigDecimal.valueOf(10000));
            if (result != null) {
                results.add(result);
            }
        }
        
        if (results.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Hiçbir strateji çalıştırılamadı"));
        }
        
        // En iyi stratejiyi bul
        BacktestResult best = results.stream()
                .max((a, b) -> Double.compare(a.totalReturnPercent(), b.totalReturnPercent()))
                .orElse(results.get(0));
        
        String message = String.format("En iyi strateji: %s (%.2f%% getiri)", 
                best.strategyName(), best.totalReturnPercent());
        
        return ResponseEntity.ok(ApiResponse.success(results, message));
    }

    // =================== REQUEST DTOs ===================

    public record BacktestRequest(
        String strategy,
        String symbol,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,
        BigDecimal initialCapital
    ) {}
}
