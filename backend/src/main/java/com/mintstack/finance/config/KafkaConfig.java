package com.mintstack.finance.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.security.protocol:PLAINTEXT}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism:}")
    private String saslMechanism;

    @Value("${spring.kafka.properties.sasl.jaas.config:}")
    private String saslJaasConfig;

    @Value("${app.messaging.market-data-consumer.retry.max-attempts:3}")
    private long marketDataRetryMaxAttempts;

    @Value("${app.messaging.market-data-consumer.retry.backoff-ms:1000}")
    private long marketDataRetryBackoffMs;

    @Value("${app.messaging.market-data-consumer.dlt-enabled:true}")
    private boolean marketDataDltEnabled;

    // Topic names
    public static final String TOPIC_LOGS = "mintstack-logs";
    public static final String TOPIC_MARKET_DATA = "mintstack-market-data";
    public static final String TOPIC_MARKET_DATA_DLT = "mintstack-market-data-dlt";
    public static final String TOPIC_NOTIFICATIONS = "mintstack-notifications";

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        addSecurityProps(configs);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic logsTopic() {
        return TopicBuilder.name(TOPIC_LOGS)
            .partitions(3)
            .replicas(1)
            .compact()
            .build();
    }

    @Bean
    public NewTopic marketDataTopic() {
        return TopicBuilder.name(TOPIC_MARKET_DATA)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.messaging.market-data-consumer.dlt-enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic marketDataDltTopic() {
        return TopicBuilder.name(TOPIC_MARKET_DATA_DLT)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATIONS)
            .partitions(2)
            .replicas(1)
            .build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        addSecurityProps(configProps);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "finance-portal");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        addSecurityProps(props);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        return factory;
    }

    @Bean(name = "marketDataKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> marketDataKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(marketDataErrorHandler(kafkaTemplate));
        return factory;
    }

    @Bean
    public CommonErrorHandler marketDataErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        long retries = Math.max(marketDataRetryMaxAttempts - 1, 0);
        FixedBackOff backOff = new FixedBackOff(marketDataRetryBackoffMs, retries);

        if (!marketDataDltEnabled) {
            DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
            errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
            return errorHandler;
        }

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> new TopicPartition(TOPIC_MARKET_DATA_DLT, record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }

    private void addSecurityProps(Map<String, Object> props) {
        if (!StringUtils.hasText(securityProtocol)) {
            return;
        }

        props.put("security.protocol", securityProtocol);
        if (securityProtocol.startsWith("SASL")) {
            if (StringUtils.hasText(saslMechanism)) {
                props.put("sasl.mechanism", saslMechanism);
            }
            if (StringUtils.hasText(saslJaasConfig)) {
                props.put("sasl.jaas.config", saslJaasConfig);
            }
        }
    }
}
