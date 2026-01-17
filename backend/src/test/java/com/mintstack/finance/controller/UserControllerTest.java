package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.config.SecurityConfig;
import com.mintstack.finance.dto.request.UpdateProfileRequest;
import com.mintstack.finance.dto.response.UserResponse;
import com.mintstack.finance.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({CorsProperties.class, SecurityConfig.class})
@AutoConfigureDataJpa
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    private static final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";

    @Test
    void getProfile_ShouldReturnUserProfile() throws Exception {
        // Given
        UserResponse userResponse = UserResponse.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .firstName("John")
            .lastName("Doe")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();

        when(userService.getOrCreateUser(any())).thenReturn(null);
        when(userService.getUserProfile(TEST_KEYCLOAK_ID)).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.firstName").value("John"));
    }

    @Test
    void updateProfile_ShouldReturnUpdatedProfile() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");

        UserResponse updatedUser = UserResponse.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .firstName("Jane")
            .lastName("Smith")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();

        when(userService.updateProfile(eq(TEST_KEYCLOAK_ID), any(UpdateProfileRequest.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/me")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.firstName").value("Jane"))
            .andExpect(jsonPath("$.data.lastName").value("Smith"));
    }

    @Test
    void getProfile_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");

        // When & Then
        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
