package com.mintstack.finance.config;

import com.mintstack.finance.service.ClusterWebSocketPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(
        value = "app.redis.cache.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RedisWebSocketConfig {

    @Bean
    public RedisMessageListenerContainer webSocketRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ClusterWebSocketPublisher publisher) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                (message, pattern) -> publisher.receive(
                        new String(message.getBody(), StandardCharsets.UTF_8)
                ),
                new ChannelTopic(ClusterWebSocketPublisher.CHANNEL)
        );
        return container;
    }
}
