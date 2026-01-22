package com.mintstack.finance.service.simulation;

import com.mintstack.finance.entity.SimulationConfig;
import com.mintstack.finance.entity.SimulationConfig.MarketTrend;
import com.mintstack.finance.entity.SimulationConfig.VolatilityLevel;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.repository.SimulationConfigRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.service.PriceUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimulationDataService Tests")
class SimulationDataServiceTest {

    @Mock
    private SimulationConfigRepository configRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private UserApiConfigRepository userApiConfigRepository;

    @Mock
    private PriceUpdateService priceUpdateService;

    private PriceSimulationEngine priceEngine;
    private SimulationDataService simulationDataService;

    @BeforeEach
    void setUp() {
        priceEngine = new PriceSimulationEngine();
        simulationDataService = new SimulationDataService(
                configRepository,
                instrumentRepository,
                currencyRateRepository,
                priceHistoryRepository,
                userApiConfigRepository,
                priceUpdateService,
                priceEngine
        );
        // Market data'yı initialize et
        simulationDataService.initializeMarketData();
    }

    @Test
    @DisplayName("getStocks tüm hisseleri döndürmeli")
    void testGetStocks_ReturnsAllStocks() {
        // When
        Map<String, SimulationDataService.SimulatedStock> stocks = simulationDataService.getStocks();

        // Then
        assertThat(stocks).isNotEmpty();
        assertThat(stocks).containsKey("THYAO");
        assertThat(stocks).containsKey("GARAN");
        assertThat(stocks).containsKey("AKBNK");
        assertThat(stocks.size()).isGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("getCurrencies tüm dövizleri döndürmeli")
    void testGetCurrencies_ReturnsAllCurrencies() {
        // When
        Map<String, SimulationDataService.SimulatedCurrency> currencies = simulationDataService.getCurrencies();

        // Then
        assertThat(currencies).isNotEmpty();
        assertThat(currencies).containsKey("USD");
        assertThat(currencies).containsKey("EUR");
        assertThat(currencies).containsKey("GBP");
        assertThat(currencies).containsKey("XAU"); // Altın
    }

    @Test
    @DisplayName("getIndices tüm endeksleri döndürmeli")
    void testGetIndices_ReturnsAllIndices() {
        // When
        Map<String, SimulationDataService.SimulatedIndex> indices = simulationDataService.getIndices();

        // Then
        assertThat(indices).isNotEmpty();
        assertThat(indices).containsKey("XU100");
        assertThat(indices).containsKey("XU030");
    }

    @Test
    @DisplayName("getStock tek hisse döndürmeli")
    void testGetStock_ReturnsSingleStock() {
        // When
        SimulationDataService.SimulatedStock stock = simulationDataService.getStock("THYAO");

        // Then
        assertThat(stock).isNotNull();
        assertThat(stock.getName()).isEqualTo("Türk Hava Yolları");
        assertThat(stock.getExchange()).isEqualTo("BIST");
        assertThat(stock.getCurrentPrice()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getCurrency tek döviz döndürmeli")
    void testGetCurrency_ReturnsSingleCurrency() {
        // When
        SimulationDataService.SimulatedCurrency currency = simulationDataService.getCurrency("USD");

        // Then
        assertThat(currency).isNotNull();
        assertThat(currency.getName()).isEqualTo("ABD Doları");
        assertThat(currency.getBuyingRate()).isGreaterThan(BigDecimal.ZERO);
        assertThat(currency.getSellingRate()).isGreaterThan(currency.getBuyingRate());
    }

    @Test
    @DisplayName("isSimulationEnabled doğru durum döndürmeli")
    void testIsSimulationEnabled_ReturnsCorrectState() {
        // Given
        SimulationConfig config = SimulationConfig.builder()
                .isEnabled(true)
                .build();
        when(configRepository.getOrCreateDefault()).thenReturn(config);

        // When
        boolean enabled = simulationDataService.isSimulationEnabled();

        // Then
        assertThat(enabled).isTrue();
    }

    @Test
    @DisplayName("getConfig yapılandırma döndürmeli")
    void testGetConfig_ReturnsConfig() {
        // Given
        SimulationConfig config = SimulationConfig.builder()
                .isEnabled(true)
                .volatilityLevel(VolatilityLevel.HIGH)
                .marketTrend(MarketTrend.BULLISH)
                .updateIntervalSeconds(10)
                .enableRandomEvents(true)
                .build();
        when(configRepository.getOrCreateDefault()).thenReturn(config);

        // When
        SimulationConfig result = simulationDataService.getConfig();

        // Then
        assertThat(result.getIsEnabled()).isTrue();
        assertThat(result.getVolatilityLevel()).isEqualTo(VolatilityLevel.HIGH);
        assertThat(result.getMarketTrend()).isEqualTo(MarketTrend.BULLISH);
    }

    @Test
    @DisplayName("updateConfig tüm alanları güncellemeli")
    void testUpdateConfig_UpdatesAllFields() {
        // Given
        SimulationConfig existingConfig = SimulationConfig.builder()
                .isEnabled(false)
                .volatilityLevel(VolatilityLevel.MEDIUM)
                .marketTrend(MarketTrend.NEUTRAL)
                .updateIntervalSeconds(5)
                .enableRandomEvents(false)
                .build();
        existingConfig.setId(UUID.randomUUID());
        
        when(configRepository.getOrCreateDefault()).thenReturn(existingConfig);
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        SimulationConfig result = simulationDataService.updateConfig(
                true, VolatilityLevel.HIGH, 15, MarketTrend.BULLISH, true);

        // Then
        assertThat(result.getIsEnabled()).isTrue();
        assertThat(result.getVolatilityLevel()).isEqualTo(VolatilityLevel.HIGH);
        assertThat(result.getMarketTrend()).isEqualTo(MarketTrend.BULLISH);
        assertThat(result.getUpdateIntervalSeconds()).isEqualTo(15);
        assertThat(result.getEnableRandomEvents()).isTrue();
    }

    @Test
    @DisplayName("updateConfig null değerleri atlamalı")
    void testUpdateConfig_SkipsNullValues() {
        // Given
        SimulationConfig existingConfig = SimulationConfig.builder()
                .isEnabled(false)
                .volatilityLevel(VolatilityLevel.MEDIUM)
                .marketTrend(MarketTrend.NEUTRAL)
                .updateIntervalSeconds(5)
                .enableRandomEvents(false)
                .build();
        
        when(configRepository.getOrCreateDefault()).thenReturn(existingConfig);
        when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When - Sadece enabled güncelle
        SimulationConfig result = simulationDataService.updateConfig(
                true, null, null, null, null);

        // Then
        assertThat(result.getIsEnabled()).isTrue();
        assertThat(result.getVolatilityLevel()).isEqualTo(VolatilityLevel.MEDIUM); // Değişmemeli
        assertThat(result.getMarketTrend()).isEqualTo(MarketTrend.NEUTRAL); // Değişmemeli
    }

    @Test
    @DisplayName("resetSimulation verileri sıfırlamalı")
    void testResetSimulation_ClearsAllState() {
        // Given - Önce bazı fiyatları değiştir
        SimulationDataService.SimulatedStock stock = simulationDataService.getStock("THYAO");
        BigDecimal originalPrice = stock.getCurrentPrice();

        // When
        simulationDataService.resetSimulation();
        SimulationDataService.SimulatedStock resetStock = simulationDataService.getStock("THYAO");

        // Then - Hisse hala mevcut olmalı
        assertThat(resetStock).isNotNull();
        assertThat(resetStock.getCurrentPrice()).isNotNull();
    }

    @Test
    @DisplayName("SimulatedStock changePercent doğru hesaplamalı")
    void testSimulatedStock_CalculatesChangePercent() {
        // Given
        SimulationDataService.SimulatedStock stock = new SimulationDataService.SimulatedStock(
                "Test Stock", "BIST", 100.0, 0.02);

        // When
        stock.updatePrice(BigDecimal.valueOf(105.0)); // %5 artış

        // Then
        BigDecimal changePercent = stock.getChangePercent();
        assertThat(changePercent.doubleValue()).isBetween(4.9, 5.1);
    }

    @Test
    @DisplayName("SimulatedStock OHLC güncellenmeli")
    void testSimulatedStock_UpdatesOHLC() {
        // Given
        SimulationDataService.SimulatedStock stock = new SimulationDataService.SimulatedStock(
                "Test Stock", "BIST", 100.0, 0.02);

        // When
        stock.updatePrice(BigDecimal.valueOf(105.0)); // Yüksek
        stock.updatePrice(BigDecimal.valueOf(95.0));  // Düşük
        stock.updatePrice(BigDecimal.valueOf(102.0)); // Final

        // Then
        assertThat(stock.getHighPrice().doubleValue()).isEqualTo(105.0);
        assertThat(stock.getLowPrice().doubleValue()).isEqualTo(95.0);
        assertThat(stock.getCurrentPrice().doubleValue()).isEqualTo(102.0);
    }

    @Test
    @DisplayName("SimulatedCurrency midRate hesaplamalı")
    void testSimulatedCurrency_CalculatesMidRate() {
        // Given
        SimulationDataService.SimulatedCurrency currency = new SimulationDataService.SimulatedCurrency(
                "Test Currency", 38.0, 38.5, 0.01);

        // When
        BigDecimal midRate = currency.getMidRate();

        // Then
        assertThat(midRate.doubleValue()).isEqualTo(38.25);
    }
}
