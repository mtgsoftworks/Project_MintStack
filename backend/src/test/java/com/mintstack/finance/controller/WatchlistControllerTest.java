package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.dto.request.CreateWatchlistRequest;
import com.mintstack.finance.dto.response.WatchlistResponse;
import com.mintstack.finance.service.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WatchlistController.class)
@Import(CorsProperties.class)
@AutoConfigureDataJpa
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WatchlistService watchlistService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    private static final String TEST_KEYCLOAK_ID = "test-keycloak-id-123";

    private WatchlistResponse createTestWatchlist() {
        return WatchlistResponse.builder()
            .id(UUID.randomUUID())
            .name("My Watchlist")
            .isDefault(true)
            .itemCount(5)
            .items(Collections.emptyList())
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    void getUserWatchlists_ShouldReturnWatchlists() throws Exception {
        // Given
        WatchlistResponse watchlist = createTestWatchlist();
        when(watchlistService.getUserWatchlists(TEST_KEYCLOAK_ID)).thenReturn(List.of(watchlist));

        // When & Then
        mockMvc.perform(get("/api/v1/watchlist")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].name").value("My Watchlist"));
    }

    @Test
    void getWatchlist_ShouldReturnSpecificWatchlist() throws Exception {
        // Given
        UUID watchlistId = UUID.randomUUID();
        WatchlistResponse watchlist = createTestWatchlist();

        when(watchlistService.getWatchlist(TEST_KEYCLOAK_ID, watchlistId)).thenReturn(watchlist);

        // When & Then
        mockMvc.perform(get("/api/v1/watchlist/{id}", watchlistId)
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("My Watchlist"));
    }

    @Test
    void createWatchlist_ShouldReturnCreatedWatchlist() throws Exception {
        // Given
        CreateWatchlistRequest request = new CreateWatchlistRequest();
        request.setName("New Watchlist");

        WatchlistResponse response = WatchlistResponse.builder()
            .id(UUID.randomUUID())
            .name("New Watchlist")
            .isDefault(false)
            .itemCount(0)
            .items(Collections.emptyList())
            .createdAt(LocalDateTime.now())
            .build();

        when(watchlistService.createWatchlist(eq(TEST_KEYCLOAK_ID), any(CreateWatchlistRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/watchlist")
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("New Watchlist"));
    }

    @Test
    void updateWatchlist_ShouldReturnUpdatedWatchlist() throws Exception {
        // Given
        UUID watchlistId = UUID.randomUUID();
        CreateWatchlistRequest request = new CreateWatchlistRequest();
        request.setName("Updated Watchlist");

        WatchlistResponse response = WatchlistResponse.builder()
            .id(watchlistId)
            .name("Updated Watchlist")
            .isDefault(false)
            .itemCount(0)
            .items(Collections.emptyList())
            .createdAt(LocalDateTime.now())
            .build();

        when(watchlistService.updateWatchlist(eq(TEST_KEYCLOAK_ID), eq(watchlistId), any(CreateWatchlistRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/v1/watchlist/{id}", watchlistId)
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Updated Watchlist"));
    }

    @Test
    void deleteWatchlist_ShouldReturnSuccess() throws Exception {
        // Given
        UUID watchlistId = UUID.randomUUID();
        doNothing().when(watchlistService).deleteWatchlist(TEST_KEYCLOAK_ID, watchlistId);

        // When & Then
        mockMvc.perform(delete("/api/v1/watchlist/{id}", watchlistId)
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void addInstrument_ShouldReturnUpdatedWatchlist() throws Exception {
        // Given
        UUID watchlistId = UUID.randomUUID();
        String symbol = "THYAO";

        WatchlistResponse response = WatchlistResponse.builder()
            .id(watchlistId)
            .name("My Watchlist")
            .isDefault(true)
            .itemCount(6)
            .items(Collections.emptyList())
            .createdAt(LocalDateTime.now())
            .build();

        when(watchlistService.addInstrument(TEST_KEYCLOAK_ID, watchlistId, symbol)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/watchlist/{id}/items/{symbol}", watchlistId, symbol)
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.itemCount").value(6));
    }

    @Test
    void removeInstrument_ShouldReturnUpdatedWatchlist() throws Exception {
        // Given
        UUID watchlistId = UUID.randomUUID();
        String symbol = "THYAO";

        WatchlistResponse response = WatchlistResponse.builder()
            .id(watchlistId)
            .name("My Watchlist")
            .isDefault(true)
            .itemCount(4)
            .items(Collections.emptyList())
            .createdAt(LocalDateTime.now())
            .build();

        when(watchlistService.removeInstrument(TEST_KEYCLOAK_ID, watchlistId, symbol)).thenReturn(response);

        // When & Then
        mockMvc.perform(delete("/api/v1/watchlist/{id}/items/{symbol}", watchlistId, symbol)
                .with(jwt().jwt(jwt -> jwt.subject(TEST_KEYCLOAK_ID))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.itemCount").value(4));
    }

    @Test
    void getUserWatchlists_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/watchlist"))
            .andExpect(status().isUnauthorized());
    }
}
