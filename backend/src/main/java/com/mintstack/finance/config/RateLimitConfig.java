package com.mintstack.finance.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration using Bucket4j.
 * Provides IP-based and user-based rate limiting.
 */
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitConfig {

    /**
     * Enable/disable rate limiting globally
     */
    private boolean enabled = true;

    /**
     * Requests per minute for anonymous users (by IP)
     */
    private int anonymousRequestsPerMinute = 100;

    /**
     * Requests per minute for authenticated users
     */
    private int authenticatedRequestsPerMinute = 200;

    /**
     * Requests per minute for admin users
     */
    private int adminRequestsPerMinute = 500;

    /**
     * Cache for IP-based buckets
     */
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    /**
     * Cache for user-based buckets
     */
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    /**
     * Get or create a bucket for anonymous user (by IP address)
     */
    public Bucket resolveAnonymousBucket(String ip) {
        return ipBuckets.computeIfAbsent(ip, key -> createBucket(anonymousRequestsPerMinute));
    }

    /**
     * Get or create a bucket for authenticated user
     */
    public Bucket resolveUserBucket(String userId) {
        return userBuckets.computeIfAbsent(userId, key -> createBucket(authenticatedRequestsPerMinute));
    }

    /**
     * Get or create a bucket for admin user
     */
    public Bucket resolveAdminBucket(String userId) {
        return userBuckets.computeIfAbsent("admin_" + userId, key -> createBucket(adminRequestsPerMinute));
    }

    /**
     * Create a new bucket with specified requests per minute
     */
    private Bucket createBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(
            requestsPerMinute,
            Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Clear all cached buckets (useful for testing)
     */
    public void clearBuckets() {
        ipBuckets.clear();
        userBuckets.clear();
    }

    /**
     * Get current bucket count (for monitoring)
     */
    public int getBucketCount() {
        return ipBuckets.size() + userBuckets.size();
    }
}
