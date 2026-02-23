package com.mintstack.finance.service.simulation;

import com.mintstack.finance.dto.simulation.MarketEvent;
import com.mintstack.finance.dto.simulation.MarketEvent.EventType;
import com.mintstack.finance.dto.simulation.MarketEvent.Severity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
class MarketEventFactory {

    private static final List<String> HEAVILY_SHORTED_STOCKS = List.of("SASA", "PGSUS", "THYAO");
    private static final List<String> SECTORS = List.of("BANKA", "HAVACILIK", "TEKNOLOJI", "OTOMOTIV", "METAL", "PETROL", "HOLDING", "KIMYA");
    private static final List<String> HIGH_VOLATILITY_STOCKS = List.of("SASA", "PGSUS", "KOZAL");

    private final Random random = new Random();

    Optional<MarketEvent> createEvent(EventType type, String eventId) {
        return switch (type) {
            case CIRCUIT_BREAKER -> Optional.empty();
            case SHORT_SQUEEZE -> Optional.of(createShortSqueezeEvent(eventId));
            case WHALE_ACTIVITY -> Optional.of(createWhaleActivityEvent(eventId));
            case SECTOR_ROTATION -> Optional.of(createSectorRotationEvent(eventId));
            case VOLATILITY_SPIKE -> Optional.of(createVolatilitySpikeEvent(eventId));
            case LIQUIDITY_CRISIS -> Optional.of(createLiquidityCrisisEvent(eventId));
            case HALT -> Optional.of(createHaltEvent(eventId));
            case RALLY -> Optional.of(createRallyEvent(eventId));
            case FLASH_CRASH -> Optional.of(createFlashCrashEvent(eventId));
            case GOLDEN_CROSS -> Optional.of(createGoldenCrossEvent(eventId));
            case DEATH_CROSS -> Optional.of(createDeathCrossEvent(eventId));
        };
    }

    EventType randomEventType() {
        EventType[] types = EventType.values();
        return types[random.nextInt(types.length)];
    }

    private MarketEvent createShortSqueezeEvent(String eventId) {
        String targetSymbol = HEAVILY_SHORTED_STOCKS.get(random.nextInt(HEAVILY_SHORTED_STOCKS.size()));
        double priceIncrease = 0.05 + random.nextDouble() * 0.10;
        int duration = 3 + random.nextInt(5);

        return MarketEvent.builder()
            .id(eventId)
            .type(EventType.SHORT_SQUEEZE)
            .description(String.format("Short Squeeze: %s uzerinde kisa pozisyon kapama - Fiyat %%%.1f yukseliyor", targetSymbol, priceIncrease * 100))
            .affectedSymbols(List.of(targetSymbol))
            .priceMultiplier(1.0 + priceIncrease)
            .volatilityMultiplier(2.5)
            .remainingDurationTicks(duration)
            .totalDurationTicks(duration)
            .startTime(LocalDateTime.now())
            .severity(Severity.HIGH)
            .isGlobal(false)
            .build();
    }

    private MarketEvent createWhaleActivityEvent(String eventId) {
        String targetSymbol = HIGH_VOLATILITY_STOCKS.get(random.nextInt(HIGH_VOLATILITY_STOCKS.size()));
        boolean isBuy = random.nextBoolean();
        double impact = 0.02 + random.nextDouble() * 0.05;

        return MarketEvent.builder()
            .id(eventId)
            .type(EventType.WHALE_ACTIVITY)
            .description(String.format("Balina Aktivitesi: %s uzerinde %s - Fiyat %%%.1f etki", targetSymbol, isBuy ? "devasa alim" : "devasa satim", impact * 100))
            .affectedSymbols(List.of(targetSymbol))
            .priceMultiplier(isBuy ? 1.0 + impact : 1.0 - impact)
            .volatilityMultiplier(2.0)
            .remainingDurationTicks(2)
            .totalDurationTicks(2)
            .startTime(LocalDateTime.now())
            .severity(Severity.MEDIUM)
            .isGlobal(false)
            .build();
    }

    private MarketEvent createSectorRotationEvent(String eventId) {
        String upSector = SECTORS.get(random.nextInt(SECTORS.size()));
        String downSector;
        do {
            downSector = SECTORS.get(random.nextInt(SECTORS.size()));
        } while (downSector.equals(upSector));

        int duration = 5 + random.nextInt(10);

        return MarketEvent.builder()
            .id(eventId)
            .type(EventType.SECTOR_ROTATION)
            .description(String.format("Sektor Rotasyonu: %s sektorune gecis, %s sektorunden cikis", upSector, downSector))
            .affectedSectors(List.of(upSector, downSector))
            .priceMultiplier(1.0)
            .volatilityMultiplier(1.3)
            .remainingDurationTicks(duration)
            .totalDurationTicks(duration)
            .startTime(LocalDateTime.now())
            .severity(Severity.MEDIUM)
            .isGlobal(false)
            .build();
    }

    private MarketEvent createVolatilitySpikeEvent(String eventId) {
        double volatilityMultiplier = 2.0 + random.nextDouble() * 2.0;
        int duration = 10 + random.nextInt(20);

        return MarketEvent.builder()
            .id(eventId)
            .type(EventType.VOLATILITY_SPIKE)
            .description(String.format("Volatilite Patlamasi: Piyasa volatilitesi %.1fx artti", volatilityMultiplier))
            .isGlobal(true)
            .priceMultiplier(1.0)
            .volatilityMultiplier(volatilityMultiplier)
            .remainingDurationTicks(duration)
            .totalDurationTicks(duration)
            .startTime(LocalDateTime.now())
            .severity(Severity.HIGH)
            .build();
    }

