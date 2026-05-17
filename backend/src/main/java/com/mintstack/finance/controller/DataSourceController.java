package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.DataPreferenceRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.DataPreferenceResponse;
import com.mintstack.finance.service.DataSourceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/data-sources")
@RequiredArgsConstructor
@Tag(name = "Data Sources", description = "Veri kaynağı yönetimi ve tercihleri")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasRole('ADMIN')")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    /**
     * Get all provider capabilities (what data types each provider supports)
     */
    @GetMapping("/capabilities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCapabilities() {
        Map<String, Object> capabilities = dataSourceService.getProviderCapabilities();
        return ResponseEntity.ok(ApiResponse.success(capabilities));
    }

    /**
     * Get user's data source preferences
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<List<DataPreferenceResponse>>> getPreferences(
            @AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        List<DataPreferenceResponse> preferences = dataSourceService.getUserPreferences(keycloakId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * Set data source preference for a specific data type
     */
    @PostMapping("/preferences")
    public ResponseEntity<ApiResponse<DataPreferenceResponse>> setPreference(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DataPreferenceRequest request) {
        String keycloakId = jwt.getSubject();
        DataPreferenceResponse response = dataSourceService.setPreference(keycloakId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Veri kaynağı tercihi kaydedildi"));
    }

    /**
     * Trigger immediate data fetch for a specific API config
     * Called when user activates an API key
     */
    @PostMapping("/trigger/{apiConfigId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerFetch(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID apiConfigId) {
        String keycloakId = jwt.getSubject();
        Map<String, Object> result = dataSourceService.triggerDataFetch(keycloakId, apiConfigId);
        return ResponseEntity.ok(ApiResponse.success(result, "Veri çekme işlemi başlatıldı"));
    }
}
