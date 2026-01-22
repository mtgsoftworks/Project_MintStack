package com.mintstack.finance.service.simulation;

import com.mintstack.finance.entity.SimulationConfig.MarketTrend;
import com.mintstack.finance.entity.SimulationConfig.VolatilityLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PriceSimulationEngine {

    private final Random random = new Random();
    private final Map<String, Double> recentVolatility = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();

    /**
     * Geometric Brownian Motion (GBM) - Hisse senetleri için ideal
     * dS = μ*S*dt + σ*S*dW
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
        
        // Volatilite çarpanı
        double volMultiplier = getVolatilityMultiplier(volatilityLevel);
        double sigma = baseVolatility * volMultiplier;
        
        // Drift (trend etkisi)
        double mu = trend.getDrift();
        
        // Zaman normalizasyonu (yıllık bazda)
        double dt = deltaTime / (252.0 * 24 * 60 * 60); // Trading days * hours * minutes * seconds
        
        // Wiener process (Brownian motion)
        double dW = random.nextGaussian() * Math.sqrt(dt);
        
        // Volatility clustering - son volatiliteyi kontrol et
        double clusterEffect = getVolatilityClusterEffect(symbol, sigma);
        sigma *= clusterEffect;
        
        // GBM formülü
        double drift = (mu - 0.5 * sigma * sigma) * dt;
        double diffusion = sigma * dW;
        double newPrice = price * Math.exp(drift + diffusion);
        
        // Fiyat sınırları (negatif olamaz, %10'dan fazla tek seferde hareket edemez)
        newPrice = Math.max(newPrice, price * 0.90);
        newPrice = Math.min(newPrice, price * 1.10);
        newPrice = Math.max(newPrice, 0.01);
        
        // Son volatiliteyi kaydet
        updateRecentVolatility(symbol, Math.abs(newPrice - price) / price);
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
        
        double dt = deltaTime / (252.0 * 24 * 60 * 60);
        double dW = random.nextGaussian() * Math.sqrt(dt);
        
        // Ornstein-Uhlenbeck formülü
        double reversion = reversionSpeed * (mean - price) * dt;
        double diffusion = sigma * dW;
        double newPrice = price + reversion + diffusion;
        
        // Sınırlar
        newPrice = Math.max(newPrice, mean * 0.85);
        newPrice = Math.min(newPrice, mean * 1.15);
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
    }
}
