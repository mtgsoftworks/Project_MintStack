package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.CompareInstrumentsRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Analysis", description = "Teknik analiz API'leri")
@SecurityRequirement(name = "bearer")
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/ma/{symbol}")
    @Operation(summary = "Hareketli ortalama hesapla", 
               description = "Belirli bir enstrüman için SMA hesaplar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMovingAverage(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "20") int period,
            @RequestParam(defaultValue = "SMA") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        Map<String, Object> ma = analysisService.getMovingAverage(symbol, period, endDate, type);
        return ResponseEntity.ok(ApiResponse.success(ma));
    }

    @GetMapping("/ma/multiple/{symbol}")
    @Operation(summary = "Çoklu hareketli ortalama (MA7, MA25, MA99)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMultipleMovingAverages(
            @PathVariable String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        Map<String, Object> ma = analysisService.getMultipleMovingAverages(symbol, endDate);
        return ResponseEntity.ok(ApiResponse.success(ma));
    }

    @GetMapping("/trend/{symbol}")
    @Operation(summary = "Trend analizi", 
               description = "Yükselen/düşen/yatay trend tespiti")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrendAnalysis(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "30") int days) {
        
        Map<String, Object> trend = analysisService.getTrendAnalysis(symbol, days);
        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    @PostMapping("/compare")
    @Operation(summary = "Enstrüman karşılaştırma", 
               description = "Birden fazla enstrümanın performans karşılaştırması")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareInstruments(
            @Valid @RequestBody CompareInstrumentsRequest request) {
        
        Map<String, Object> comparison = analysisService.compareInstruments(request);
        return ResponseEntity.ok(ApiResponse.success(comparison));
    }
}
