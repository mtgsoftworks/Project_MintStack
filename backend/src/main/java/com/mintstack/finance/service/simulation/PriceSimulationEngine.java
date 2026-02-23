package com.mintstack.finance.service.simulation;

import com.mintstack.finance.entity.SimulationConfig.MarketTrend;
import com.mintstack.finance.entity.SimulationConfig.VolatilityLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PriceSimulationEngine {

    private final Random random = new Random();
    private final Map<String, Double> recentVolatility = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();
    
    private final Map<String, Double> currentVolatility = new ConcurrentHashMap<>();
    private final Map<String, Double> longTermVolatility = new ConcurrentHashMap<>();
    private final Map<String, VolatilityRegime> volatilityRegime = new ConcurrentHashMap<>();
    
    private static final double ALPHA = 0.1;
    private static final double BETA = 0.85;
    private static final double OMEGA = 0.05;
    
    private static final Map<String, Map<String, Double>> SECTOR_CORRELATION = Map.of(
        "BANKA", Map.of("BANKA", 0.85, "HOLDING", 0.6, "TEKNOLOJI", 0.3, "HAVACILIK", 0.4, "OTOMOTIV", 0.5),
        "HAVACILIK", Map.of("HAVACILIK", 0.9, "OTOMOTIV", 0.4, "PETROL", 0.3, "BANKA", 0.4),
        "TEKNOLOJI", Map.of("TEKNOLOJI", 0.8, "HAVACILIK", 0.3, "BANKA", 0.3, "METAL", 0.2),
        "OTOMOTIV", Map.of("OTOMOTIV", 0.85, "METAL", 0.6, "PETROL", 0.5, "HAVACILIK", 0.4),
        "METAL", Map.of("METAL", 0.8, "OTOMOTIV", 0.6, "PETROL", 0.4, "KIMYA", 0.5),
        "PETROL", Map.of("PETROL", 0.85, "PETROKIMYA", 0.8, "OTOMOTIV", 0.5, "METAL", 0.4),
        "HOLDING", Map.of("HOLDING", 0.9, "BANKA", 0.6, "TEKNOLOJI", 0.4, "PERAKENDE", 0.5),
        "DEFAULT", Map.of("DEFAULT", 0.5)
    );

    public enum VolatilityRegime {
        LOW, NORMAL, HIGH, CRISIS
    }

    /**
     * Geometric Brownian Motion (GBM) - Hisse senetleri için ideal
     * dS = μ*S*dt + σ*S*dW
     * GARCH(1,1) volatilite clustering desteği ile
     * 
     * @param symbol Enstrüman sembolü
     * @param currentPrice Mevcut fiyat
     * @param baseVolatility Temel volatilite (yıllık)
     * @param volatilityLevel Kullanıcı seçimi volatilite seviyesi
     * @param trend Piyasa trendi
     * @param deltaTime Zaman dilimi (saniye cinsinden)
     * @return Yeni simüle edilmiş fiyat
     */
    public BigDecimal simulateGBM(String symbol, BigDecimal currentPrice, 
                                   double baseVolatility, VolatilityLevel volatilityLevel,
                                   MarketTrend trend, double deltaTime) {
        
        double price = currentPrice.doubleValue();
        
        double dynamicVolatility = currentVolatility.getOrDefault(symbol, baseVolatility);
        double regimeMultiplier = getRegimeMultiplier(symbol);
        double volMultiplier = getVolatilityMultiplier(volatilityLevel);
        double sigma = dynamicVolatility * volMultiplier * regimeMultiplier;
        
        double mu = trend.getDrift();
        double dt = deltaTime / (252.0 * 23400.0);
        double dW = random.nextGaussian() * Math.sqrt(dt);
        
        double clusterEffect = getVolatilityClusterEffect(symbol, sigma);
        sigma *= clusterEffect;
        
        double drift = (mu - 0.5 * sigma * sigma) * dt;
        double diffusion = sigma * dW;
        double newPrice = price * Math.exp(drift + diffusion);
        
        newPrice = Math.max(newPrice, price * 0.90);
        newPrice = Math.min(newPrice, price * 1.10);
        newPrice = Math.max(newPrice, 0.01);
        
        double priceReturn = (newPrice - price) / price;
        updateRecentVolatility(symbol, Math.abs(priceReturn));
        updateVolatility(symbol, baseVolatility, priceReturn);
        lastPrices.put(symbol, BigDecimal.valueOf(newPrice));
        
        return BigDecimal.valueOf(newPrice).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Mean Reversion (Ornstein-Uhlenbeck) - Döviz kurları için ideal
     * dX = θ(μ - X)dt + σdW
     * 
     * @param symbol Döviz kodu
     * @param currentPrice Mevcut kur
     * @param meanPrice Ortalama kur (dönüş noktası)
     * @param baseVolatility Temel volatilite
     * @param volatilityLevel Kullanıcı volatilite seviyesi
     * @param reversionSpeed Ortalamaya dönüş hızı (theta)
     * @param deltaTime Zaman dilimi (saniye)
     * @return Yeni simüle edilmiş kur
     */
    public BigDecimal simulateMeanReversion(String symbol, BigDecimal currentPrice,
                                             BigDecimal meanPrice, double baseVolatility,
                                             VolatilityLevel volatilityLevel,
                                             double reversionSpeed, double deltaTime) {
        
        double price = currentPrice.doubleValue();
        double mean = meanPrice.doubleValue();
        
        double volMultiplier = getVolatilityMultiplier(volatilityLevel);
        double sigma = baseVolatility * volMultiplier;
        
        double dt = deltaTime / (252.0 * 23400.0); // Trading days * trading seconds per day
        double dW = random.nextGaussian() * Math.sqrt(dt);
        
        // Ornstein-Uhlenbeck formülü
        double reversion = reversionSpeed * (mean - price) * dt;
        double diffusion = sigma * dW;
        double newPrice = price + reversion + diffusion;
        
        // Sınırlar (döviz için daha geniş aralık - TRY yılda %30-50 değişebilir)
        newPrice = Math.max(newPrice, mean * 0.70);
        newPrice = Math.min(newPrice, mean * 1.50);
        newPrice = Math.max(newPrice, 0.001);
        
        lastPrices.put(symbol, BigDecimal.valueOf(newPrice));
        
        return BigDecimal.valueOf(newPrice).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Spread simülasyonu (alış-satış farkı)
     */
    public BigDecimal[] simulateSpread(BigDecimal midPrice, double spreadPercent) {
        double mid = midPrice.doubleValue();
        double halfSpread = mid * spreadPercent / 2;
        
        // Küçük rastgele varyasyon ekle
        double variation = random.nextGaussian() * halfSpread * 0.1;
        
        BigDecimal buyingRate = BigDecimal.valueOf(mid - halfSpread + variation)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal sellingRate = BigDecimal.valueOf(mid + halfSpread + variation)
                .setScale(6, RoundingMode.HALF_UP);
        
        return new BigDecimal[]{buyingRate, sellingRate};
    }

    /**
     * Piyasa olayı simülasyonu
     * @return Fiyat çarpanı (1.0 = değişim yok, 1.02 = %2 artış)
     */
    public double simulateMarketEvent(boolean enableRandomEvents) {
        if (!enableRandomEvents) {
            return 1.0;
        }
        
        // %2 olasılıkla piyasa olayı
        if (random.nextDouble() < 0.02) {
            // Pozitif veya negatif olay
            boolean positive = random.nextBoolean();
            double impact = 0.01 + random.nextDouble() * 0.02; // %1-%3 etki
            
            if (positive) {
                log.info("🟢 Pozitif piyasa olayı simüle edildi: +{}%", String.format("%.2f", impact * 100));
                return 1.0 + impact;
            } else {
                log.info("🔴 Negatif piyasa olayı simüle edildi: -{}%", String.format("%.2f", impact * 100));
                return 1.0 - impact;
            }
        }
        
        return 1.0;
    }

    /**
     * Kripto para piyasası olayı simülasyonu
     * Daha yüksek olasılık (%5) ve etki (0-5%)
     * @return Fiyat çarpanı (1.0 = değişim yok)
     */
    public double simulateCryptoEvent(boolean enableRandomEvents) {
        if (!enableRandomEvents) {
            return 1.0;
        }
        
        // %5 olasılıkla kripto olayı (whale movement, regulation news)
        if (random.nextDouble() < 0.05) {
            double impact = random.nextDouble() * 0.05; // 0-5% etki
            boolean positive = random.nextBoolean();
            
            String eventType = positive ? "pozitif" : "negatif";
            String[] eventTypes = positive 
                ? new String[]{"Whale alımı", "Kurumsal yatırım", "Pozitif regülasyon", "Adopsiyon haberi"}
                : new String[]{"Whale satışı", "Regülasyon endişesi", "Exchange hack", "Pazar panik"};
            String eventName = eventTypes[random.nextInt(eventTypes.length)];
            
            log.info("🪙 Kripto olayı simüle edildi: {} - {} {}%", 
                    eventName, eventType, String.format("%.2f", impact * 100));
            
            return positive ? 1.0 + impact : 1.0 - impact;
        }
        
        return 1.0;
    }

    /**
     * Açılış gap simülasyonu
     */
    public double simulateOpeningGap() {
        // %30 olasılıkla gap
        if (random.nextDouble() < 0.30) {
            double gap = (random.nextDouble() - 0.5) * 0.01; // -%0.5 ile +%0.5 arası
            return 1.0 + gap;
        }
        return 1.0;
    }

    /**
     * Gün içi volatilite profili (U-şekilli)
     * Açılış ve kapanışta yüksek, öğle saatlerinde düşük
     */
    public double getIntradayVolatilityMultiplier(int hour, int minute) {
        // BIST saatleri: 10:00-18:00
        double totalMinutes = hour * 60 + minute - 600; // 10:00'dan itibaren
        double tradingMinutes = 480; // 8 saat
        
        if (totalMinutes < 0 || totalMinutes > tradingMinutes) {
            return 1.0; // Piyasa kapalı
        }
        
        // U-şekilli profil
        double normalized = totalMinutes / tradingMinutes; // 0-1 arası
        double volatility = 1.5 - Math.sin(normalized * Math.PI) * 0.7;
        
        return Math.max(0.5, Math.min(1.5, volatility));
    }

    private double getVolatilityMultiplier(VolatilityLevel level) {
        return switch (level) {
            case LOW -> 0.5;
            case MEDIUM -> 1.0;
            case HIGH -> 2.0;
            case EXTREME -> 4.0;
        };
    }

    private double getVolatilityClusterEffect(String symbol, double currentSigma) {
        Double recent = recentVolatility.get(symbol);
        if (recent == null) {
            return 1.0;
        }
        
        // Son volatilite yüksekse, mevcut volatiliteyi artır (clustering)
        double threshold = currentSigma * 1.5;
        if (recent > threshold) {
            return 1.3; // %30 artış
        }
        return 1.0;
    }

    private void updateRecentVolatility(String symbol, double volatility) {
        // Exponential moving average
        Double previous = recentVolatility.get(symbol);
        if (previous == null) {
            recentVolatility.put(symbol, volatility);
        } else {
            double alpha = 0.3;
            recentVolatility.put(symbol, alpha * volatility + (1 - alpha) * previous);
        }
    }

    public BigDecimal getLastPrice(String symbol) {
        return lastPrices.get(symbol);
    }

    public void setLastPrice(String symbol, BigDecimal price) {
        lastPrices.put(symbol, price);
    }

    public void clearState() {
        lastPrices.clear();
        recentVolatility.clear();
        currentVolatility.clear();
        longTermVolatility.clear();
        volatilityRegime.clear();
    }
    
    /**
     * Update volatility using GARCH(1,1) model
     * σ²(t) = ω + α*ε²(t-1) + β*σ²(t-1)
     */
    public double updateVolatility(String symbol, double baseVolatility, double priceReturn) {
        double previousVol = currentVolatility.getOrDefault(symbol, baseVolatility);
        double squaredReturn = priceReturn * priceReturn;
        
        double newVol = Math.sqrt(OMEGA + ALPHA * squaredReturn + BETA * previousVol * previousVol);
        
        newVol = Math.max(newVol, baseVolatility * 0.3);
        newVol = Math.min(newVol, baseVolatility * 3.0);
        
        currentVolatility.put(symbol, newVol);
        
        if (!longTermVolatility.containsKey(symbol)) {
            longTermVolatility.put(symbol, baseVolatility);
        } else {
            double currentLT = longTermVolatility.get(symbol);
            longTermVolatility.put(symbol, 0.99 * currentLT + 0.01 * newVol);
        }
        
        updateVolatilityRegime(symbol, newVol, baseVolatility);
        
        return newVol;
    }
    
    /**
     * Determine volatility regime
     */
    private void updateVolatilityRegime(String symbol, double currentVol, double baseVol) {
        double ratio = currentVol / baseVol;
        
        VolatilityRegime regime;
        if (ratio < 0.5) {
            regime = VolatilityRegime.LOW;
        } else if (ratio < 1.5) {
            regime = VolatilityRegime.NORMAL;
        } else if (ratio < 2.5) {
            regime = VolatilityRegime.HIGH;
        } else {
            regime = VolatilityRegime.CRISIS;
        }
        
        volatilityRegime.put(symbol, regime);
    }
    
    /**
     * Get regime-adjusted volatility multiplier
     */
    public double getRegimeMultiplier(String symbol) {
        VolatilityRegime regime = volatilityRegime.getOrDefault(symbol, VolatilityRegime.NORMAL);
        return switch (regime) {
            case LOW -> 0.7;
            case NORMAL -> 1.0;
            case HIGH -> 1.5;
            case CRISIS -> 2.5;
        };
    }
    
    /**
     * Get sector correlation between two sectors
     */
    public double getSectorCorrelation(String sector1, String sector2) {
        if (sector1 == null || sector2 == null) return 0.5;
        if (sector1.equals(sector2)) return 0.85;
        
        Map<String, Double> sectorCorrelations = SECTOR_CORRELATION.getOrDefault(sector1, SECTOR_CORRELATION.get("DEFAULT"));
        return sectorCorrelations.getOrDefault(sector2, 0.5);
    }
    
    /**
     * Calculate correlation between two symbols
     */
    public double getCorrelation(String symbol1, String symbol2) {
        return 0.5;
    }
    
    /**
     * Trigger volatility burst for a symbol (used by market events)
     */
    public void triggerVolatilityBurst(String symbol, double multiplier, int durationTicks) {
        double currentVol = currentVolatility.getOrDefault(symbol, 0.02);
        currentVolatility.put(symbol, currentVol * multiplier);
        
        VolatilityRegime currentRegime = volatilityRegime.get(symbol);
        if (multiplier >= 2.0) {
            volatilityRegime.put(symbol, VolatilityRegime.CRISIS);
        } else if (multiplier >= 1.5 && currentRegime != VolatilityRegime.CRISIS) {
            volatilityRegime.put(symbol, VolatilityRegime.HIGH);
        }
        
        log.info("⚡ Volatility burst triggered for {}: {}x multiplier, {} ticks duration", symbol, multiplier, durationTicks);
    }
    
    /**
     * Get current volatility for all symbols
     */
    public Map<String, Double> getCurrentVolatility() {
        return new HashMap<>(currentVolatility);
    }
    
    /**
     * Get regime distribution statistics
     */
    public Map<String, Long> getRegimeDistribution() {
        Map<String, Long> distribution = new HashMap<>();
        for (VolatilityRegime regime : VolatilityRegime.values()) {
            distribution.put(regime.name(), 
                volatilityRegime.values().stream().filter(r -> r == regime).count());
        }
        return distribution;
    }
    
    /**
     * Get volatility regime for a symbol
     */
    public VolatilityRegime getVolatilityRegime(String symbol) {
        return volatilityRegime.getOrDefault(symbol, VolatilityRegime.NORMAL);
    }
    
    /**
     * Get long-term volatility for a symbol
     */
    public double getLongTermVolatility(String symbol) {
        return longTermVolatility.getOrDefault(symbol, 0.02);
    }
}
