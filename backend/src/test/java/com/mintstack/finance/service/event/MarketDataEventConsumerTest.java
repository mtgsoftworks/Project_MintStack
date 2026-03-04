package com.mintstack.finance.service.event;

import com.mintstack.finance.service.search.OpenSearchService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MarketDataEventConsumerTest {

    @Mock
    private OpenSearchService openSearchService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @InjectMocks
    private MarketDataEventConsumer marketDataEventConsumer;

    @BeforeEach
    void setUp() {
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
    }

    @Test
    void consumeMarketData_ShouldIndexMarketDataDocument() {
        // Given
        Map<String, Object> payload = Map.of("price", 145.35);
        MarketDataEvent event = MarketDataEvent.builder()
                .id("evt-1")
                .symbol("THYAO")
                .type("STOCK_PRICE")
                .data(payload)
                .build();

        // When
        marketDataEventConsumer.consumeMarketData(event);

        // Then
        verify(openSearchService).indexMarketData("THYAO", "STOCK_PRICE", payload);
        verify(counter).increment();
    }

    @Test
    void consumeMarketData_ShouldIgnoreNullEvent() {
        // When
        marketDataEventConsumer.consumeMarketData(null);

        // Then
        verifyNoInteractions(openSearchService);
    }

    @Test
    void consumeMarketData_ShouldThrowWhenSymbolAndTypeMissing() {
        // Given
        Map<String, Object> payload = Map.of("buyingRate", 34.50);
        MarketDataEvent event = MarketDataEvent.builder()
                .id("evt-2")
                .symbol(" ")
                .type(null)
                .data(payload)
                .build();

        // When / Then
        assertThatThrownBy(() -> marketDataEventConsumer.consumeMarketData(event))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(openSearchService);
        verify(counter).increment();
    }

    @Test
    void consumeMarketData_ShouldRethrowWhenIndexingFails() {
        // Given
        Map<String, Object> payload = Map.of("price", 145.35);
        MarketDataEvent event = MarketDataEvent.builder()
                .id("evt-3")
                .symbol("THYAO")
                .type("STOCK_PRICE")
                .data(payload)
                .build();
        doThrow(new RuntimeException("OpenSearch unavailable"))
                .when(openSearchService).indexMarketData("THYAO", "STOCK_PRICE", payload);

        // When / Then
        assertThatThrownBy(() -> marketDataEventConsumer.consumeMarketData(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OpenSearch unavailable");

        verify(counter).increment();
    }
}
