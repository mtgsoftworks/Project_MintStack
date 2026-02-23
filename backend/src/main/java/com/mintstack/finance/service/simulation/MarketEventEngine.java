package com.mintstack.finance.service.simulation;

import com.mintstack.finance.dto.simulation.MarketEvent;
import com.mintstack.finance.dto.simulation.MarketEvent.EventType;
import com.mintstack.finance.dto.simulation.MarketEvent.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketEventEngine {

    private static final double CIRCUIT_BREAKER_THRESHOLD = -0.10;
    private static final int CIRCUIT_BREAKER_HALT_MINUTES = 5;

    private final MarketEventFactory marketEventFactory;
    private final Map<String, MarketEvent> activeEvents = new ConcurrentHashMap<>();

    private int eventCounter = 0;

    public Optional<MarketEvent> checkForEvent(Map<String, SimulatedStock> stocks) {
        Optional<MarketEvent> circuitBreaker = checkCircuitBreaker(stocks);
        if (circuitBreaker.isPresent()) {
            return circuitBreaker;
        }

        if (Math.random() < 0.01) {
            return generateRandomEvent();
        }

        return Optional.empty();
    }

    public Optional<MarketEvent> generateRandomEvent() {
        EventType type = marketEventFactory.randomEventType();
        return generateEventByType(type);
    }

    public Optional<MarketEvent> generateEventByType(EventType type) {
        String eventId = "EVT-" + (++eventCounter) + "-" + System.currentTimeMillis();
        Optional<MarketEvent> event = marketEventFactory.createEvent(type, eventId);
        event.ifPresent(this::activateEvent);
        return event;
    }

    public List<MarketEvent> getActiveEventsForSymbol(String symbol) {
        return activeEvents.values().stream()
            .filter((event) -> {
                if (event.isGlobal()) {
                    return true;
                }
                if (event.getAffectedSymbols() != null && event.getAffectedSymbols().contains(symbol)) {
                    return true;
                }
                return event.getAffectedSectors() != null && !event.getAffectedSectors().isEmpty();
            })
            .filter(MarketEvent::isActive)
            .collect(Collectors.toList());
    }

    public void applyEventEffects(MarketEvent event, Map<String, SimulatedStock> stocks) {
        if (!event.isActive()) {
            return;
        }

        if (event.isTradingHalted()) {
            log.debug("Trading halted - skipping price update");
            return;
        }

        for (Map.Entry<String, SimulatedStock> entry : stocks.entrySet()) {
            String symbol = entry.getKey();
            SimulatedStock stock = entry.getValue();

            List<MarketEvent> affectingEvents = getActiveEventsForSymbol(symbol);

            double combinedPriceMultiplier = 1.0;
            double combinedVolatilityMultiplier = 1.0;

            for (MarketEvent activeEvent : affectingEvents) {
                if (activeEvent.isTradingHalted()
                    && activeEvent.getAffectedSymbols() != null
                    && activeEvent.getAffectedSymbols().contains(symbol)) {
                    continue;
                }

                boolean applies = false;
                if (activeEvent.isGlobal()) {
                    applies = true;
                } else if (activeEvent.getAffectedSymbols() != null && activeEvent.getAffectedSymbols().contains(symbol)) {
                    applies = true;
                } else if (activeEvent.getAffectedSectors() != null && stock.getSector() != null) {
                    if (activeEvent.getAffectedSectors().contains(stock.getSector())) {
                        int sectorIndex = activeEvent.getAffectedSectors().indexOf(stock.getSector());
                        if (sectorIndex == 0) {
                            combinedPriceMultiplier *= activeEvent.getPriceMultiplier();
                        } else {
                            combinedPriceMultiplier *= (2.0 - activeEvent.getPriceMultiplier());
                        }
                        applies = true;
                    }
                }

                if (applies) {
                    combinedPriceMultiplier *= activeEvent.getPriceMultiplier();
                    combinedVolatilityMultiplier *= activeEvent.getVolatilityMultiplier();
                }
            }

            if (combinedPriceMultiplier != 1.0 || combinedVolatilityMultiplier != 1.0) {
                BigDecimal currentPrice = stock.getCurrentPrice();
                BigDecimal newPrice = currentPrice.multiply(BigDecimal.valueOf(combinedPriceMultiplier))
                    .setScale(2, RoundingMode.HALF_UP);
                stock.updatePrice(newPrice);
            }
        }
    }

    public void decayEvents() {
        Iterator<Map.Entry<String, MarketEvent>> iterator = activeEvents.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, MarketEvent> entry = iterator.next();
            MarketEvent event = entry.getValue();

            event.tick();

            if (!event.isActive()) {
                log.info("Market event ended: {} ({})", event.getType(), event.getDescription());
                if (event.getType() == EventType.CIRCUIT_BREAKER) {
                    log.info("Circuit breaker lifted");
                }
                iterator.remove();
            }
        }
    }

    public Map<String, MarketEvent> getActiveEvents() {
        return Collections.unmodifiableMap(activeEvents);
    }

    public boolean isTradingHalted(String symbol) {
        return activeEvents.values().stream()
            .filter(MarketEvent::isTradingHalted)
            .filter(MarketEvent::isActive)
            .anyMatch((event) -> event.isGlobal()
                || (event.getAffectedSymbols() != null && event.getAffectedSymbols().contains(symbol)));
    }

    public void clearAllEvents() {
        activeEvents.clear();
        eventCounter = 0;
        log.info("All market events cleared");
    }

    private Optional<MarketEvent> checkCircuitBreaker(Map<String, SimulatedStock> stocks) {
        Optional<SimulatedStock> bist100 = Optional.ofNullable(stocks.get("XU100"));
        if (bist100.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal changePercent = bist100.get().getChangePercent();
        if (changePercent.doubleValue() > CIRCUIT_BREAKER_THRESHOLD * 100) {
            return Optional.empty();
        }

        String eventId = "CB-" + System.currentTimeMillis();
        MarketEvent event = MarketEvent.builder()
            .id(eventId)
            .type(EventType.CIRCUIT_BREAKER)
            .description("Circuit breaker triggered: BIST 100 dropped over 10%")
            .isGlobal(true)
            .priceMultiplier(0.0)
            .volatilityMultiplier(0.0)
            .remainingDurationTicks(CIRCUIT_BREAKER_HALT_MINUTES * 6)
            .totalDurationTicks(CIRCUIT_BREAKER_HALT_MINUTES * 6)
            .startTime(LocalDateTime.now())
            .severity(Severity.CRITICAL)
            .tradingHalted(true)
            .haltDurationMinutes(CIRCUIT_BREAKER_HALT_MINUTES)
            .build();

        activateEvent(event);
        log.warn(
            "Circuit breaker triggered - BIST 100 change: {}%, halt minutes: {}",
            String.format("%.2f", changePercent.doubleValue()),
            CIRCUIT_BREAKER_HALT_MINUTES
        );
        return Optional.of(event);
    }

    private void activateEvent(MarketEvent event) {
        activeEvents.put(event.getId(), event);
    }
}
