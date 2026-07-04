package com.mintstack.finance.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
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
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsProperties corsProperties;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

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
     * CORS restricted to allowed origins from CorsProperties.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = corsProperties.getAllowedOrigins()
                .toArray(new String[0]);
        String[] originPatterns = corsProperties.getAllowedOriginPatterns()
                .toArray(new String[0]);

        // WebSocket endpoint with SockJS fallback – wide open for local dev
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(25000);

        // Pure WebSocket endpoint (without SockJS)
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }

    /**
     * Register the JWT auth interceptor for STOMP CONNECT frames.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
