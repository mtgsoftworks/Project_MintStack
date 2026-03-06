package com.mintstack.finance.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting configuration using Bucket4j.
 * Provides IP-based and user-based rate limiting.
 */
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitConfig {

    private static final Duration BUCKET_TTL = Duration.ofHours(6);
    private static final int MAX_BUCKETS_PER_MAP = 10_000;
    private static final long CLEANUP_INTERVAL_REQUESTS = 500L;

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

    @Getter(AccessLevel.NONE)
    private final Map<String, BucketEntry> ipBuckets = new ConcurrentHashMap<>();
    @Getter(AccessLevel.NONE)
    private final Map<String, BucketEntry> userBuckets = new ConcurrentHashMap<>();
    @Getter(AccessLevel.NONE)
    private final AtomicLong accessCounter = new AtomicLong();

    /**
     * Get or create a bucket for anonymous user (by IP address)
     */
    public Bucket resolveAnonymousBucket(String ip) {
        return resolveBucket(ipBuckets, ip, anonymousRequestsPerMinute);
    }

    /**
     * Get or create a bucket for authenticated user
     */
    public Bucket resolveUserBucket(String userId) {
        return resolveBucket(userBuckets, userId, authenticatedRequestsPerMinute);
    }

    /**
     * Get or create a bucket for admin user
     */
    public Bucket resolveAdminBucket(String userId) {
        return resolveBucket(userBuckets, "admin_" + userId, adminRequestsPerMinute);
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

    /**
     * Update runtime rate limit settings.
     */
    public synchronized void updateSettings(
        Boolean enabledValue,
        Integer anonymousPerMinute,
        Integer authenticatedPerMinute,
        Integer adminPerMinute,
        boolean clearExistingBuckets
    ) {
        if (enabledValue != null) {
            this.enabled = enabledValue;
        }
        if (anonymousPerMinute != null) {
            validatePositive(anonymousPerMinute, "anonymousRequestsPerMinute");
            this.anonymousRequestsPerMinute = anonymousPerMinute;
        }
        if (authenticatedPerMinute != null) {
            validatePositive(authenticatedPerMinute, "authenticatedRequestsPerMinute");
            this.authenticatedRequestsPerMinute = authenticatedPerMinute;
        }
        if (adminPerMinute != null) {
            validatePositive(adminPerMinute, "adminRequestsPerMinute");
            this.adminRequestsPerMinute = adminPerMinute;
        }

        if (clearExistingBuckets) {
            clearBuckets();
        }
    }

    private Bucket resolveBucket(Map<String, BucketEntry> bucketMap, String key, int requestsPerMinute) {
        long now = System.currentTimeMillis();
        BucketEntry entry = bucketMap.compute(key, (k, existing) -> {
            if (existing == null) {
                return new BucketEntry(createBucket(requestsPerMinute), now);
            }
            return new BucketEntry(existing.bucket(), now);
        });

        cleanupIfNeeded(bucketMap, now);
        return entry.bucket();
    }

    private void cleanupIfNeeded(Map<String, BucketEntry> bucketMap, long now) {
        long currentAccessCount = accessCounter.incrementAndGet();
        if (currentAccessCount % CLEANUP_INTERVAL_REQUESTS != 0
            && bucketMap.size() < MAX_BUCKETS_PER_MAP) {
            return;
        }

        long expiryCutoff = now - BUCKET_TTL.toMillis();
        bucketMap.entrySet().removeIf(entry -> entry.getValue().lastAccessEpochMillis() < expiryCutoff);

        if (bucketMap.size() <= MAX_BUCKETS_PER_MAP) {
            return;
        }

        int overflow = bucketMap.size() - MAX_BUCKETS_PER_MAP;
        bucketMap.entrySet().stream()
            .sorted(Comparator.comparingLong(entry -> entry.getValue().lastAccessEpochMillis()))
            .limit(overflow)
            .map(Map.Entry::getKey)
            .toList()
            .forEach(bucketMap::remove);
    }

    /**
     * Create a new bucket with specified requests per minute
     */
    private Bucket createBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.builder()
            .capacity(requestsPerMinute)
            .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    private void validatePositive(int value, String fieldName) {
        if (value < 1) {
            throw new IllegalArgumentException(fieldName + " en az 1 olmalidir");
        }
    }

    private record BucketEntry(Bucket bucket, long lastAccessEpochMillis) {
    }
}
