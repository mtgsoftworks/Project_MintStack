package com.mintstack.finance.service.event;

import com.mintstack.finance.config.KafkaConfig;
import com.mintstack.finance.service.search.OpenSearchService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = {"app.messaging.enabled", "app.messaging.market-data-consumer.enabled"},
        havingValue = "true",
        matchIfMissing = true
)
public class MarketDataEventConsumer {

    private final OpenSearchService openSearchService;
    private final MeterRegistry meterRegistry;

    @Observed(name = "kafka.consume.market-data", contextualName = "consume-market-data-event")
    @KafkaListener(
            topics = KafkaConfig.TOPIC_MARKET_DATA,
            groupId = "${app.messaging.market-data-consumer.group-id:market-data-consumer}",
            containerFactory = "marketDataKafkaListenerContainerFactory"
    )
    public void consumeMarketData(MarketDataEvent event) {
        if (event == null) {
            log.warn("Received null market data event.");
            return;
        }

        String symbol = normalize(event.getSymbol());
        String type = normalize(event.getType());
        String eventId = normalize(event.getId());
        validateEvent(event, eventId);

        try {
            openSearchService.indexMarketData(symbol, type, event.getData());
            meterRegistry.counter("mintstack.kafka.market_data.consumed").increment();
            log.debug("Market data event processed. id={}, symbol={}, type={}", eventId, symbol, type);
        } catch (Exception error) {
            meterRegistry.counter("mintstack.kafka.market_data.failed").increment();
            log.error("Failed to process market data event. id={}, symbol={}, type={}", eventId, symbol, type, error);
            throw error;
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private void validateEvent(MarketDataEvent event, String eventId) {
        if (!StringUtils.hasText(event.getSymbol()) || !StringUtils.hasText(event.getType()) || event.getData() == null) {
            meterRegistry.counter("mintstack.kafka.market_data.validation_failed").increment();
            throw new IllegalArgumentException(
                "Invalid market data event. id=%s symbol=%s type=%s".formatted(
                    eventId,
                    event.getSymbol(),
                    event.getType()
                )
            );
        }
    }
}
