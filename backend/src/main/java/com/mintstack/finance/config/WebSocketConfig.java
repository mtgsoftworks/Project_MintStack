package com.mintstack.finance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time price updates.
 * Uses STOMP protocol with SockJS fallback for browser compatibility.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure the message broker.
     * /topic - for broadcasting messages to all subscribers
     * /queue - for point-to-point messaging
     * /app - prefix for messages handled by @MessageMapping methods
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple broker for broadcasting messages
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for application destination mappings
        config.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix for private messages
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints with SockJS fallback.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25000);
        
        // Pure WebSocket endpoint (without SockJS)
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }
}
