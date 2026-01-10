package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.CreateAlertRequest;
import com.mintstack.finance.dto.response.AlertResponse;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.service.AlertService;
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
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getUserAlerts(
            @AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        List<AlertResponse> alerts = alertService.getUserAlerts(keycloakId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getActiveAlerts(
            @AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        List<AlertResponse> alerts = alertService.getActiveAlerts(keycloakId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AlertResponse>> createAlert(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAlertRequest request) {
        String keycloakId = jwt.getSubject();
        AlertResponse alert = alertService.createAlert(keycloakId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(alert, "Fiyat alarmı oluşturuldu"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlert(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        String keycloakId = jwt.getSubject();
        alertService.deleteAlert(keycloakId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Fiyat alarmı silindi"));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAlert(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        String keycloakId = jwt.getSubject();
        alertService.deactivateAlert(keycloakId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Fiyat alarmı devre dışı bırakıldı"));
    }
}
