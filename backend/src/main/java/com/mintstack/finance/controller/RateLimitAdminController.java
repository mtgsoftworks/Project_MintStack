package com.mintstack.finance.controller;

import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.dto.request.UpdateRateLimitRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.RateLimitConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/rate-limit")
@RequiredArgsConstructor
@Tag(name = "Admin Rate Limit", description = "Rate limiting runtime yonetimi (Admin only)")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasRole('ADMIN')")
public class RateLimitAdminController {

    private final RateLimitConfig rateLimitConfig;

    @GetMapping
    @Operation(summary = "Rate limit ayarlarini getir")
    public ResponseEntity<ApiResponse<RateLimitConfigResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(toResponse()));
    }

    @PutMapping
    @Operation(summary = "Rate limit ayarlarini guncelle")
    public ResponseEntity<ApiResponse<RateLimitConfigResponse>> updateSettings(
        @Valid @RequestBody UpdateRateLimitRequest request
    ) {
        rateLimitConfig.updateSettings(
            request.getEnabled(),
            request.getAnonymousRequestsPerMinute(),
            request.getAuthenticatedRequestsPerMinute(),
            request.getAdminRequestsPerMinute(),
            Boolean.TRUE.equals(request.getClearBuckets())
        );

        return ResponseEntity.ok(
            ApiResponse.success(toResponse(), "Rate limit ayarlari guncellendi")
        );
    }

    private RateLimitConfigResponse toResponse() {
        return RateLimitConfigResponse.builder()
            .enabled(rateLimitConfig.isEnabled())
            .anonymousRequestsPerMinute(rateLimitConfig.getAnonymousRequestsPerMinute())
            .authenticatedRequestsPerMinute(rateLimitConfig.getAuthenticatedRequestsPerMinute())
            .adminRequestsPerMinute(rateLimitConfig.getAdminRequestsPerMinute())
            .bucketCount(rateLimitConfig.getBucketCount())
            .build();
    }
}
