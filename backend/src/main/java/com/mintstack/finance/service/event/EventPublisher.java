package com.mintstack.finance.service.event;

import com.mintstack.finance.config.KafkaConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.messaging.enabled:true}")
    private boolean messagingEnabled = true;

    public EventPublisher(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishLogEvent(String level, String message, String logger, Map<String, Object> context) {
        LogEvent event = LogEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .level(level)
                .message(message)
                .logger(logger)
                .context(context)
                .service("finance-portal")
                .build();

        send(KafkaConfig.TOPIC_LOGS, event.getId(), event);
    }

    public void publishMarketDataEvent(String symbol, String type, Object data) {
        MarketDataEvent event = MarketDataEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .symbol(symbol)
                .type(type)
                .data(data)
                .build();

        send(KafkaConfig.TOPIC_MARKET_DATA, symbol, event);
    }

    public void publishNotificationEvent(String userId, String type, String title, String message, Map<String, Object> metadata) {
        NotificationEvent event = NotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .metadata(metadata)
                .build();

        send(KafkaConfig.TOPIC_NOTIFICATIONS, userId, event);
    }

    private void send(String topic, String key, Object event) {
        if (!messagingEnabled || kafkaTemplate == null) {
            log.debug("Messaging disabled or Kafka template unavailable. Skipping publish to topic {}", topic);
            return;
        }

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event to topic {}: {}", topic, ex.getMessage());
            } else {
                log.debug("Event sent to topic {} with offset {}", 
                        topic, result.getRecordMetadata().offset());
            }
        });
    }
}
