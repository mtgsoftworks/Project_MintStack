package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.dto.request.ApiConfigRequest;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.dto.response.ApiConfigResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.service.SettingsService;
import com.mintstack.finance.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettingsController.class)
@Import(CorsProperties.class)
@AutoConfigureDataJpa
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SettingsService settingsService;

    @MockBean
    private UserService userService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    private static final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    private User createTestUser() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setKeycloakId(TEST_KEYCLOAK_ID);
        user.setEmail("test@example.com");
        return user;
    }

    @Test
    void getApiConfigs_ShouldReturnConfigs() throws Exception {
        // Given
        User user = createTestUser();
        ApiConfigResponse config = ApiConfigResponse.builder()
            .id(UUID.randomUUID())
            .provider(ApiProvider.ALPHA_VANTAGE)
            .isActive(true)
            .build();

        when(userService.getUserByKeycloakId(TEST_KEYCLOAK_ID)).thenReturn(user);
        when(settingsService.getApiConfigs(TEST_USER_ID)).thenReturn(List.of(config));

        // When & Then
        mockMvc.perform(get("/api/v1/settings/api-keys")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].provider").value("ALPHA_VANTAGE"));
    }

    @Test
    void addApiConfig_ShouldReturnCreatedConfig() throws Exception {
        // Given
        User user = createTestUser();
        ApiConfigRequest request = new ApiConfigRequest();
        request.setProvider(ApiProvider.YAHOO_FINANCE);
        request.setApiKey("test-api-key-123");

        ApiConfigResponse response = ApiConfigResponse.builder()
            .id(UUID.randomUUID())
            .provider(ApiProvider.YAHOO_FINANCE)
            .isActive(true)
            .build();

        when(userService.getOrCreateUser(any())).thenReturn(user);
        when(settingsService.addApiConfig(eq(TEST_USER_ID), any(ApiConfigRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/settings/api-keys")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.provider").value("YAHOO_FINANCE"));
    }

    @Test
    void deleteApiConfig_ShouldReturnSuccess() throws Exception {
        // Given
        User user = createTestUser();
        UUID configId = UUID.randomUUID();

        when(userService.getUserByKeycloakId(TEST_KEYCLOAK_ID)).thenReturn(user);
        doNothing().when(settingsService).deleteApiConfig(TEST_USER_ID, configId);

        // When & Then
        mockMvc.perform(delete("/api/v1/settings/api-keys/{id}", configId)
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getApiConfigs_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/settings/api-keys"))
            .andExpect(status().isUnauthorized());
    }
}