    private MarketEvent createLiquidityCrisisEvent(String eventId) {
        int numAffected = 3 + random.nextInt(5);
        List<String> affectedSymbols = new ArrayList<>();
        List<String> symbols = new ArrayList<>(HIGH_VOLATILITY_STOCKS);
        symbols.addAll(HEAVILY_SHORTED_STOCKS);

        for (int i = 0; i < numAffected && !symbols.isEmpty(); i++) {
            String symbol = symbols.get(random.nextInt(symbols.size()));
            affectedSymbols.add(symbol);
            symbols.remove(symbol);
        }

        int duration = 5 + random.nextInt(10);

        return MarketEvent.builder()
            .id(eventId)
            .type(EventType.LIQUIDITY_CRISIS)
            .description(String.format("Likidite Krizi: %d hissede likidite sorunu - Spreadler genisledi", numAffected))
            .affectedSymbols(affectedSymbols)
            .priceMultiplier(0.98)
            .volatilityMultiplier(1.8)
            .remainingDurationTicks(duration)
            .totalDurationTicks(duration)
            .startTime(LocalDateTime.now())
            .severity(Severity.HIGH)
            .isGlobal(false)
            .build();
    }

    private MarketEvent createHaltEvent(String eventId) {
        String targetSymbol = HIGH_VOLATILITY_STOCKS.get(random.nextInt(HIGH_VOLATILITY_STOCKS.size()));
        int haltMinutes = 2 + random.nextInt(3);

        return MarketEvent.builder()
            .id(eventId)
            .type(EventType.HALT)
            .description(String.format("Islem Durdurma: %s olagan disi hareket nedeniyle durduruldu", targetSymbol))
            .affectedSymbols(List.of(targetSymbol))
            .priceMultiplier(1.0)
            .volatilityMultiplier(0.0)
            .remainingDurationTicks(haltMinutes * 6)
            .totalDurationTicks(haltMinutes * 6)
            .startTime(LocalDateTime.now())
            .severity(Severity.HIGH)
            .tradingHalted(true)
            .haltDurationMinutes(haltMinutes)
            .isGlobal(false)
            .build();
    }

    private MarketEvent createRallyEvent(String eventId) {
        double rallyStrength = 0.02 + random.nextDouble() * 0.03;
        int duration = 8 + random.nextInt(12);

        return MarketEvent.builder()
            .id(eventId)
            .type(EventType.RALLY)
            .description(String.format("Piyasa Rallisi: Genis tabanli yukselis - %%%.1f artis", rallyStrength * 100))
            .isGlobal(true)
            .priceMultiplier(1.0 + rallyStrength)
            .volatilityMultiplier(1.2)
            .remainingDurationTicks(duration)
            .totalDurationTicks(duration)
            .startTime(LocalDateTime.now())
            .severity(Severity.MEDIUM)
            .build();
    }

    private MarketEvent createFlashCrashEvent(String eventId) {
        double dropPercent = 0.03 + random.nextDouble() * 0.02;
        int duration = 1 + random.nextInt(2);

        return MarketEvent.builder()
            .id(eventId)
            .type(EventType.FLASH_CRASH)
            .description(String.format("Flash Cokus: Anlik %%%.1f dusus sonrasi toparlanma bekleniyor", dropPercent * 100))
            .isGlobal(true)
            .priceMultiplier(1.0 - dropPercent)
            .volatilityMultiplier(3.0)
            .remainingDurationTicks(duration)
            .totalDurationTicks(duration)
            .startTime(LocalDateTime.now())
            .severity(Severity.CRITICAL)
            .build();
    }

    private MarketEvent createGoldenCrossEvent(String eventId) {
        String targetSymbol = HEAVILY_SHORTED_STOCKS.get(random.nextInt(HEAVILY_SHORTED_STOCKS.size()));
        int duration = 10 + random.nextInt(15);

        return MarketEvent.builder()
            .id(eventId)
            .type(EventType.GOLDEN_CROSS)
            .description(String.format("Golden Cross: %s 50 gunluk ortalama 200 gunlugu yukari kirdi", targetSymbol))
            .affectedSymbols(List.of(targetSymbol))
            .priceMultiplier(1.02)
            .volatilityMultiplier(1.1)
            .remainingDurationTicks(duration)
            .totalDurationTicks(duration)
            .startTime(LocalDateTime.now())
            .severity(Severity.LOW)
            .isGlobal(false)
            .build();
    }

    private MarketEvent createDeathCrossEvent(String eventId) {
        String targetSymbol = HIGH_VOLATILITY_STOCKS.get(random.nextInt(HIGH_VOLATILITY_STOCKS.size()));
        int duration = 10 + random.nextInt(15);

        return MarketEvent.builder()
            .id(eventId)
            .type(EventType.DEATH_CROSS)
            .description(String.format("Death Cross: %s 50 gunluk ortalama 200 gunlugu asagi kirdi", targetSymbol))
            .affectedSymbols(List.of(targetSymbol))
            .priceMultiplier(0.98)
            .volatilityMultiplier(1.2)
            .remainingDurationTicks(duration)
            .totalDurationTicks(duration)
            .startTime(LocalDateTime.now())
            .severity(Severity.MEDIUM)
            .isGlobal(false)
            .build();
    }
}
