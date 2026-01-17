package com.mintstack.finance.controller;

import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.config.SecurityConfig;
import com.mintstack.finance.dto.response.AdminDashboardResponse;
import com.mintstack.finance.dto.response.UserAdminResponse;
import com.mintstack.finance.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import({CorsProperties.class, SecurityConfig.class})
@AutoConfigureDataJpa
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDashboard_ShouldReturnDashboardStats() throws Exception {
        // Given
        AdminDashboardResponse dashboard = AdminDashboardResponse.builder()
            .totalUsers(100L)
            .activeUsers(85L)
            .totalPortfolios(50L)
            .build();

        when(adminService.getDashboardStats()).thenReturn(dashboard);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalUsers").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_ShouldReturnPaginatedUsers() throws Exception {
        // Given
        UserAdminResponse user = UserAdminResponse.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .isActive(true)
            .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<UserAdminResponse> page = new PageImpl<>(List.of(user), pageable, 1);
        when(adminService.getAllUsers(any(Pageable.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].email").value("test@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_ShouldReturnUser() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserAdminResponse user = UserAdminResponse.builder()
            .id(userId)
            .email("admin@example.com")
            .firstName("Admin")
            .lastName("User")
            .isActive(true)
            .build();

        when(adminService.getUserById(userId)).thenReturn(user);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/users/{id}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("admin@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchUsers_ShouldReturnMatchingUsers() throws Exception {
        // Given
        UserAdminResponse user = UserAdminResponse.builder()
            .id(UUID.randomUUID())
            .email("search@example.com")
            .firstName("Search")
            .lastName("User")
            .isActive(true)
            .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<UserAdminResponse> page = new PageImpl<>(List.of(user), pageable, 1);
        when(adminService.searchUsers(eq("search"), any(Pageable.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/users/search").param("query", "search"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].email").value("search@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateUser_ShouldReturnSuccess() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        doNothing().when(adminService).activateUser(userId);

        // When & Then
        mockMvc.perform(put("/api/v1/admin/users/{id}/activate", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateUser_ShouldReturnSuccess() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        doNothing().when(adminService).deactivateUser(userId);

        // When & Then
        mockMvc.perform(put("/api/v1/admin/users/{id}/deactivate", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getDashboard_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/admin/dashboard"))
            .andExpect(status().isForbidden());
    }
}
