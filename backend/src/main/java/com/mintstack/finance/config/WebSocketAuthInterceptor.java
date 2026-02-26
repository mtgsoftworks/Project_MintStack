package com.mintstack.finance.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Intercepts WebSocket CONNECT frames to validate JWT tokens.
 * Tokens can be passed via:
 *   1. Authorization header: "Bearer <token>"
 *   2. Query parameter: ?token=<token>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket connection rejected: missing JWT token");
                throw new AccessDeniedException("WebSocket authentication required");
            }

            try {
                Jwt jwt = jwtDecoder.decode(token);
                List<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, authorities);

                message = attachAuthentication(message, accessor, authentication);
                log.debug("WebSocket authenticated for user: {}", jwt.getSubject());
            } catch (Exception e) {
                log.warn("WebSocket JWT validation failed: {}", e.getMessage());
                throw new AccessDeniedException("Invalid WebSocket authentication token");
            }
        }

        return message;
    }

    private Message<?> attachAuthentication(
        Message<?> message,
        StompHeaderAccessor accessor,
        UsernamePasswordAuthenticationToken authentication
    ) {
        if (accessor.isMutable()) {
            accessor.setUser(authentication);
            return message;
        }

        // Some test/runtime flows provide immutable headers; wrap to a mutable accessor.
        StompHeaderAccessor mutableAccessor = StompHeaderAccessor.wrap(message);
        mutableAccessor.setLeaveMutable(true);
        mutableAccessor.setUser(authentication);
        return MessageBuilder.createMessage(message.getPayload(), mutableAccessor.getMessageHeaders());
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization header first
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        // Try token header
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            roles.forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            );
        }

        return authorities;
    }
}
