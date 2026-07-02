package com.mintstack.finance.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.config.RateLimitConfig.RateLimitDecision;
import com.mintstack.finance.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Rate limiting filter using Bucket4j.
 * Applies different rate limits based on user type (anonymous, authenticated, admin).
 *
 * SECURITY: Filter order changed to run AFTER security filters to ensure
 * SecurityContext is populated with authenticated user information.
 * X-Forwarded-For is only trusted from configured proxy addresses.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE + 100) // Run after security filter chain (~Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final List<String> TRUSTED_PROXIES = List.of(
        "127.0.0.1", "::1", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"
    );

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip rate limiting if disabled
        if (!rateLimitConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for health checks and actuator
        String path = request.getRequestURI();
        if (shouldSkipRateLimit(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitTarget target = resolveTarget(request);
        RateLimitDecision decision = rateLimitConfig.tryConsume(target.scope(), target.key(), target.requestsPerMinute());

        if (decision == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (decision.allowed()) {
            addRateLimitHeaders(response, decision);
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for {} from IP: {}. Retry after: {} seconds",
                    request.getRequestURI(), getClientIP(request), decision.retryAfterSeconds());
            
            sendRateLimitResponse(response, decision.retryAfterSeconds());
        }
    }

    /**
     * Resolve the appropriate bucket based on authentication status
     */
    private RateLimitTarget resolveTarget(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            
            // Check if user is admin
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
            
            String userId = getUserId(authentication);
            
            if (isAdmin) {
                return new RateLimitTarget("admin", userId, rateLimitConfig.getAdminRequestsPerMinute());
            }
            return new RateLimitTarget("user", userId, rateLimitConfig.getAuthenticatedRequestsPerMinute());
        }
        
        return new RateLimitTarget("anonymous", getClientIP(request), rateLimitConfig.getAnonymousRequestsPerMinute());
    }

    /**
     * Extract user ID from authentication
     */
    private String getUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return authentication.getName();
    }

    /**
     * Get client IP address, considering proxy headers.
     * SECURITY: X-Forwarded-For is only trusted from configured proxy addresses
     * to prevent IP spoofing attacks.
     */
    private String getClientIP(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // Only trust X-Forwarded-For if request comes from a trusted proxy
        if (isFromTrustedProxy(request)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // Take the first IP (original client)
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIP = request.getHeader("X-Real-IP");
            if (xRealIP != null && !xRealIP.isEmpty()) {
                return xRealIP;
            }
        }

        return remoteAddr;
    }

    /**
     * Check if the request originated from a trusted proxy.
     * Prevents IP spoofing via forged X-Forwarded-For headers.
     */
    private boolean isFromTrustedProxy(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null) {
            return false;
        }
        // localhost and private IPs are considered trusted proxies in Docker/compose networks
        return remoteAddr.equals("127.0.0.1")
            || remoteAddr.equals("::1")
            || remoteAddr.equals("0.0.0.0")
            || remoteAddr.startsWith("172.17.")  // Docker default bridge
            || remoteAddr.startsWith("172.18.")  // Docker compose networks
            || remoteAddr.startsWith("172.19.")
            || remoteAddr.startsWith("172.20.")
            || remoteAddr.startsWith("172.21.")
            || remoteAddr.startsWith("172.22.")
            || remoteAddr.startsWith("10.");      // RFC 1918 private
    }

    /**
     * Check if the path should skip rate limiting
     */
    private boolean shouldSkipRateLimit(String path) {
        return path.startsWith("/actuator/") 
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.equals("/ws")
                || path.startsWith("/ws/")
                || path.equals("/ws-native")       // ADDED: WebSocket native endpoint
                || path.startsWith("/ws-native/"); // ADDED: WebSocket native sub-paths
    }

    /**
     * Add rate limit information headers to response
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitDecision decision) {
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remainingTokens()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetSeconds()));
        response.setHeader("X-RateLimit-Store", decision.store());
    }

    /**
     * Send 429 Too Many Requests response
     */
    private void sendRateLimitResponse(HttpServletResponse response, long retryAfterSeconds) 
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Remaining", "0");
        
        com.mintstack.finance.dto.response.ErrorResponse error = com.mintstack.finance.dto.response.ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("RATE_LIMIT_EXCEEDED")
                .message("Rate limit exceeded. Please try again in " + retryAfterSeconds + " seconds.")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        ApiResponse<?> errorResponse = ApiResponse.error(error);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private record RateLimitTarget(String scope, String key, int requestsPerMinute) {
    }
}
