package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.MarketRefreshRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.CurrencyRateResponse;
import com.mintstack.finance.dto.response.InstrumentResponse;
import com.mintstack.finance.dto.response.MarketRefreshResponse;
import com.mintstack.finance.dto.response.PaginationInfo;
import com.mintstack.finance.dto.response.PriceHistoryResponse;
import com.mintstack.finance.entity.Instrument.InstrumentType;
import com.mintstack.finance.service.MarketDataRefreshService;
import com.mintstack.finance.service.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Tag(name = "Market Data", description = "Market data APIs")
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final MarketDataRefreshService marketDataRefreshService;

    @GetMapping("/currencies")
    @Operation(summary = "Get current currency rates")
    public ResponseEntity<ApiResponse<List<CurrencyRateResponse>>> getCurrencyRates(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeEndDate) {
        List<CurrencyRateResponse> rates = marketDataService.getLatestCurrencyRates(changeStartDate, changeEndDate);
        return ResponseEntity.ok(ApiResponse.success(rates));
    }

    @PostMapping("/refresh")
    @SecurityRequirement(name = "bearer")
    @Operation(
        summary = "Refresh market data from sources",
        description = "Runs external source fetch jobs for selected data types and clears market caches"
    )
    public ResponseEntity<ApiResponse<MarketRefreshResponse>> refreshMarketData(
            @RequestBody(required = false) MarketRefreshRequest request) {
        MarketRefreshResponse response = marketDataRefreshService.refreshMarketData(
            request != null ? request.dataTypes() : null
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Piyasa verileri yenilendi"));
    }

    @GetMapping("/currencies/{code}")
    @Operation(summary = "Get one currency rate")
    public ResponseEntity<ApiResponse<CurrencyRateResponse>> getCurrencyRate(
            @PathVariable @Parameter(description = "Currency code, e.g. USD, EUR") String code) {
        CurrencyRateResponse rate = marketDataService.getCurrencyRate(code);
        return ResponseEntity.ok(ApiResponse.success(rate));
    }

    @GetMapping("/currencies/{code}/history")
    @Operation(summary = "Get currency rate history")
    public ResponseEntity<ApiResponse<List<CurrencyRateResponse>>> getCurrencyHistory(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CurrencyRateResponse> history = marketDataService.getCurrencyHistory(code, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/stocks")
    @Operation(summary = "List stocks")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> getStocks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeEndDate,
            @PageableDefault(size = 20, sort = "symbol") Pageable pageable) {

        Page<InstrumentResponse> stocks;
        if (search != null && !search.isEmpty()) {
            stocks = marketDataService.searchInstruments(
                InstrumentType.STOCK,
                search,
                pageable,
                changeStartDate,
                changeEndDate
            );
        } else {
            stocks = marketDataService.getInstrumentsByType(
                InstrumentType.STOCK,
                pageable,
                changeStartDate,
                changeEndDate
            );
        }

        return ResponseEntity.ok(ApiResponse.success(stocks.getContent(), PaginationInfo.from(stocks)));
    }

    @GetMapping("/stocks/{symbol}")
    @Operation(summary = "Get stock detail")
    public ResponseEntity<ApiResponse<InstrumentResponse>> getStock(
            @PathVariable @Parameter(description = "Stock symbol, e.g. THYAO") String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeEndDate) {
        InstrumentResponse stock = marketDataService.getInstrument(symbol, changeStartDate, changeEndDate);
        return ResponseEntity.ok(ApiResponse.success(stock));
    }

    @GetMapping("/stocks/{symbol}/history")
    @Operation(summary = "Get stock price history")
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

    @GetMapping("/bonds")
    @Operation(summary = "List bonds")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> getBonds(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeEndDate,
            @PageableDefault(size = 20, sort = "symbol") Pageable pageable) {
        Page<InstrumentResponse> bonds;
        if (search != null && !search.isEmpty()) {
            bonds = marketDataService.searchInstruments(
                InstrumentType.BOND,
                search,
                pageable,
                changeStartDate,
                changeEndDate
            );
        } else {
            bonds = marketDataService.getInstrumentsByType(
                InstrumentType.BOND,
                pageable,
                changeStartDate,
                changeEndDate
            );
        }
        return ResponseEntity.ok(ApiResponse.success(bonds.getContent(), PaginationInfo.from(bonds)));
    }

    @GetMapping("/funds")
    @Operation(summary = "List funds")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> getFunds(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeEndDate,
            @PageableDefault(size = 20, sort = "symbol") Pageable pageable) {

        Page<InstrumentResponse> funds;
        if (search != null && !search.isEmpty()) {
            funds = marketDataService.searchInstruments(
                InstrumentType.FUND,
                search,
                pageable,
                changeStartDate,
                changeEndDate
            );
        } else {
            funds = marketDataService.getInstrumentsByType(
                InstrumentType.FUND,
                pageable,
                changeStartDate,
                changeEndDate
            );
        }

        return ResponseEntity.ok(ApiResponse.success(funds.getContent(), PaginationInfo.from(funds)));
    }

    @GetMapping("/viop")
    @Operation(summary = "List VIOP instruments")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> getViop(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeEndDate,
            @PageableDefault(size = 20, sort = "symbol") Pageable pageable) {
        Page<InstrumentResponse> viop;
        if (search != null && !search.isEmpty()) {
            viop = marketDataService.searchInstruments(
                InstrumentType.VIOP,
                search,
                pageable,
                changeStartDate,
                changeEndDate
            );
        } else {
            viop = marketDataService.getInstrumentsByType(
                InstrumentType.VIOP,
                pageable,
                changeStartDate,
                changeEndDate
            );
        }
        return ResponseEntity.ok(ApiResponse.success(viop.getContent(), PaginationInfo.from(viop)));
    }

    @GetMapping("/indices/{symbol}")
    @Operation(summary = "Get market index data")
    public ResponseEntity<ApiResponse<InstrumentResponse>> getIndex(
            @PathVariable String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeEndDate) {
        InstrumentResponse index = marketDataService.getMarketIndex(symbol, changeStartDate, changeEndDate);
        return ResponseEntity.ok(ApiResponse.success(index));
    }

    @GetMapping("/search")
    @Operation(summary = "Search all instruments")
    public ResponseEntity<ApiResponse<List<InstrumentResponse>>> searchInstruments(
            @RequestParam String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate changeEndDate,
            @PageableDefault(size = 20, sort = "symbol") Pageable pageable) {
        Page<InstrumentResponse> results = marketDataService.searchInstruments(
            query,
            pageable,
            changeStartDate,
            changeEndDate
        );
        return ResponseEntity.ok(ApiResponse.success(results.getContent(), PaginationInfo.from(results)));
    }
}
