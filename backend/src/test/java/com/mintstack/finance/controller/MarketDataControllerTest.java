package com.mintstack.finance.controller;

import com.mintstack.finance.config.CorsProperties;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.config.SecurityConfig;
import com.mintstack.finance.dto.response.CurrencyRateResponse;
import com.mintstack.finance.dto.response.InstrumentResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

        when(marketDataService.getLatestCurrencyRates()).thenReturn(List.of(usdRate));

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
    void getCrypto_ShouldReturnCryptoList() throws Exception {
        InstrumentResponse btc = InstrumentResponse.builder()
            .id(UUID.randomUUID())
            .symbol("BTC-USD")
            .name("Bitcoin")
            .type(Instrument.InstrumentType.CRYPTO)
            .currentPrice(BigDecimal.valueOf(100000))
            .build();

        when(marketDataService.getInstrumentsByType(eq(Instrument.InstrumentType.CRYPTO), any()))
            .thenReturn(new PageImpl<>(List.of(btc), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/market/crypto"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].symbol").value("BTC-USD"));
    }
}
