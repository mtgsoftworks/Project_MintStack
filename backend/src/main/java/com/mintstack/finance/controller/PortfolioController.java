package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.AddPortfolioItemRequest;
import com.mintstack.finance.dto.request.AdjustPortfolioCashRequest;
import com.mintstack.finance.dto.request.CreatePortfolioRequest;
import com.mintstack.finance.dto.request.ExecutePortfolioTradeRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.PaginationInfo;
import com.mintstack.finance.dto.response.PortfolioResponse;
import com.mintstack.finance.dto.response.PortfolioSummaryResponse;
import com.mintstack.finance.dto.response.PortfolioTransactionResponse;
import com.mintstack.finance.service.ExportService;
import com.mintstack.finance.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portfoy yonetimi API'leri")
@SecurityRequirement(name = "bearer")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final ExportService exportService;
    private final com.mintstack.finance.service.UserService userService;

    @GetMapping
    @Operation(summary = "Kullanicinin portfoylerini listele")
    public ResponseEntity<ApiResponse<List<PortfolioResponse>>> getPortfolios(
            @AuthenticationPrincipal Jwt jwt) {
        userService.getOrCreateUser(jwt);
        List<PortfolioResponse> portfolios = portfolioService.getUserPortfolios(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(portfolios));
    }

    @GetMapping("/summary")
    @Operation(summary = "Kullanicinin tum portfoy ozetini getir")
    public ResponseEntity<ApiResponse<PortfolioSummaryResponse>> getPortfolioSummary(
            @AuthenticationPrincipal Jwt jwt) {
        userService.getOrCreateUser(jwt);
        PortfolioSummaryResponse summary = portfolioService.getUserPortfolioSummary(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Portfoy detayini getir")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        PortfolioResponse portfolio = portfolioService.getPortfolio(jwt.getSubject(), id);
        return ResponseEntity.ok(ApiResponse.success(portfolio));
    }

    @PostMapping
    @Operation(summary = "Yeni portfoy olustur")
    public ResponseEntity<ApiResponse<PortfolioResponse>> createPortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePortfolioRequest request) {
        userService.getOrCreateUser(jwt);
        PortfolioResponse portfolio = portfolioService.createPortfolio(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(portfolio, "Portfoy basariyla olusturuldu"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Portfoyu guncelle")
    public ResponseEntity<ApiResponse<PortfolioResponse>> updatePortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody CreatePortfolioRequest request) {
        PortfolioResponse portfolio = portfolioService.updatePortfolio(jwt.getSubject(), id, request);
        return ResponseEntity.ok(ApiResponse.success(portfolio, "Portfoy basariyla guncellendi"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Portfoyu sil")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        portfolioService.deletePortfolio(jwt.getSubject(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Portfoy basariyla silindi"));
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Portfoye enstruman ekle")
    public ResponseEntity<ApiResponse<PortfolioResponse>> addItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody AddPortfolioItemRequest request) {
        PortfolioResponse portfolio = portfolioService.addItem(jwt.getSubject(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(portfolio, "Enstruman portfoye eklendi"));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "Portfoyden enstruman cikar")
    public ResponseEntity<ApiResponse<PortfolioResponse>> removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        PortfolioResponse portfolio = portfolioService.removeItem(jwt.getSubject(), id, itemId);
        return ResponseEntity.ok(ApiResponse.success(portfolio, "Enstruman portfoyden cikarildi"));
    }

    @PostMapping("/{id}/trades")
    @Operation(summary = "Portfoyde al/sat islemi gerceklestir")
    public ResponseEntity<ApiResponse<PortfolioResponse>> executeTrade(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ExecutePortfolioTradeRequest request) {
        PortfolioResponse portfolio = portfolioService.executeTrade(jwt.getSubject(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(portfolio, "Islem basariyla gerceklestirildi"));
    }

    @PostMapping("/{id}/orders/process")
    @Operation(summary = "Bekleyen emirleri simule et ve uygun olanlari doldur")
    public ResponseEntity<ApiResponse<PortfolioResponse>> processPendingOrders(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        PortfolioResponse portfolio = portfolioService.processPendingOrders(jwt.getSubject(), id);
        return ResponseEntity.ok(ApiResponse.success(portfolio, "Bekleyen emirler islendi"));
    }

    @PostMapping("/{id}/orders/{orderId}/cancel")
    @Operation(summary = "Bekleyen emri iptal et")
    public ResponseEntity<ApiResponse<PortfolioResponse>> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID orderId,
            @RequestParam(required = false) String reason) {
        PortfolioResponse portfolio = portfolioService.cancelOrder(jwt.getSubject(), id, orderId, reason);
        return ResponseEntity.ok(ApiResponse.success(portfolio, "Emir iptal edildi"));
    }

    @PostMapping("/{id}/cash")
    @Operation(summary = "Portfoy nakit bakiyesini arttir/azalt")
    public ResponseEntity<ApiResponse<PortfolioResponse>> adjustCashBalance(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody AdjustPortfolioCashRequest request) {
        PortfolioResponse portfolio = portfolioService.adjustCashBalance(jwt.getSubject(), id, request);
        return ResponseEntity.ok(ApiResponse.success(portfolio, "Nakit bakiyesi guncellendi"));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Portfoy islem gecmisini getir")
    public ResponseEntity<ApiResponse<List<PortfolioTransactionResponse>>> getPortfolioTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestParam(required = false) com.mintstack.finance.entity.PortfolioTransaction.OrderStatus orderStatus,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PortfolioTransactionResponse> transactions = portfolioService
            .getPortfolioTransactions(jwt.getSubject(), id, orderStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions.getContent(), PaginationInfo.from(transactions)));
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Portfoy ozeti ve kar/zarar bilgisini getir")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getSinglePortfolioSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        PortfolioResponse summary = portfolioService.getPortfolioSummary(jwt.getSubject(), id);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/{id}/export/excel")
    @Operation(summary = "Portfoyu Excel formatinda disa aktar")
    public ResponseEntity<byte[]> exportToExcel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        PortfolioResponse portfolio = portfolioService.getPortfolio(jwt.getSubject(), id);
        byte[] excelBytes = exportService.exportPortfolioToExcel(portfolio);

        String filename = generateFilename(portfolio.getName(), "xlsx");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(excelBytes.length);

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/export/pdf")
    @Operation(summary = "Portfoyu PDF formatinda disa aktar")
    public ResponseEntity<byte[]> exportToPdf(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        PortfolioResponse portfolio = portfolioService.getPortfolio(jwt.getSubject(), id);
        byte[] pdfBytes = exportService.exportPortfolioToPdf(portfolio);

        String filename = generateFilename(portfolio.getName(), "pdf");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private String generateFilename(String portfolioName, String extension) {
        String sanitizedName = portfolioName
            .replaceAll("[^a-zA-Z0-9\\s-]", "")
            .replaceAll("\\s+", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("portfoy_%s_%s.%s", sanitizedName, timestamp, extension);
    }
}
