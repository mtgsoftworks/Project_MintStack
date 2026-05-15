package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.SimulationConfig;
import com.mintstack.finance.service.simulation.SimulationDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimulationScheduler {

    private final SimulationDataService simulationDataService;
    private final AtomicLong tickCount = new AtomicLong(0);
    private volatile long lastUpdateTime = 0;

    @Value("${app.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * Ana simülasyon döngüsü - Her saniye çalışır
     * Gerçek güncelleme aralığı config'den alınır
     */
    @Scheduled(fixedRate = 1000)
    public void simulationTick() {
        if (!schedulerEnabled) {
            return;
        }
        if (!simulationDataService.isSimulationEnabled()) {
            return;
        }

        SimulationConfig config = simulationDataService.getConfig();
        int intervalSeconds = config.getUpdateIntervalSeconds();
        
        // Market hours kontrolü (opsiyonel)
        if (config.getEnableMarketHours() && !isMarketOpen()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastUpdateTime;

        // Belirlenen aralıkta güncelle
        if (elapsed >= intervalSeconds * 1000L) {
            try {
                simulationDataService.simulateAllPrices();
                lastUpdateTime = currentTime;
                
                long tick = tickCount.incrementAndGet();
                if (tick % 60 == 0) { // Her dakika log
                    log.debug("🎮 Simülasyon çalışıyor - Tick #{}, Volatilite: {}, Trend: {}",
                            tick, config.getVolatilityLevel(), config.getMarketTrend());
                }
            } catch (Exception e) {
                log.error("Simülasyon hatası", e);
            }
        }
    }

    /**
     * Gece yarısı günlük reset - previousClose değerlerini güncelle
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void dailyReset() {
        if (!schedulerEnabled) {
            return;
        }
        if (!simulationDataService.isSimulationEnabled()) {
            return;
        }
        
        log.info("🌙 Simülasyon günlük reset - yeni işlem günü başlıyor");
        // previousClose değerleri otomatik olarak SimulatedStock/Currency sınıflarında güncelleniyor
    }

    /**
     * BIST piyasa saatleri kontrolü
     * Hafta içi 10:00-18:00
     */
    private boolean isMarketOpen() {
        LocalTime now = LocalTime.now();
        LocalTime marketOpen = LocalTime.of(10, 0);
        LocalTime marketClose = LocalTime.of(18, 0);
        
        // Hafta sonu kontrolü
        java.time.DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
        if (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY) {
            return false;
        }
        
        return now.isAfter(marketOpen) && now.isBefore(marketClose);
    }

    public long getTickCount() {
        return tickCount.get();
    }

    public void resetTickCount() {
        tickCount.set(0);
        lastUpdateTime = 0;
    }
}
