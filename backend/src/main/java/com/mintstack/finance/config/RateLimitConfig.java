package com.mintstack.finance.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
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
@Slf4j
public class RateLimitConfig {

    private static final Duration BUCKET_TTL = Duration.ofHours(6);
    private static final int MAX_BUCKETS_PER_MAP = 10_000;
    private static final long CLEANUP_INTERVAL_REQUESTS = 500L;
    private static final String REDIS_KEY_PREFIX = "rate-limit:";
    private static final String REDIS_TOKEN_BUCKET_SCRIPT = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local now = tonumber(ARGV[2])
        local window = 60000
        local values = redis.call('HMGET', key, 'tokens', 'updated')
        local tokens = tonumber(values[1])
        local updated = tonumber(values[2])
        if tokens == nil then
            tokens = limit
            updated = now
        end
        local delta = math.max(0, now - updated)
        local refill = (delta / window) * limit
        tokens = math.min(limit, tokens + refill)
        local allowed = 0
        if tokens >= 1 then
            allowed = 1
            tokens = tokens - 1
        end
        redis.call('HMSET', key, 'tokens', tokens, 'updated', now)
        redis.call('PEXPIRE', key, 21600000)
        local retry = 0
        if allowed == 0 then
            retry = math.ceil(((1 - tokens) * window) / limit / 1000)
        end
        return {allowed, math.floor(tokens), 60, retry}
        """;

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
     * Store used by the request path. "redis" is distributed, "memory" is local.
     */
    private String store = "redis";

    @Autowired(required = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private StringRedisTemplate stringRedisTemplate;

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

    public RateLimitDecision tryConsume(String scope, String key, int requestsPerMinute) {
        if (!enabled) {
            return new RateLimitDecision(true, requestsPerMinute, 0, 0, currentStoreName());
        }
        validatePositive(requestsPerMinute, "requestsPerMinute");

        if ("redis".equalsIgnoreCase(store) && stringRedisTemplate != null) {
            try {
                return consumeWithRedis(scope, key, requestsPerMinute);
            } catch (RuntimeException error) {
                log.warn("Redis rate limit failed, falling back to memory bucket: {}", error.getMessage());
            }
        }
        return consumeWithMemory(scope, key, requestsPerMinute);
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

    private RateLimitDecision consumeWithRedis(String scope, String key, int requestsPerMinute) {
        String redisKey = REDIS_KEY_PREFIX + sanitize(scope) + ":" + sanitize(key);
        DefaultRedisScript<List> script = new DefaultRedisScript<>(REDIS_TOKEN_BUCKET_SCRIPT, List.class);
        List<?> result = stringRedisTemplate.execute(
            script,
            List.of(redisKey),
            String.valueOf(requestsPerMinute),
            String.valueOf(System.currentTimeMillis())
        );
        if (result == null || result.size() < 4) {
            throw new IllegalStateException("Redis script returned an invalid response");
        }

        boolean allowed = toLong(result.get(0)) == 1L;
        long remaining = Math.max(0L, toLong(result.get(1)));
        long resetSeconds = Math.max(0L, toLong(result.get(2)));
        long retryAfterSeconds = Math.max(0L, toLong(result.get(3)));
        return new RateLimitDecision(allowed, remaining, resetSeconds, retryAfterSeconds, "redis");
    }

    private RateLimitDecision consumeWithMemory(String scope, String key, int requestsPerMinute) {
        Map<String, BucketEntry> bucketMap = "anonymous".equals(scope) ? ipBuckets : userBuckets;
        Bucket bucket = resolveBucket(bucketMap, scope + ":" + key, requestsPerMinute);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        long retryAfterSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds();
        return new RateLimitDecision(
            probe.isConsumed(),
            probe.getRemainingTokens(),
            retryAfterSeconds,
            retryAfterSeconds,
            "memory"
        );
    }

    private String currentStoreName() {
        return "redis".equalsIgnoreCase(store) && stringRedisTemplate != null ? "redis" : "memory";
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private record BucketEntry(Bucket bucket, long lastAccessEpochMillis) {
    }

    public record RateLimitDecision(
        boolean allowed,
        long remainingTokens,
        long resetSeconds,
        long retryAfterSeconds,
        String store
    ) {
    }
}
