package com.mintstack.finance.controller;

import com.mintstack.finance.entity.SimulationConfig;
import com.mintstack.finance.entity.SimulationConfig.MarketTrend;
import com.mintstack.finance.entity.SimulationConfig.VolatilityLevel;
import com.mintstack.finance.scheduler.SimulationScheduler;
import com.mintstack.finance.service.simulation.SimulationDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SimulationController.class)
@DisplayName("SimulationController Tests")
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SimulationDataService simulationDataService;

    @MockBean
    private SimulationScheduler simulationScheduler;

    private SimulationConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = SimulationConfig.builder()
                .isEnabled(true)
                .volatilityLevel(VolatilityLevel.MEDIUM)
                .marketTrend(MarketTrend.NEUTRAL)
                .updateIntervalSeconds(5)
                .enableRandomEvents(true)
                .enableMarketHours(false)
                .build();
        testConfig.setId(UUID.randomUUID());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/simulation/config config döndürmeli")
    void testGetConfig_ReturnsConfig() throws Exception {
        // Given
        when(simulationDataService.getConfig()).thenReturn(testConfig);

        // When & Then
        mockMvc.perform(get("/api/v1/simulation/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.volatilityLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.data.marketTrend").value("NEUTRAL"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/simulation/config config güncellemeli")
    void testUpdateConfig_ReturnsUpdatedConfig() throws Exception {
        // Given
        SimulationConfig updatedConfig = SimulationConfig.builder()
                .isEnabled(true)
                .volatilityLevel(VolatilityLevel.HIGH)
                .marketTrend(MarketTrend.BULLISH)
                .updateIntervalSeconds(10)
                .enableRandomEvents(false)
                .enableMarketHours(true)
                .build();
        
        when(simulationDataService.updateConfig(any(), any(), any(), any(), any()))
                .thenReturn(updatedConfig);

        Map<String, Object> request = new HashMap<>();
        request.put("enabled", true);
        request.put("volatilityLevel", "HIGH");
        request.put("marketTrend", "BULLISH");
        request.put("updateIntervalSeconds", 10);
        request.put("enableRandomEvents", false);

        // When & Then
        mockMvc.perform(post("/api/v1/simulation/config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.volatilityLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.marketTrend").value("BULLISH"));

        verify(simulationDataService).updateConfig(
                eq(true), eq(VolatilityLevel.HIGH), eq(10), eq(MarketTrend.BULLISH), eq(false));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/simulation/toggle simülasyonu toggle etmeli")
    void testToggle_TogglesSimulation() throws Exception {
        // Given
        when(simulationDataService.getConfig()).thenReturn(testConfig);
        
        SimulationConfig toggledConfig = SimulationConfig.builder()
                .isEnabled(false)
                .volatilityLevel(VolatilityLevel.MEDIUM)
                .marketTrend(MarketTrend.NEUTRAL)
                .updateIntervalSeconds(5)
                .enableRandomEvents(true)
                .enableMarketHours(false)
                .build();
        
        when(simulationDataService.updateConfig(eq(false), any(), any(), any(), any()))
                .thenReturn(toggledConfig);

        // When & Then
        mockMvc.perform(post("/api/v1/simulation/toggle")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/simulation/reset simülasyonu sıfırlamalı")
    void testReset_ResetsSimulation() throws Exception {
        // Given
        doNothing().when(simulationDataService).resetSimulation();
        doNothing().when(simulationScheduler).resetTickCount();

        // When & Then
        mockMvc.perform(post("/api/v1/simulation/reset")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(simulationDataService).resetSimulation();
        verify(simulationScheduler).resetTickCount();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/simulation/status tam durum döndürmeli")
    void testGetStatus_ReturnsCompleteStatus() throws Exception {
        // Given
        when(simulationDataService.getConfig()).thenReturn(testConfig);
        when(simulationScheduler.getTickCount()).thenReturn(100L);
        
        Map<String, SimulationDataService.SimulatedStock> stocks = new HashMap<>();
        stocks.put("THYAO", new SimulationDataService.SimulatedStock("THY", "BIST", 100.0, 0.02));
        when(simulationDataService.getStocks()).thenReturn(stocks);

        Map<String, SimulationDataService.SimulatedCurrency> currencies = new HashMap<>();
        currencies.put("USD", new SimulationDataService.SimulatedCurrency("USD", 38.0, 38.5, 0.01));
        when(simulationDataService.getCurrencies()).thenReturn(currencies);

        Map<String, SimulationDataService.SimulatedIndex> indices = new HashMap<>();
        indices.put("XU100", new SimulationDataService.SimulatedIndex("BIST 100", 9850.0, 0.015));
        when(simulationDataService.getIndices()).thenReturn(indices);

        // When & Then
        mockMvc.perform(get("/api/v1/simulation/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.tickCount").value(100))
                .andExpect(jsonPath("$.data.stockCount").value(1))
                .andExpect(jsonPath("$.data.currencyCount").value(1))
                .andExpect(jsonPath("$.data.indexCount").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/simulation/stocks hisseleri döndürmeli")
    void testGetStocks_ReturnsStocks() throws Exception {
        // Given
        Map<String, SimulationDataService.SimulatedStock> stocks = new HashMap<>();
        stocks.put("THYAO", new SimulationDataService.SimulatedStock("Türk Hava Yolları", "BIST", 285.50, 0.025));
        stocks.put("GARAN", new SimulationDataService.SimulatedStock("Garanti BBVA", "BIST", 125.80, 0.022));
        when(simulationDataService.getStocks()).thenReturn(stocks);

        // When & Then
        mockMvc.perform(get("/api/v1/simulation/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.THYAO").exists())
                .andExpect(jsonPath("$.data.THYAO.name").value("Türk Hava Yolları"))
                .andExpect(jsonPath("$.data.GARAN").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/simulation/currencies dövizleri döndürmeli")
    void testGetCurrencies_ReturnsCurrencies() throws Exception {
        // Given
        Map<String, SimulationDataService.SimulatedCurrency> currencies = new HashMap<>();
        currencies.put("USD", new SimulationDataService.SimulatedCurrency("ABD Doları", 38.42, 38.58, 0.008));
        currencies.put("EUR", new SimulationDataService.SimulatedCurrency("Euro", 40.15, 40.38, 0.009));
        when(simulationDataService.getCurrencies()).thenReturn(currencies);

        // When & Then
        mockMvc.perform(get("/api/v1/simulation/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.USD").exists())
                .andExpect(jsonPath("$.data.USD.name").value("ABD Doları"))
                .andExpect(jsonPath("$.data.EUR").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/v1/simulation/indices endeksleri döndürmeli")
    void testGetIndices_ReturnsIndices() throws Exception {
        // Given
        Map<String, SimulationDataService.SimulatedIndex> indices = new HashMap<>();
        indices.put("XU100", new SimulationDataService.SimulatedIndex("BIST 100", 9850.0, 0.015));
        indices.put("XU030", new SimulationDataService.SimulatedIndex("BIST 30", 10200.0, 0.016));
        when(simulationDataService.getIndices()).thenReturn(indices);

        // When & Then
        mockMvc.perform(get("/api/v1/simulation/indices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.XU100").exists())
                .andExpect(jsonPath("$.data.XU100.name").value("BIST 100"))
                .andExpect(jsonPath("$.data.XU030").exists());
    }

    @Test
    @DisplayName("Yetkisiz erişim 401 döndürmeli")
    void testUnauthorizedAccess_Returns401() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/simulation/config"))
                .andExpect(status().isUnauthorized());
    }
}
