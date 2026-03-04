package com.mintstack.finance.service;

import com.mintstack.finance.dto.cache.SimulationConfigData;
import com.mintstack.finance.dto.cache.StockPriceData;
import com.mintstack.finance.entity.SimulationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PriceCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PriceCacheService priceCacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ReflectionTestUtils.setField(priceCacheService, "simulationStockTtlSeconds", 300L);
        ReflectionTestUtils.setField(priceCacheService, "simulationCurrencyTtlSeconds", 300L);
        ReflectionTestUtils.setField(priceCacheService, "simulationIndexTtlSeconds", 300L);
        ReflectionTestUtils.setField(priceCacheService, "simulationConfigTtlSeconds", 600L);
    }

    @Test
    void saveStockPrice_ShouldRefreshTtlForDataAndSymbolKeys() {
        // Given
        StockPriceData data = StockPriceData.builder()
                .symbol("THYAO")
                .price(new BigDecimal("145.30"))
                .build();

        // When
        priceCacheService.saveStockPrice("THYAO", data);

        // Then
        verify(hashOperations).put("simulation:stock-prices", "THYAO", data);
        verify(setOperations).add("simulation:stock-symbols", "THYAO");
        verify(redisTemplate).expire("simulation:stock-prices", Duration.ofSeconds(300));
        verify(redisTemplate).expire("simulation:stock-symbols", Duration.ofSeconds(300));
    }

    @Test
    void saveSimulationConfig_ShouldRefreshTtlForConfigKey() {
        // Given
        SimulationConfig config = SimulationConfig.builder()
                .isEnabled(true)
                .volatilityLevel(SimulationConfig.VolatilityLevel.MEDIUM)
                .updateIntervalSeconds(5)
                .marketTrend(SimulationConfig.MarketTrend.NEUTRAL)
                .enableRandomEvents(true)
                .enableMarketHours(false)
                .build();

        // When
        priceCacheService.saveSimulationConfig(config);

        // Then
        verify(valueOperations).set(eq("simulation:config"), any(SimulationConfigData.class));
        verify(redisTemplate).expire("simulation:config", Duration.ofSeconds(600));
    }
}
