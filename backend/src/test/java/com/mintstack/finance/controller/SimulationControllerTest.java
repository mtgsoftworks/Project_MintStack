package com.mintstack.finance.controller;

import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.entity.SimulationConfig;
import com.mintstack.finance.entity.SimulationConfig.MarketTrend;
import com.mintstack.finance.entity.SimulationConfig.VolatilityLevel;
import com.mintstack.finance.scheduler.SimulationScheduler;
import com.mintstack.finance.service.PriceCacheService;
import com.mintstack.finance.service.simulation.MarketEventEngine;
import com.mintstack.finance.service.simulation.PriceSimulationEngine;
import com.mintstack.finance.service.simulation.SimulatedCurrency;
import com.mintstack.finance.service.simulation.SimulatedIndex;
import com.mintstack.finance.service.simulation.SimulatedStock;
import com.mintstack.finance.service.simulation.SimulationDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulationController.class)
@DisplayName("SimulationController Tests")
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SimulationDataService simulationDataService;

    @MockitoBean
    private SimulationScheduler simulationScheduler;

    @MockitoBean
    private PriceSimulationEngine priceSimulationEngine;

    @MockitoBean
    private MarketEventEngine marketEventEngine;

    @MockitoBean
    private PriceCacheService priceCacheService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/simulation/config config dÃ¶ndÃ¼rmeli")
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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/simulation/config config gÃ¼ncellemeli")
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
        
        when(simulationDataService.updateConfig(any(), any(), any(), any(), any(), any()))
                .thenReturn(updatedConfig);

        Map<String, Object> request = new HashMap<>();
        request.put("enabled", true);
        request.put("volatilityLevel", "HIGH");
        request.put("marketTrend", "BULLISH");
        request.put("updateIntervalSeconds", 10);
        request.put("enableRandomEvents", false);
        request.put("enableMarketHours", true);

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
                eq(true), eq(VolatilityLevel.HIGH), eq(10), eq(MarketTrend.BULLISH), eq(false), eq(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/simulation/toggle simÃ¼lasyonu toggle etmeli")
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
        
        when(simulationDataService.updateConfig(eq(false), any(), any(), any(), any(), any()))
                .thenReturn(toggledConfig);

        // When & Then
        mockMvc.perform(post("/api/v1/simulation/toggle")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/simulation/reset simÃ¼lasyonu sÄ±fÄ±rlamalÄ±")
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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/simulation/status tam durum dÃ¶ndÃ¼rmeli")
    void testGetStatus_ReturnsCompleteStatus() throws Exception {
        // Given
        when(simulationDataService.getConfig()).thenReturn(testConfig);
        when(simulationScheduler.getTickCount()).thenReturn(100L);
        
        Map<String, SimulatedStock> stocks = new HashMap<>();
        stocks.put("THYAO", new SimulatedStock("THY", "BIST", 100.0, 0.02));
        when(simulationDataService.getStocks()).thenReturn(stocks);
        when(simulationDataService.getBonds()).thenReturn(new HashMap<>());
        when(simulationDataService.getFunds()).thenReturn(new HashMap<>());
        when(simulationDataService.getViop()).thenReturn(new HashMap<>());

        Map<String, SimulatedCurrency> currencies = new HashMap<>();
        currencies.put("USD", new SimulatedCurrency("USD", 38.0, 38.5, 0.01));
        when(simulationDataService.getCurrencies()).thenReturn(currencies);

        Map<String, SimulatedIndex> indices = new HashMap<>();
        indices.put("XU100", new SimulatedIndex("BIST 100", 9850.0, 0.015));
        when(simulationDataService.getIndices()).thenReturn(indices);
        when(simulationDataService.getCryptos()).thenReturn(new HashMap<>());

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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/simulation/stocks hisseleri dÃ¶ndÃ¼rmeli")
    void testGetStocks_ReturnsStocks() throws Exception {
        // Given
        Map<String, SimulatedStock> stocks = new HashMap<>();
        stocks.put("THYAO", new SimulatedStock("TÃ¼rk Hava YollarÄ±", "BIST", 285.50, 0.025));
        stocks.put("GARAN", new SimulatedStock("Garanti BBVA", "BIST", 125.80, 0.022));
        when(simulationDataService.getStocks()).thenReturn(stocks);

        // When & Then
        mockMvc.perform(get("/api/v1/simulation/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.THYAO").exists())
                .andExpect(jsonPath("$.data.THYAO.name").value("TÃ¼rk Hava YollarÄ±"))
                .andExpect(jsonPath("$.data.GARAN").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/simulation/currencies dÃ¶vizleri dÃ¶ndÃ¼rmeli")
    void testGetCurrencies_ReturnsCurrencies() throws Exception {
        // Given
        Map<String, SimulatedCurrency> currencies = new HashMap<>();
        currencies.put("USD", new SimulatedCurrency("ABD DolarÄ±", 38.42, 38.58, 0.008));
        currencies.put("EUR", new SimulatedCurrency("Euro", 40.15, 40.38, 0.009));
        when(simulationDataService.getCurrencies()).thenReturn(currencies);

        // When & Then
        mockMvc.perform(get("/api/v1/simulation/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.USD").exists())
                .andExpect(jsonPath("$.data.USD.name").value("ABD DolarÄ±"))
                .andExpect(jsonPath("$.data.EUR").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/simulation/indices endeksleri dÃ¶ndÃ¼rmeli")
    void testGetIndices_ReturnsIndices() throws Exception {
        // Given
        Map<String, SimulatedIndex> indices = new HashMap<>();
        indices.put("XU100", new SimulatedIndex("BIST 100", 9850.0, 0.015));
        indices.put("XU030", new SimulatedIndex("BIST 30", 10200.0, 0.016));
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
    @DisplayName("Yetkisiz eriÅŸim 401 dÃ¶ndÃ¼rmeli")
    void testUnauthorizedAccess_Returns401() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/simulation/config"))
                .andExpect(status().isUnauthorized());
    }
}


