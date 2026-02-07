package com.mintstack.finance.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
    @DisplayName("GET /api/v1/market/currencies - Returns currency list")
    @WithMockUser(roles = "USER")
    void getCurrencies_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/market/currencies")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/market/stocks - Returns stock list")
    @WithMockUser(roles = "USER")
    void getStocks_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/market/stocks")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/market/bonds - Returns bonds list")
    @WithMockUser(roles = "USER")
    void getBonds_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/market/bonds")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated market request returns 401")
    void getCurrencies_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/market/currencies")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
