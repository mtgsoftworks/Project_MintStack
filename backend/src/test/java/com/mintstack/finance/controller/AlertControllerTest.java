package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.dto.request.CreateAlertRequest;
import com.mintstack.finance.entity.PriceAlert.AlertType;
import com.mintstack.finance.dto.response.AlertResponse;
import com.mintstack.finance.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertController.class)
@Import(CorsProperties.class)
@AutoConfigureDataJpa
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    private static final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";

    @Test
    void getUserAlerts_ShouldReturnAlerts() throws Exception {
        // Given
        AlertResponse alert = AlertResponse.builder()
            .id(UUID.randomUUID())
            .symbol("USD/TRY")
            .targetValue(BigDecimal.valueOf(34.50))
            .alertType(AlertType.PRICE_ABOVE.name())
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();

        when(alertService.getUserAlerts(TEST_KEYCLOAK_ID)).thenReturn(List.of(alert));

        // When & Then
        mockMvc.perform(get("/api/v1/alerts")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].symbol").value("USD/TRY"));
    }

    @Test
    void getActiveAlerts_ShouldReturnActiveAlerts() throws Exception {
        // Given
        AlertResponse alert = AlertResponse.builder()
            .id(UUID.randomUUID())
            .symbol("EUR/TRY")
            .targetValue(BigDecimal.valueOf(36.00))
            .alertType(AlertType.PRICE_BELOW.name())
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();

        when(alertService.getActiveAlerts(TEST_KEYCLOAK_ID)).thenReturn(List.of(alert));

        // When & Then
        mockMvc.perform(get("/api/v1/alerts/active")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].isActive").value(true));
    }

    @Test
    void createAlert_ShouldReturnCreatedAlert() throws Exception {
        // Given
        CreateAlertRequest request = new CreateAlertRequest();
        request.setSymbol("USD/TRY");
        request.setTargetValue(BigDecimal.valueOf(35.00));
        request.setAlertType(AlertType.PRICE_ABOVE);

        AlertResponse response = AlertResponse.builder()
            .id(UUID.randomUUID())
            .symbol("USD/TRY")
            .targetValue(BigDecimal.valueOf(35.00))
            .alertType(AlertType.PRICE_ABOVE.name())
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();

        when(alertService.createAlert(eq(TEST_KEYCLOAK_ID), any(CreateAlertRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/alerts")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.symbol").value("USD/TRY"));
    }

    @Test
    void deleteAlert_ShouldReturnSuccess() throws Exception {
        // Given
        UUID alertId = UUID.randomUUID();
        doNothing().when(alertService).deleteAlert(TEST_KEYCLOAK_ID, alertId);

        // When & Then
        mockMvc.perform(delete("/api/v1/alerts/{id}", alertId)
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deactivateAlert_ShouldReturnSuccess() throws Exception {
        // Given
        UUID alertId = UUID.randomUUID();
        doNothing().when(alertService).deactivateAlert(TEST_KEYCLOAK_ID, alertId);

        // When & Then
        mockMvc.perform(put("/api/v1/alerts/{id}/deactivate", alertId)
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getUserAlerts_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/alerts"))
            .andExpect(status().isUnauthorized());
    }
}
