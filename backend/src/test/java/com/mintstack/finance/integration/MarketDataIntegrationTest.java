package com.mintstack.finance.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Market Data API with real database.
 */
class MarketDataIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/market/stocks - Returns stock list")
    void getStocks_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/market/stocks")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/market/bonds - Returns bonds list")
    void getBonds_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/market/bonds")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Market endpoints are publicly readable")
    void getStocks_PublicEndpoint_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/market/stocks")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
