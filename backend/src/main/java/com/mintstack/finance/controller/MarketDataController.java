package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.CurrencyRateResponse;
import com.mintstack.finance.dto.response.InstrumentResponse;
import com.mintstack.finance.dto.response.PaginationInfo;
import com.mintstack.finance.dto.response.PriceHistoryResponse;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import com.mintstack.finance.service.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Tag(name = "Market Data", description = "Piyasa verileri API'leri")
public class MarketDataController {

    private final MarketDataService marketDataService;

    // Currency Endpoints
    @GetMapping("/currencies")
    @Operation(summary = "Güncel döviz kurlarını getir", description = "TCMB'den alınan güncel döviz kurları")
    public ResponseEntity<ApiResponse<List<CurrencyRateResponse>>> getCurrencyRates() {
        List<CurrencyRateResponse> rates = marketDataService.getLatestCurrencyRates();
        return ResponseEntity.ok(ApiResponse.success(rates));
    }

    @GetMapping("/currencies/{code}")
    @Operation(summary = "Belirli bir döviz kurunı getir")
    public ResponseEntity<ApiResponse<CurrencyRateResponse>> getCurrencyRate(
            @PathVariable @Parameter(description = "Döviz kodu (örn: USD, EUR)") String code) {
        CurrencyRateResponse rate = marketDataService.getCurrencyRate(code);
        return ResponseEntity.ok(ApiResponse.success(rate));
    }

    @GetMapping("/currencies/{code}/history")
    @Operation(summary = "Döviz kuru geçmişini getir")
    public ResponseEntity<ApiResponse<List<CurrencyRateResponse>>> getCurrencyHistory(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CurrencyRateResponse> history = marketDataService.getCurrencyHistory(code, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // Stock Endpoints
    @GetMapping("/stocks")
    @Operation(summary = "Hisse senetlerini listele")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> getStocks(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<InstrumentResponse> stocks;
        if (search != null && !search.isEmpty()) {
            stocks = marketDataService.searchInstruments(InstrumentType.STOCK, search, pageable);
        } else {
            stocks = marketDataService.getInstrumentsByType(InstrumentType.STOCK, pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success(stocks.getContent(), PaginationInfo.from(stocks)));
    }

    @GetMapping("/stocks/{symbol}")
    @Operation(summary = "Hisse senedi detayını getir")
    public ResponseEntity<ApiResponse<InstrumentResponse>> getStock(
            @PathVariable @Parameter(description = "Hisse sembolü (örn: THYAO)") String symbol) {
        InstrumentResponse stock = marketDataService.getInstrument(symbol);
        return ResponseEntity.ok(ApiResponse.success(stock));
    }

    @GetMapping("/stocks/{symbol}/history")
    @Operation(summary = "Hisse senedi fiyat geçmişini getir")
    public ResponseEntity<ApiResponse<List<PriceHistoryResponse>>> getStockHistory(
            @PathVariable String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        
        List<PriceHistoryResponse> history;
        if (startDate != null && endDate != null) {
            history = marketDataService.getPriceHistory(symbol, startDate, endDate);
        } else {
            history = marketDataService.getRecentPriceHistory(symbol, days);
        }
        
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // Bond Endpoints
    @GetMapping("/bonds")
    @Operation(summary = "Tahvil/bono listesini getir")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> getBonds(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InstrumentResponse> bonds = marketDataService.getInstrumentsByType(InstrumentType.BOND, pageable);
        return ResponseEntity.ok(ApiResponse.success(bonds.getContent(), PaginationInfo.from(bonds)));
    }

    // Fund Endpoints
    @GetMapping("/funds")
    @Operation(summary = "Yatırım fonlarını listele")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> getFunds(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<InstrumentResponse> funds;
        if (search != null && !search.isEmpty()) {
            funds = marketDataService.searchInstruments(InstrumentType.FUND, search, pageable);
        } else {
            funds = marketDataService.getInstrumentsByType(InstrumentType.FUND, pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success(funds.getContent(), PaginationInfo.from(funds)));
    }

    // VIOP Endpoints
    @GetMapping("/viop")
    @Operation(summary = "VIOP enstrümanlarını listele")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> getViop(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InstrumentResponse> viop = marketDataService.getInstrumentsByType(InstrumentType.VIOP, pageable);
        return ResponseEntity.ok(ApiResponse.success(viop.getContent(), PaginationInfo.from(viop)));
    }

    // Index Endpoints
    @GetMapping("/indices/{symbol}")
    @Operation(summary = "Piyasa endeksi verisini getir (örn: XU100.IS)")
    public ResponseEntity<ApiResponse<InstrumentResponse>> getIndex(
            @PathVariable String symbol) {
        InstrumentResponse index = marketDataService.getMarketIndex(symbol);
        return ResponseEntity.ok(ApiResponse.success(index));
    }

    // Search
    @GetMapping("/search")
    @Operation(summary = "Tüm enstrümanlarda ara")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> searchInstruments(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InstrumentResponse> results = marketDataService.searchInstruments(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(results.getContent(), PaginationInfo.from(results)));
    }
}
