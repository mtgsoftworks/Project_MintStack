package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.service.AlertWebhookSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/webhooks")
@RequiredArgsConstructor
@Tag(name = "Alertmanager Webhooks", description = "Prometheus Alertmanager webhook alimi")
public class AlertWebhookController {

    private final AlertWebhookSecurityService alertWebhookSecurityService;
    private final ObjectMapper objectMapper;

    @PostMapping("/alerts")
    @Operation(summary = "Alertmanager webhook payload al")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receiveAlertmanagerPayload(
        @RequestBody String rawPayload,
        HttpServletRequest request
    ) {
        alertWebhookSecurityService.validateRequest(request, rawPayload);

        Map<String, Object> payload = parsePayload(rawPayload);
        Object status = payload.getOrDefault("status", "unknown");
        Object alerts = payload.getOrDefault("alerts", List.of());
        int alertCount = alerts instanceof List<?> list ? list.size() : 0;
        log.warn("Alertmanager payload received: status={}, alertCount={}", status, alertCount);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "accepted", true,
            "status", status,
            "alertCount", alertCount
        )));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String rawPayload) {
        try {
            if (rawPayload == null || rawPayload.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(rawPayload, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Webhook payload JSON formati gecersiz");
        }
    }
}
