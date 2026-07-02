package com.mintstack.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClusterWebSocketPublisher {

    public static final String CHANNEL = "mintstack:websocket:broadcast";

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Value("${app.redis.cache.enabled:true}")
    private boolean redisEnabled;

    public void broadcast(String destination, Object payload) {
        publishOrSendLocally(new BroadcastEnvelope(destination, null, payload));
    }

    public void broadcastToUser(String userId, String destination, Object payload) {
        publishOrSendLocally(new BroadcastEnvelope(destination, userId, payload));
    }

    public void receive(String serializedEnvelope) {
        try {
            JsonNode root = objectMapper.readTree(serializedEnvelope);
            String destination = requiredText(root, "destination");
            JsonNode payload = root.get("payload");
            JsonNode userId = root.get("userId");

            if (userId != null && !userId.isNull() && !userId.asText().isBlank()) {
                messagingTemplate.convertAndSendToUser(userId.asText(), destination, payload);
            } else {
                messagingTemplate.convertAndSend(destination, payload);
            }
        } catch (Exception error) {
            log.error("Rejected invalid WebSocket broadcast envelope: {}", error.getMessage());
        }
    }

    private void publishOrSendLocally(BroadcastEnvelope envelope) {
        if (redisEnabled) {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                try {
                    String serialized = objectMapper.writeValueAsString(envelope);
                    Long subscribers = redisTemplate.convertAndSend(CHANNEL, serialized);
                    if (subscribers != null && subscribers > 0) {
                        return;
                    }
                    log.warn("No Redis WebSocket subscribers available; using local delivery");
                } catch (Exception error) {
                    log.warn("Redis WebSocket broadcast failed; using local delivery: {}", error.getMessage());
                }
            }
        }
        sendLocally(envelope);
    }

    private void sendLocally(BroadcastEnvelope envelope) {
        if (envelope.userId() == null) {
            messagingTemplate.convertAndSend(envelope.destination(), envelope.payload());
        } else {
            messagingTemplate.convertAndSendToUser(
                    envelope.userId(),
                    envelope.destination(),
                    envelope.payload()
            );
        }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing broadcast field: " + field);
        }
        return value.asText();
    }

    private record BroadcastEnvelope(String destination, String userId, Object payload) {
    }
}
