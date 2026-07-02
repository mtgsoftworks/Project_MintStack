package com.mintstack.finance.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.mintstack.finance.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    private Jwt validJwt;

    @BeforeEach
    void setUp() {
        validJwt = Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .subject("user-123")
                .claim("realm_access", Map.of("roles", List.of("user")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("CONNECT with valid JWT sets user authentication")
    void preSend_ValidJwt_SetsAuthentication() {
        when(jwtDecoder.decode("valid-token")).thenReturn(validJwt);
        when(userRepository.findActiveStatusByKeycloakId("user-123"))
                .thenReturn(java.util.Optional.of(true));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer valid-token");

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("CONNECT with invalid JWT throws AccessDeniedException")
    void preSend_InvalidJwt_ThrowsAccessDeniedException() {
        when(jwtDecoder.decode(anyString())).thenThrow(new JwtException("Invalid token"));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer bad-token");

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("Client SEND messages are rejected")
    void preSend_SendCommand_IsRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("Authenticated price subscriptions are allowed")
    void preSend_AllowedSubscription_PassesThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(new UsernamePasswordAuthenticationToken("user-123", null, List.of()));
        accessor.setDestination("/topic/prices/stocks");

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Subscriptions to arbitrary destinations are rejected")
    void preSend_UnknownSubscription_IsRejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(new UsernamePasswordAuthenticationToken("user-123", null, List.of()));
        accessor.setDestination("/queue/internal");

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("CONNECT without token throws AccessDeniedException")
    void preSend_NoToken_ThrowsAccessDeniedException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
