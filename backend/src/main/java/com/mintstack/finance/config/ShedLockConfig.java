package com.mintstack.finance.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * ShedLock configuration for distributed scheduler locking.
 * Prevents duplicate job execution when running multiple backend replicas.
 *
 * Uses Redis as the lock provider - locks are automatically released
 * after the configured lock-at-most-for duration.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    /**
     * Configure ShedLock to use Redis for distributed locking.
     * Each scheduled task should use @SchedulerLock annotation.
     */
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "mintstack");
    }
}
