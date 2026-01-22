package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.MonteCarloResult;
import com.mintstack.finance.dto.response.PortfolioRiskResult;
import com.mintstack.finance.dto.response.VaRResult;
import com.mintstack.finance.service.MonteCarloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Monte Carlo Simülasyon API Controller
 */
@RestController
@RequestMapping("/api/v1/montecarlo")
@RequiredArgsConstructor
@Tag(name = "Monte Carlo", description = "Monte Carlo simülasyonu ve risk analizi")
public class MonteCarloController {

    private final MonteCarloService monteCarloService;

    @PostMapping("/simulate")
    @Operation(summary = "Monte Carlo simülasyonu çalıştır", 
               description = "Belirtilen enstrüman için fiyat tahmin simülasyonu yapar")
    public ResponseEntity<ApiResponse<MonteCarloResult>> runSimulation(
            @RequestBody SimulationRequest request) {
        
        int simulations = request.simulations() != null ? request.simulations() : 10000;
        double confidence = request.confidence() != null ? request.confidence() : 0.95;
        
        MonteCarloResult result = monteCarloService.runSimulation(
                request.symbol(), 
                request.days(), 
                simulations, 
                confidence
        );
        
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.error("Simülasyon çalıştırılamadı - yeterli veri yok"));
        }
        
        String message = String.format("%d gün sonra beklenen fiyat aralığı: %.2f - %.2f TL (%%.90 güven)",
                request.days(), result.p5().doubleValue(), result.p95().doubleValue());
        
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @GetMapping("/var/{symbol}")
    @Operation(summary = "VaR hesapla", description = "Value at Risk (Riske Maruz Değer) hesaplar")
    public ResponseEntity<ApiResponse<VaRResult>> calculateVaR(
            @PathVariable @Parameter(description = "Enstrüman sembolü") String symbol,
            @RequestParam(defaultValue = "10") @Parameter(description = "Gün sayısı") int days,
            @RequestParam(defaultValue = "0.95") @Parameter(description = "Güven düzeyi") double confidence) {
        
        VaRResult result = monteCarloService.calculateVaR(symbol, days, confidence);
        
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.error("VaR hesaplanamadı"));
        }
        
        String message = String.format("%%%.0f güven düzeyinde %d günlük VaR: %.2f TL (%%%.2f)",
                confidence * 100, days, result.varAmount().doubleValue(), result.varPercent());
        
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    @PostMapping("/portfolio-risk")
    @Operation(summary = "Portföy risk analizi", description = "Portföy için kapsamlı risk analizi yapar")
    public ResponseEntity<ApiResponse<PortfolioRiskResult>> analyzePortfolioRisk(
            @RequestBody PortfolioRiskRequest request) {
        
        int simulations = request.simulations() != null ? request.simulations() : 5000;
        
        PortfolioRiskResult result = monteCarloService.analyzePortfolioRisk(
                request.portfolioId(), 
                request.days(), 
                simulations
        );
        
        if (result == null) {
            return ResponseEntity.ok(ApiResponse.error("Portföy risk analizi yapılamadı"));
        }
        
        String message = String.format("Portföy VaR: %.2f TL, Sharpe Ratio: %.2f",
                result.portfolioVaR().doubleValue(), result.sharpeRatio());
        
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    // =================== REQUEST DTOs ===================

    public record SimulationRequest(
        String symbol,
        int days,
        Integer simulations,
        Double confidence
    ) {}

    public record PortfolioRiskRequest(
        UUID portfolioId,
        int days,
        Integer simulations
    ) {}
}
