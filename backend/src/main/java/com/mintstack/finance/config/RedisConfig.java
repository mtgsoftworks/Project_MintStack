package com.mintstack.finance.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration with secure type validation.
 *
 * SECURITY: The PolymorphicTypeValidator is configured with a strict whitelist
 * to prevent deserialization of arbitrary classes (RCE prevention).
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Creates a secure PolymorphicTypeValidator that only allows known, safe types.
     *
     * SECURITY: This prevents deserialization attacks where malicious payloads
     * could execute arbitrary code through Redis.
     */
    @Bean
    public PolymorphicTypeValidator secureRedisTypeValidator() {
        return BasicPolymorphicTypeValidator.builder()
            // Only allow explicitly listed package prefixes
            .allowIfSubType("com.mintstack.finance.dto.")
            .allowIfSubType("com.mintstack.finance.dto.cache.")
            .allowIfSubType("com.mintstack.finance.dto.response.")
            .allowIfSubType("com.mintstack.finance.dto.simulation.")
            // Allow Java standard types commonly used in serialization
            .allowIfSubType("java.util.")
            .allowIfSubType("java.time.")
            .allowIfSubType("java.math.")
            .allowIfSubType("java.lang.")
            .allowIfSubType("java.net.")
            // Allow common Spring types
            .allowIfSubType("org.springframework.data.redis.")
            .allowIfSubType("org.springframework.http.")
            .build();
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (StringUtils.hasText(redisPassword)) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            PolymorphicTypeValidator secureRedisTypeValidator) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // SECURITY: Use strict type validation to prevent RCE via deserialization
        objectMapper.activateDefaultTyping(
            secureRedisTypeValidator,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
