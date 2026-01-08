package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.CurrencyRateResponse;
import com.mintstack.finance.dto.response.InstrumentResponse;
import com.mintstack.finance.dto.response.PriceHistoryResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.service.MarketDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketDataController.class)
@ActiveProfiles("test")
@DisplayName("MarketDataController Tests")
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketDataService marketDataService;

    private CurrencyRateResponse createUsdRate() {
        return CurrencyRateResponse.builder()
                .id(1L)
                .currencyCode("USD")
                .currencyName("US Dollar")
                .buyingRate(new BigDecimal("32.50"))
                .sellingRate(new BigDecimal("32.80"))
                .averageRate(new BigDecimal("32.65"))
                .source("TCMB")
                .fetchedAt(LocalDateTime.now())
                .build();
    }

    private InstrumentResponse createThyStock() {
        return InstrumentResponse.builder()
                .id(1L)
                .symbol("THYAO")
                .name("Türk Hava Yolları")
                .type(Instrument.InstrumentType.STOCK)
                .exchange("BIST")
                .currency("TRY")
                .currentPrice(new BigDecimal("280.50"))
                .previousClose(new BigDecimal("275.00"))
                .change(new BigDecimal("5.50"))
                .changePercent(new BigDecimal("2.00"))
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("Currency Rate Endpoints")
    class CurrencyRateEndpoints {

        @Test
        @DisplayName("GET /api/v1/market/currencies - Should return currency rates (public)")
        void getCurrencyRates_shouldReturnRates() throws Exception {
            // Given
            when(marketDataService.getLatestCurrencyRates())
                    .thenReturn(Arrays.asList(createUsdRate()));

            // When/Then
            mockMvc.perform(get("/api/v1/market/currencies")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].currencyCode").value("USD"));
        }

        @Test
        @DisplayName("GET /api/v1/market/currencies/{code} - Should return specific currency rate")
        void getCurrencyRate_shouldReturnSpecificRate() throws Exception {
            // Given
            when(marketDataService.getCurrencyRate("USD"))
                    .thenReturn(createUsdRate());

            // When/Then
            mockMvc.perform(get("/api/v1/market/currencies/USD")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currencyCode").value("USD"))
                    .andExpect(jsonPath("$.data.buyingRate").value(32.50));
        }

        @Test
        @DisplayName("GET /api/v1/market/currencies/{code}/history - Should return currency history")
        void getCurrencyHistory_shouldReturnHistory() throws Exception {
            // Given
            when(marketDataService.getCurrencyHistory(eq("USD"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Arrays.asList(createUsdRate()));

            // When/Then
            mockMvc.perform(get("/api/v1/market/currencies/USD/history")
                            .param("startDate", LocalDate.now().minusDays(7).toString())
                            .param("endDate", LocalDate.now().toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("Instrument Endpoints")
    class InstrumentEndpoints {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/v1/market/stocks - Should return stocks (authenticated)")
        void getStocks_shouldReturnStocks() throws Exception {
            // Given
            Page<InstrumentResponse> page = new PageImpl<>(
                    Arrays.asList(createThyStock()),
                    PageRequest.of(0, 20),
                    1
            );
            when(marketDataService.getInstrumentsByType(eq(Instrument.InstrumentType.STOCK), any()))
                    .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/market/stocks")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].symbol").value("THYAO"));
        }

        @Test
        @DisplayName("GET /api/v1/market/stocks - Should return 401 when not authenticated")
        void getStocks_whenNotAuthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(get("/api/v1/market/stocks")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/v1/market/stocks/{symbol} - Should return stock details")
        void getStockBySymbol_shouldReturnStock() throws Exception {
            // Given
            when(marketDataService.getInstrument("THYAO"))
                    .thenReturn(createThyStock());

            // When/Then
            mockMvc.perform(get("/api/v1/market/stocks/THYAO")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.symbol").value("THYAO"))
                    .andExpect(jsonPath("$.data.name").value("Türk Hava Yolları"))
                    .andExpect(jsonPath("$.data.currentPrice").value(280.50));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/v1/market/stocks/search - Should search stocks")
        void searchStocks_shouldReturnMatchingStocks() throws Exception {
            // Given
            Page<InstrumentResponse> page = new PageImpl<>(
                    Arrays.asList(createThyStock()),
                    PageRequest.of(0, 20),
                    1
            );
            when(marketDataService.searchInstruments(eq(Instrument.InstrumentType.STOCK), eq("THY"), any()))
                    .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/market/stocks/search")
                            .param("q", "THY")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].symbol").value("THYAO"));
        }
    }

    @Nested
    @DisplayName("Price History Endpoints")
    class PriceHistoryEndpoints {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/v1/market/stocks/{symbol}/history - Should return price history")
        void getPriceHistory_shouldReturnHistory() throws Exception {
            // Given
            PriceHistoryResponse history = PriceHistoryResponse.builder()
                    .date(LocalDate.now().minusDays(1))
                    .open(new BigDecimal("275.00"))
                    .high(new BigDecimal("282.00"))
                    .low(new BigDecimal("273.00"))
                    .close(new BigDecimal("280.50"))
                    .volume(1500000L)
                    .build();

            when(marketDataService.getPriceHistory(eq("THYAO"), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(Arrays.asList(history));

            // When/Then
            mockMvc.perform(get("/api/v1/market/stocks/THYAO/history")
                            .param("startDate", LocalDate.now().minusDays(30).toString())
                            .param("endDate", LocalDate.now().toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].close").value(280.50));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/v1/market/stocks/{symbol}/history/recent - Should return recent history")
        void getRecentPriceHistory_shouldReturnRecentHistory() throws Exception {
            // Given
            PriceHistoryResponse history = PriceHistoryResponse.builder()
                    .date(LocalDate.now().minusDays(1))
                    .close(new BigDecimal("280.50"))
                    .build();

            when(marketDataService.getRecentPriceHistory("THYAO", 30))
                    .thenReturn(Arrays.asList(history));

            // When/Then
            mockMvc.perform(get("/api/v1/market/stocks/THYAO/history/recent")
                            .param("days", "30")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("Other Instrument Types")
    class OtherInstrumentTypes {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/v1/market/bonds - Should return bonds")
        void getBonds_shouldReturnBonds() throws Exception {
            // Given
            InstrumentResponse bond = InstrumentResponse.builder()
                    .id(1L)
                    .symbol("GOVT-2Y")
                    .name("2 Yıllık DİBS")
                    .type(Instrument.InstrumentType.BOND)
                    .build();

            Page<InstrumentResponse> page = new PageImpl<>(
                    Arrays.asList(bond),
                    PageRequest.of(0, 20),
                    1
            );
            when(marketDataService.getInstrumentsByType(eq(Instrument.InstrumentType.BOND), any()))
                    .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/market/bonds")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].type").value("BOND"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/v1/market/funds - Should return funds")
        void getFunds_shouldReturnFunds() throws Exception {
            // Given
            InstrumentResponse fund = InstrumentResponse.builder()
                    .id(1L)
                    .symbol("TRF001")
                    .name("Türk Fonu A.Ş.")
                    .type(Instrument.InstrumentType.FUND)
                    .build();

            Page<InstrumentResponse> page = new PageImpl<>(
                    Arrays.asList(fund),
                    PageRequest.of(0, 20),
                    1
            );
            when(marketDataService.getInstrumentsByType(eq(Instrument.InstrumentType.FUND), any()))
                    .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/market/funds")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].type").value("FUND"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/v1/market/viop - Should return VIOP contracts")
        void getViop_shouldReturnViop() throws Exception {
            // Given
            InstrumentResponse viop = InstrumentResponse.builder()
                    .id(1L)
                    .symbol("F_XU030")
                    .name("BIST 30 Vadeli")
                    .type(Instrument.InstrumentType.FUTURES)
                    .build();

            Page<InstrumentResponse> page = new PageImpl<>(
                    Arrays.asList(viop),
                    PageRequest.of(0, 20),
                    1
            );
            when(marketDataService.getInstrumentsByType(eq(Instrument.InstrumentType.FUTURES), any()))
                    .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/market/viop")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].type").value("FUTURES"));
        }
    }
}
