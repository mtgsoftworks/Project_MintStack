package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.dto.request.CompareInstrumentsRequest;
import com.mintstack.finance.service.AnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalysisController.class)
@Import(CorsProperties.class)
@AutoConfigureDataJpa
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalysisService analysisService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    private static final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";

    @Test
    void getMovingAverage_ShouldReturnMA() throws Exception {
        // Given
        Map<String, Object> maResult = new HashMap<>();
        maResult.put("symbol", "THYAO");
        maResult.put("period", 20);
        maResult.put("sma", BigDecimal.valueOf(145.50));

        when(analysisService.getMovingAverage(eq("THYAO"), eq(20), any(LocalDate.class), eq("SMA")))
            .thenReturn(maResult);

        // When & Then
        mockMvc.perform(get("/api/v1/analysis/ma/{symbol}", "THYAO")
                .param("period", "20")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.symbol").value("THYAO"));
    }

    @Test
    void getMultipleMovingAverages_ShouldReturnMultipleMA() throws Exception {
        // Given
        Map<String, Object> maResult = new HashMap<>();
        maResult.put("symbol", "SISE");
        maResult.put("ma7", BigDecimal.valueOf(50.20));
        maResult.put("ma25", BigDecimal.valueOf(48.75));
        maResult.put("ma99", BigDecimal.valueOf(45.00));

        when(analysisService.getMultipleMovingAverages(eq("SISE"), any(LocalDate.class))).thenReturn(maResult);

        // When & Then
        mockMvc.perform(get("/api/v1/analysis/ma/multiple/{symbol}", "SISE")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.symbol").value("SISE"));
    }

    @Test
    void getTrendAnalysis_ShouldReturnTrend() throws Exception {
        // Given
        Map<String, Object> trendResult = new HashMap<>();
        trendResult.put("symbol", "GARAN");
        trendResult.put("trend", "UPTREND");
        trendResult.put("strength", 0.75);
        trendResult.put("dayAnalyzed", 30);

        when(analysisService.getTrendAnalysis("GARAN", 30)).thenReturn(trendResult);

        // When & Then
        mockMvc.perform(get("/api/v1/analysis/trend/{symbol}", "GARAN")
                .param("days", "30")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.trend").value("UPTREND"));
    }

    @Test
    void compareInstruments_ShouldReturnComparison() throws Exception {
        // Given
        CompareInstrumentsRequest request = new CompareInstrumentsRequest();
        request.setSymbols(List.of("THYAO", "PGSUS"));
        request.setStartDate(LocalDate.now().minusDays(30));
        request.setEndDate(LocalDate.now());

        Map<String, Object> comparisonResult = new HashMap<>();
        comparisonResult.put("instruments", List.of("THYAO", "PGSUS"));
        comparisonResult.put("THYAO_return", 12.5);
        comparisonResult.put("PGSUS_return", 8.3);

        when(analysisService.compareInstruments(any(CompareInstrumentsRequest.class))).thenReturn(comparisonResult);

        // When & Then
        mockMvc.perform(post("/api/v1/analysis/compare")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMovingAverage_WithDefaultPeriod_ShouldUse20() throws Exception {
        // Given
        Map<String, Object> maResult = new HashMap<>();
        maResult.put("symbol", "AKBNK");
        maResult.put("period", 20);

        when(analysisService.getMovingAverage(eq("AKBNK"), eq(20), any(LocalDate.class), eq("SMA")))
            .thenReturn(maResult);

        // When & Then
        mockMvc.perform(get("/api/v1/analysis/ma/{symbol}", "AKBNK")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.period").value(20));
    }

    @Test
    void getTrendAnalysis_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/analysis/trend/{symbol}", "TEST"))
            .andExpect(status().isUnauthorized());
    }
}
