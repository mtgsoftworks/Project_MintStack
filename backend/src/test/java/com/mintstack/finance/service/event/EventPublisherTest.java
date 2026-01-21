package com.mintstack.finance.service.event;

import com.mintstack.finance.config.KafkaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new EventPublisher(kafkaTemplate);
    }

    @Test
    void publishLogEvent_ShouldSendToLogsTopic() {
        // Given
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(KafkaConfig.TOPIC_LOGS), any(), any())).thenReturn(future);

        // When
        eventPublisher.publishLogEvent("INFO", "Test message", "TestLogger", Map.of("key", "value"));

        // Then
        ArgumentCaptor<LogEvent> captor = ArgumentCaptor.forClass(LogEvent.class);
        verify(kafkaTemplate).send(eq(KafkaConfig.TOPIC_LOGS), any(), captor.capture());
        
        LogEvent event = captor.getValue();
        assertThat(event.getLevel()).isEqualTo("INFO");
        assertThat(event.getMessage()).isEqualTo("Test message");
        assertThat(event.getLogger()).isEqualTo("TestLogger");
        assertThat(event.getService()).isEqualTo("finance-portal");
    }

    @Test
    void publishMarketDataEvent_ShouldSendToMarketDataTopic() {
        // Given
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(KafkaConfig.TOPIC_MARKET_DATA), any(), any())).thenReturn(future);

        // When
        eventPublisher.publishMarketDataEvent("THYAO", "STOCK_PRICE", Map.of("price", 100.0));

        // Then
        ArgumentCaptor<MarketDataEvent> captor = ArgumentCaptor.forClass(MarketDataEvent.class);
        verify(kafkaTemplate).send(eq(KafkaConfig.TOPIC_MARKET_DATA), eq("THYAO"), captor.capture());
        
        MarketDataEvent event = captor.getValue();
        assertThat(event.getSymbol()).isEqualTo("THYAO");
        assertThat(event.getType()).isEqualTo("STOCK_PRICE");
    }

    @Test
    void publishNotificationEvent_ShouldSendToNotificationsTopic() {
        // Given
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(KafkaConfig.TOPIC_NOTIFICATIONS), any(), any())).thenReturn(future);

        // When
        eventPublisher.publishNotificationEvent(
            "user-123",
            "PRICE_ALERT",
            "Alert Title",
            "Alert Message",
            Map.of("symbol", "GARAN")
        );

        // Then
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(kafkaTemplate).send(eq(KafkaConfig.TOPIC_NOTIFICATIONS), eq("user-123"), captor.capture());
        
        NotificationEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo("user-123");
        assertThat(event.getType()).isEqualTo("PRICE_ALERT");
        assertThat(event.getTitle()).isEqualTo("Alert Title");
        assertThat(event.getMessage()).isEqualTo("Alert Message");
    }
}
