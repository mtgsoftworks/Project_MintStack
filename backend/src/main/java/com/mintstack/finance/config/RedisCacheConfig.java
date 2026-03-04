package com.mintstack.finance.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.redis.cache.enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheConfig {

    @Value("${app.cache.currency-rates-ttl:300}")
    private long currencyRatesTtlSeconds;

    @Value("${app.cache.instruments-ttl:120}")
    private long instrumentsTtlSeconds;

    @Value("${app.cache.stock-prices-ttl:60}")
    private long stockPricesTtlSeconds;

    @Value("${app.cache.historical-data-ttl:300}")
    private long historicalDataTtlSeconds;

    @Value("${app.cache.news-ttl:600}")
    private long newsTtlSeconds;

    @Value("${app.cache.portfolio-ttl:120}")
    private long portfolioTtlSeconds;

    @Value("${app.cache.users-ttl:600}")
    private long usersTtlSeconds;

    @Value("${app.cache.index-values-ttl:300}")
    private long indexValuesTtlSeconds;

    @Value("${app.cache.simulation-config-ttl:600}")
    private long simulationConfigTtlSeconds;

    @Value("${app.cache.default-ttl:300}")
    private long defaultTtlSeconds;

    @Bean
    @Primary
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("currencyRates", defaultConfig
                .entryTtl(Duration.ofSeconds(currencyRatesTtlSeconds)));

        cacheConfigurations.put("instruments", defaultConfig
                .entryTtl(Duration.ofSeconds(instrumentsTtlSeconds)));

        cacheConfigurations.put("stockPrices", defaultConfig
                .entryTtl(Duration.ofSeconds(stockPricesTtlSeconds)));

        cacheConfigurations.put("historicalData", defaultConfig
                .entryTtl(Duration.ofSeconds(historicalDataTtlSeconds)));

        cacheConfigurations.put("news", defaultConfig
                .entryTtl(Duration.ofSeconds(newsTtlSeconds)));

        cacheConfigurations.put("userPortfolios", defaultConfig
                .entryTtl(Duration.ofSeconds(portfolioTtlSeconds)));

        cacheConfigurations.put("portfolios", defaultConfig
                .entryTtl(Duration.ofSeconds(portfolioTtlSeconds)));

        cacheConfigurations.put("users", defaultConfig
                .entryTtl(Duration.ofSeconds(usersTtlSeconds)));

        // Legacy cache names retained for backward compatibility
        cacheConfigurations.put("stock-prices", defaultConfig
                .entryTtl(Duration.ofSeconds(stockPricesTtlSeconds)));

        cacheConfigurations.put("currency-rates", defaultConfig
                .entryTtl(Duration.ofSeconds(currencyRatesTtlSeconds)));

        cacheConfigurations.put("index-values", defaultConfig
                .entryTtl(Duration.ofSeconds(indexValuesTtlSeconds)));

        cacheConfigurations.put("simulation-config", defaultConfig
                .entryTtl(Duration.ofSeconds(simulationConfigTtlSeconds)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofSeconds(defaultTtlSeconds)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
