package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.config.SecurityConfig;
import com.mintstack.finance.dto.request.ApiConfigRequest;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.dto.response.ApiConfigResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.service.SettingsService;
import com.mintstack.finance.service.UserService;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.simulation.SimulationDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingsController.class)
@Import({CorsProperties.class, SecurityConfig.class})
@AutoConfigureDataJpa
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SettingsService settingsService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @MockitoBean
    private MarketDataService marketDataService;

    @MockitoBean
    private SimulationDataService simulationDataService;

    @MockitoBean
    private CacheManager cacheManager;

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
    void addApiConfig_ShouldAllowDeactivateWithoutApiKey() throws Exception {
        // Given
        User user = createTestUser();
        ApiConfigRequest request = new ApiConfigRequest();
        request.setProvider(ApiProvider.ALPHA_VANTAGE);
        request.setApiKey("");
        request.setIsActive(false);

        ApiConfigResponse response = ApiConfigResponse.builder()
            .id(UUID.randomUUID())
            .provider(ApiProvider.ALPHA_VANTAGE)
            .isActive(false)
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
            .andExpect(jsonPath("$.data.provider").value("ALPHA_VANTAGE"))
            .andExpect(jsonPath("$.data.isActive").value(false));
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

    @Test
    void clearCache_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/settings/cache")
                .with(jwt()
                    .jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))
                    .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void clearCache_ShouldReturnSuccess_WhenAdmin() throws Exception {
        Cache cache = org.mockito.Mockito.mock(Cache.class);
        when(cacheManager.getCacheNames()).thenReturn(Set.of("currencies"));
        when(cacheManager.getCache("currencies")).thenReturn(cache);

        mockMvc.perform(delete("/api/v1/settings/cache")
                .with(jwt()
                    .jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteMarketData_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/settings/market-data")
                .with(jwt()
                    .jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))
                    .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteMarketData_ShouldReturnSuccess_WhenAdmin() throws Exception {
        when(marketDataService.deleteAllMarketData()).thenReturn(Map.of("deletedCurrencyRates", 3L));
        when(simulationDataService.isSimulationEnabled()).thenReturn(false);
        when(simulationDataService.deleteSimulationData()).thenReturn(Map.of(
            "deletedInstruments", 0L,
            "deactivatedInstruments", 0L,
            "deletedCurrencyRates", 0L
        ));
        when(cacheManager.getCacheNames()).thenReturn(Set.of());

        mockMvc.perform(delete("/api/v1/settings/market-data")
                .with(jwt()
                    .jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
