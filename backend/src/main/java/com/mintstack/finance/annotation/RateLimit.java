package com.mintstack.finance.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for rate limiting method invocations.
 * Can be applied to controller methods or service methods.
 * Uses Bucket4j for token bucket rate limiting.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of requests allowed per minute.
     * Default is 100 requests per minute.
     */
    int requestsPerMinute() default 100;

    /**
     * Key type for rate limiting bucket.
     * IP: Uses client IP address as bucket key.
     * USER: Uses authenticated user ID as bucket key.
     */
    KeyType keyType() default KeyType.IP;

    /**
     * Message to return when rate limit is exceeded.
     */
    String message() default "Rate limit exceeded. Please try again later.";

    enum KeyType {
        IP,
        USER
    }
}
