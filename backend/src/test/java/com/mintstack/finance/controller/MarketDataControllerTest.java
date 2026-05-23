package com.mintstack.finance.controller;

import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.config.SecurityConfig;
import com.mintstack.finance.dto.response.CurrencyRateResponse;
import com.mintstack.finance.service.MarketDataRefreshService;
import com.mintstack.finance.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketDataController.class)
@Import({CorsProperties.class, SecurityConfig.class})

class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketDataService marketDataService;

    @MockitoBean
    private MarketDataRefreshService marketDataRefreshService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void getCurrencyRates_ShouldReturnRates() throws Exception {
        // Given
        CurrencyRateResponse usdRate = CurrencyRateResponse.builder()
            .id(UUID.randomUUID())
            .currencyCode("USD")
            .currencyName("US DOLLAR")
            .buyingRate(BigDecimal.valueOf(32.50))
            .sellingRate(BigDecimal.valueOf(32.70))
            .source("TCMB")
            .fetchedAt(LocalDateTime.now())
            .rateDate(LocalDateTime.now())
            .build();

        when(marketDataService.getLatestCurrencyRates(isNull(LocalDate.class), isNull(LocalDate.class)))
            .thenReturn(List.of(usdRate));

        // When & Then
        mockMvc.perform(get("/api/v1/market/currencies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].currencyCode").value("USD"));
    }

    @Test
    void getCurrencyByCode_ShouldReturnRate() throws Exception {
        // Given
        CurrencyRateResponse usdRate = CurrencyRateResponse.builder()
            .id(UUID.randomUUID())
            .currencyCode("USD")
            .currencyName("US DOLLAR")
            .buyingRate(BigDecimal.valueOf(32.50))
            .sellingRate(BigDecimal.valueOf(32.70))
            .source("TCMB")
            .fetchedAt(LocalDateTime.now())
            .rateDate(LocalDateTime.now())
            .build();

        when(marketDataService.getCurrencyRate("USD")).thenReturn(usdRate);

        // When & Then
        mockMvc.perform(get("/api/v1/market/currencies/USD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.currencyCode").value("USD"));
    }

    @Test
    void getCrypto_ShouldReturnNotFound_WhenCryptoSupportIsRemoved() throws Exception {
        mockMvc.perform(get("/api/v1/market/crypto"))
            .andExpect(status().isNotFound());
    }
}
