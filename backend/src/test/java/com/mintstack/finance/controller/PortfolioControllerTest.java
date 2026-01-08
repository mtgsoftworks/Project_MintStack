package com.mintstack.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.dto.request.AddPortfolioItemRequest;
import com.mintstack.finance.dto.request.CreatePortfolioRequest;
import com.mintstack.finance.dto.response.PortfolioItemResponse;
import com.mintstack.finance.dto.response.PortfolioResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.service.PortfolioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PortfolioController.class)
@ActiveProfiles("test")
@DisplayName("PortfolioController Tests")
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PortfolioService portfolioService;

    private PortfolioResponse createTestPortfolio() {
        return PortfolioResponse.builder()
                .id(1L)
                .name("Ana Portföy")
                .description("Test portföyü")
                .isDefault(true)
                .totalValue(new BigDecimal("28050.00"))
                .totalCost(new BigDecimal("25000.00"))
                .profitLoss(new BigDecimal("3050.00"))
                .profitLossPercent(new BigDecimal("12.20"))
                .itemCount(1)
                .build();
    }

    private PortfolioItemResponse createTestItem() {
        return PortfolioItemResponse.builder()
                .id(1L)
                .symbol("THYAO")
                .name("Türk Hava Yolları")
                .type(Instrument.InstrumentType.STOCK)
                .quantity(new BigDecimal("100"))
                .averageCost(new BigDecimal("250.00"))
                .currentPrice(new BigDecimal("280.50"))
                .currentValue(new BigDecimal("28050.00"))
                .profitLoss(new BigDecimal("3050.00"))
                .profitLossPercent(new BigDecimal("12.20"))
                .build();
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("GET /api/v1/portfolios - Should return 401 when not authenticated")
        void getPortfolios_whenNotAuthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/v1/portfolios")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/v1/portfolios - Should return 401 when not authenticated")
        void createPortfolio_whenNotAuthenticated_shouldReturn401() throws Exception {
            CreatePortfolioRequest request = new CreatePortfolioRequest();
            request.setName("Test");

            mockMvc.perform(post("/api/v1/portfolios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Get Portfolio Tests")
    class GetPortfolioTests {

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("GET /api/v1/portfolios - Should return user portfolios")
        void getPortfolios_shouldReturnPortfolios() throws Exception {
            // Given
            when(portfolioService.getPortfolios(anyString()))
                    .thenReturn(Arrays.asList(createTestPortfolio()));

            // When/Then
            mockMvc.perform(get("/api/v1/portfolios")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value("Ana Portföy"))
                    .andExpect(jsonPath("$.data[0].totalValue").value(28050.00));
        }

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("GET /api/v1/portfolios - Should return empty list when no portfolios")
        void getPortfolios_whenNoPortfolios_shouldReturnEmptyList() throws Exception {
            // Given
            when(portfolioService.getPortfolios(anyString()))
                    .thenReturn(Collections.emptyList());

            // When/Then
            mockMvc.perform(get("/api/v1/portfolios")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("GET /api/v1/portfolios/{id} - Should return portfolio by id")
        void getPortfolioById_shouldReturnPortfolio() throws Exception {
            // Given
            PortfolioResponse portfolio = createTestPortfolio();
            portfolio.setItems(Arrays.asList(createTestItem()));
            
            when(portfolioService.getPortfolio(anyString(), eq(1L)))
                    .thenReturn(portfolio);

            // When/Then
            mockMvc.perform(get("/api/v1/portfolios/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Ana Portföy"))
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.items[0].symbol").value("THYAO"));
        }

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("GET /api/v1/portfolios/{id} - Should return 404 when not found")
        void getPortfolioById_whenNotFound_shouldReturn404() throws Exception {
            // Given
            when(portfolioService.getPortfolio(anyString(), eq(999L)))
                    .thenThrow(new ResourceNotFoundException("Portföy", "id", "999"));

            // When/Then
            mockMvc.perform(get("/api/v1/portfolios/999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Create Portfolio Tests")
    class CreatePortfolioTests {

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("POST /api/v1/portfolios - Should create new portfolio")
        void createPortfolio_shouldCreate() throws Exception {
            // Given
            CreatePortfolioRequest request = new CreatePortfolioRequest();
            request.setName("Yeni Portföy");
            request.setDescription("Test açıklama");

            PortfolioResponse response = PortfolioResponse.builder()
                    .id(2L)
                    .name("Yeni Portföy")
                    .description("Test açıklama")
                    .isDefault(false)
                    .totalValue(BigDecimal.ZERO)
                    .build();

            when(portfolioService.createPortfolio(anyString(), any(CreatePortfolioRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/portfolios")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Yeni Portföy"));
        }

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("POST /api/v1/portfolios - Should return 400 when name is empty")
        void createPortfolio_whenNameEmpty_shouldReturn400() throws Exception {
            // Given
            CreatePortfolioRequest request = new CreatePortfolioRequest();
            request.setName(""); // Empty name

            // When/Then
            mockMvc.perform(post("/api/v1/portfolios")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Update Portfolio Tests")
    class UpdatePortfolioTests {

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("PUT /api/v1/portfolios/{id} - Should update portfolio")
        void updatePortfolio_shouldUpdate() throws Exception {
            // Given
            CreatePortfolioRequest request = new CreatePortfolioRequest();
            request.setName("Güncellenmiş Portföy");
            request.setDescription("Güncellenmiş açıklama");

            PortfolioResponse response = PortfolioResponse.builder()
                    .id(1L)
                    .name("Güncellenmiş Portföy")
                    .description("Güncellenmiş açıklama")
                    .build();

            when(portfolioService.updatePortfolio(anyString(), eq(1L), any(CreatePortfolioRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(put("/api/v1/portfolios/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Güncellenmiş Portföy"));
        }
    }

    @Nested
    @DisplayName("Delete Portfolio Tests")
    class DeletePortfolioTests {

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("DELETE /api/v1/portfolios/{id} - Should delete portfolio")
        void deletePortfolio_shouldDelete() throws Exception {
            // Given
            doNothing().when(portfolioService).deletePortfolio(anyString(), eq(1L));

            // When/Then
            mockMvc.perform(delete("/api/v1/portfolios/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(portfolioService, times(1)).deletePortfolio(anyString(), eq(1L));
        }
    }

    @Nested
    @DisplayName("Portfolio Item Tests")
    class PortfolioItemTests {

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("POST /api/v1/portfolios/{id}/items - Should add item to portfolio")
        void addItem_shouldAddItem() throws Exception {
            // Given
            AddPortfolioItemRequest request = new AddPortfolioItemRequest();
            request.setSymbol("GARAN");
            request.setQuantity(new BigDecimal("50"));
            request.setAverageCost(new BigDecimal("45.00"));

            PortfolioItemResponse response = PortfolioItemResponse.builder()
                    .id(2L)
                    .symbol("GARAN")
                    .name("Garanti Bankası")
                    .quantity(new BigDecimal("50"))
                    .averageCost(new BigDecimal("45.00"))
                    .currentPrice(new BigDecimal("50.00"))
                    .currentValue(new BigDecimal("2500.00"))
                    .build();

            when(portfolioService.addItem(anyString(), eq(1L), any(AddPortfolioItemRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/portfolios/1/items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.symbol").value("GARAN"));
        }

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("PUT /api/v1/portfolios/{id}/items/{itemId} - Should update item")
        void updateItem_shouldUpdate() throws Exception {
            // Given
            AddPortfolioItemRequest request = new AddPortfolioItemRequest();
            request.setQuantity(new BigDecimal("150"));
            request.setAverageCost(new BigDecimal("260.00"));

            PortfolioItemResponse response = createTestItem();
            response.setQuantity(new BigDecimal("150"));

            when(portfolioService.updateItem(anyString(), eq(1L), eq(1L), any(AddPortfolioItemRequest.class)))
                    .thenReturn(response);

            // When/Then
            mockMvc.perform(put("/api/v1/portfolios/1/items/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("DELETE /api/v1/portfolios/{id}/items/{itemId} - Should remove item")
        void removeItem_shouldRemove() throws Exception {
            // Given
            doNothing().when(portfolioService).removeItem(anyString(), eq(1L), eq(1L));

            // When/Then
            mockMvc.perform(delete("/api/v1/portfolios/1/items/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(portfolioService, times(1)).removeItem(anyString(), eq(1L), eq(1L));
        }
    }

    @Nested
    @DisplayName("Portfolio Summary Tests")
    class PortfolioSummaryTests {

        @Test
        @WithMockUser(username = "test-user", roles = "USER")
        @DisplayName("GET /api/v1/portfolios/summary - Should return portfolio summary")
        void getSummary_shouldReturnSummary() throws Exception {
            // Given
            when(portfolioService.getPortfolios(anyString()))
                    .thenReturn(Arrays.asList(createTestPortfolio()));

            // When/Then
            mockMvc.perform(get("/api/v1/portfolios/summary")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
