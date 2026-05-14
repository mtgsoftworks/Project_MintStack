package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.UpdateRuntimeSettingRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.RuntimeSettingResponse;
import com.mintstack.finance.service.RuntimeSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/runtime-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearer")
@Tag(name = "Admin Runtime Settings", description = "Runtime ayar versiyonlama ve yonetimi")
public class RuntimeSettingsController {

    private final RuntimeSettingsService runtimeSettingsService;

    @GetMapping
    @Operation(summary = "Runtime ayarlari listele")
    public ResponseEntity<ApiResponse<List<RuntimeSettingResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(runtimeSettingsService.getAll()));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Runtime ayar guncelle")
    public ResponseEntity<ApiResponse<RuntimeSettingResponse>> update(
        @PathVariable String key,
        @Valid @RequestBody UpdateRuntimeSettingRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        String updatedBy = jwt != null ? jwt.getSubject() : "system";
        return ResponseEntity.ok(ApiResponse.success(runtimeSettingsService.update(key, request, updatedBy)));
    }
}
