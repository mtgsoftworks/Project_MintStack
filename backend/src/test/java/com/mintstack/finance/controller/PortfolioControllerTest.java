package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.dto.request.CreatePortfolioRequest;
import com.mintstack.finance.dto.response.PortfolioResponse;
import com.mintstack.finance.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
@Import(CorsProperties.class)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PortfolioService portfolioService;

    @Test
    @WithMockUser
    void getUserPortfolios_ShouldReturnPortfolios() throws Exception {
        // Given
        PortfolioResponse portfolio = PortfolioResponse.builder()
            .id(UUID.randomUUID())
            .name("Test Portfolio")
            .description("Test Description")
            .totalValue(BigDecimal.valueOf(10000))
            .totalCost(BigDecimal.valueOf(9000))
            .profitLoss(BigDecimal.valueOf(1000))
            .profitLossPercent(BigDecimal.valueOf(11.11))
            .itemCount(5)
            .isDefault(true)
            .build();

        when(portfolioService.getUserPortfolios(any())).thenReturn(List.of(portfolio));

        // When & Then
        mockMvc.perform(get("/api/v1/portfolio")
                .with(jwt().jwt(j -> j.subject("test-user"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].name").value("Test Portfolio"));
    }

    @Test
    @WithMockUser
    void getPortfolioById_ShouldReturnPortfolio() throws Exception {
        // Given
        UUID portfolioId = UUID.randomUUID();
        PortfolioResponse portfolio = PortfolioResponse.builder()
            .id(portfolioId)
            .name("Test Portfolio")
            .description("Test Description")
            .totalValue(BigDecimal.valueOf(10000))
            .totalCost(BigDecimal.valueOf(9000))
            .profitLoss(BigDecimal.valueOf(1000))
            .profitLossPercent(BigDecimal.valueOf(11.11))
            .itemCount(5)
            .isDefault(true)
            .build();

        when(portfolioService.getPortfolio(any(), eq(portfolioId))).thenReturn(portfolio);

        // When & Then
        mockMvc.perform(get("/api/v1/portfolio/" + portfolioId)
                .with(jwt().jwt(j -> j.subject("test-user"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Test Portfolio"));
    }

    @Test
    @WithMockUser
    void createPortfolio_ShouldCreateAndReturnPortfolio() throws Exception {
        // Given
        CreatePortfolioRequest request = CreatePortfolioRequest.builder()
            .name("New Portfolio")
            .description("New Description")
            .build();

        PortfolioResponse response = PortfolioResponse.builder()
            .id(UUID.randomUUID())
            .name("New Portfolio")
            .description("New Description")
            .totalValue(BigDecimal.ZERO)
            .totalCost(BigDecimal.ZERO)
            .profitLoss(BigDecimal.ZERO)
            .profitLossPercent(BigDecimal.ZERO)
            .itemCount(0)
            .isDefault(false)
            .build();

        when(portfolioService.createPortfolio(any(), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/portfolio")
                .with(jwt().jwt(j -> j.subject("test-user")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("New Portfolio"));
    }

    @Test
    @WithMockUser
    void deletePortfolio_ShouldDeletePortfolio() throws Exception {
        // Given
        UUID portfolioId = UUID.randomUUID();
        doNothing().when(portfolioService).deletePortfolio(any(), eq(portfolioId));

        // When & Then
        mockMvc.perform(delete("/api/v1/portfolio/" + portfolioId)
                .with(jwt().jwt(j -> j.subject("test-user")))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
