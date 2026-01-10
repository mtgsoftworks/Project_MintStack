package com.mintstack.finance.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mintstack.finance.config.RateLimitConfig;
import com.mintstack.finance.dto.response.ApiResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
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
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter using Bucket4j.
 * Applies different rate limits based on user type (anonymous, authenticated, admin).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

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

        // Get the appropriate bucket based on authentication status
        Bucket bucket = resolveBucket(request);
        
        // Try to consume a token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers to response
            addRateLimitHeaders(response, probe);
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitForRefillSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            log.warn("Rate limit exceeded for {} from IP: {}. Retry after: {} seconds",
                    request.getRequestURI(), getClientIP(request), waitForRefillSeconds);
            
            sendRateLimitResponse(response, waitForRefillSeconds);
        }
    }

    /**
     * Resolve the appropriate bucket based on authentication status
     */
    private Bucket resolveBucket(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            
            // Check if user is admin
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
            
            String userId = getUserId(authentication);
            
            if (isAdmin) {
                return rateLimitConfig.resolveAdminBucket(userId);
            } else {
                return rateLimitConfig.resolveUserBucket(userId);
            }
        }
        
        // Anonymous user - use IP-based rate limiting
        return rateLimitConfig.resolveAnonymousBucket(getClientIP(request));
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
     * Get client IP address, considering proxy headers
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Check if the path should skip rate limiting
     */
    private boolean shouldSkipRateLimit(String path) {
        return path.startsWith("/actuator/") 
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.equals("/ws")
                || path.startsWith("/ws/");
    }

    /**
     * Add rate limit information headers to response
     */
    private void addRateLimitHeaders(HttpServletResponse response, ConsumptionProbe probe) {
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        response.setHeader("X-RateLimit-Reset", 
                String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));
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
        
        ApiResponse<?> errorResponse = ApiResponse.error(
                "Rate limit exceeded. Please try again in " + retryAfterSeconds + " seconds.",
                "RATE_LIMIT_EXCEEDED"
        );
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
