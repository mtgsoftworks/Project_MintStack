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
 * Integration tests for Admin API - role-based access control.
 */
class AdminIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Admin endpoint - regular user gets 403")
    @WithMockUser(roles = "USER")
    void adminDashboard_RegularUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin endpoint - admin user gets 200")
    @WithMockUser(roles = "ADMIN")
    void adminDashboard_AdminUser_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin endpoint - unauthenticated gets 401")
    void adminDashboard_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
