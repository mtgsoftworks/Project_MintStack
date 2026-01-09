package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.AddPortfolioItemRequest;
import com.mintstack.finance.dto.request.CreatePortfolioRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.PortfolioResponse;
import com.mintstack.finance.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Portföy yönetimi API'leri")
@SecurityRequirement(name = "bearer")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final com.mintstack.finance.service.UserService userService;

    @GetMapping
    @Operation(summary = "Kullanıcının portföylerini listele")
    public ResponseEntity<ApiResponse<List<PortfolioResponse>>> getPortfolios(
            @AuthenticationPrincipal Jwt jwt) {
        userService.getOrCreateUser(jwt);
        List<PortfolioResponse> portfolios = portfolioService.getUserPortfolios(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success(portfolios));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Portföy detayını getir")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        PortfolioResponse portfolio = portfolioService.getPortfolio(jwt.getSubject(), id);
        return ResponseEntity.ok(ApiResponse.success(portfolio));
    }

    @PostMapping
    @Operation(summary = "Yeni portföy oluştur")
    public ResponseEntity<ApiResponse<PortfolioResponse>> createPortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePortfolioRequest request) {
        userService.getOrCreateUser(jwt);
        PortfolioResponse portfolio = portfolioService.createPortfolio(jwt.getSubject(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(portfolio, "Portföy başarıyla oluşturuldu"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Portföyü güncelle")
    public ResponseEntity<ApiResponse<PortfolioResponse>> updatePortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody CreatePortfolioRequest request) {
        PortfolioResponse portfolio = portfolioService.updatePortfolio(jwt.getSubject(), id, request);
        return ResponseEntity.ok(ApiResponse.success(portfolio, "Portföy başarıyla güncellendi"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Portföyü sil")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        portfolioService.deletePortfolio(jwt.getSubject(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Portföy başarıyla silindi"));
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Portföye enstrüman ekle")
    public ResponseEntity<ApiResponse<PortfolioResponse>> addItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody AddPortfolioItemRequest request) {
        PortfolioResponse portfolio = portfolioService.addItem(jwt.getSubject(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(portfolio, "Enstrüman portföye eklendi"));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "Portföyden enstrüman çıkar")
    public ResponseEntity<ApiResponse<PortfolioResponse>> removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID itemId) {
        PortfolioResponse portfolio = portfolioService.removeItem(jwt.getSubject(), id, itemId);
        return ResponseEntity.ok(ApiResponse.success(portfolio, "Enstrüman portföyden çıkarıldı"));
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Portföy özeti ve kar/zarar bilgisini getir")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolioSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        PortfolioResponse summary = portfolioService.getPortfolioSummary(jwt.getSubject(), id);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
