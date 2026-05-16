package com.mintstack.finance.service.event;

import com.mintstack.finance.config.KafkaConfig;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Environment environment;

    public EventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            Environment environment) {
        this.kafkaTemplate = kafkaTemplate;
        this.environment = environment;
    }

    @Observed(name = "kafka.publish.log", contextualName = "publish-log-event")
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

    @Observed(name = "kafka.publish.market-data", contextualName = "publish-market-data-event")
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

    @Observed(name = "kafka.publish.notification", contextualName = "publish-notification-event")
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
        if (!isMessagingEnabled() || kafkaTemplate == null) {
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

    private boolean isMessagingEnabled() {
        String value = environment.getProperty("app.messaging.enabled");
        if (!StringUtils.hasText(value)) {
            value = environment.getProperty("APP_MESSAGING_ENABLED");
        }
        return !StringUtils.hasText(value) || Boolean.parseBoolean(value);
    }
}
