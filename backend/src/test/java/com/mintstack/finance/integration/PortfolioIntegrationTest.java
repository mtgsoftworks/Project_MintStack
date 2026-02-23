package com.mintstack.finance.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Portfolio API with real database.
 */
class PortfolioIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor authenticatedJwt() {
        return jwt().jwt(jwt -> jwt
                .subject("integration-test-user")
                .claim("email", "integration@test.local")
                .claim("given_name", "Integration")
                .claim("family_name", "User"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/v1/portfolios - Unauthenticated returns 401")
    void getPortfolios_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/portfolios - Authenticated returns 200")
    void getPortfolios_Authenticated_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios")
                .with(authenticatedJwt())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/portfolios/{id} - Non-existent portfolio returns 404")
    void getPortfolio_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/00000000-0000-0000-0000-000000000000")
                .with(authenticatedJwt())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
