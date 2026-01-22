package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.MonteCarloResult;
import com.mintstack.finance.dto.response.PortfolioRiskResult;
import com.mintstack.finance.dto.response.VaRResult;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Portfolio;
import com.mintstack.finance.entity.PortfolioItem;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PortfolioRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Monte Carlo Simülasyon Servisi
 * Fiyat tahminleri, VaR hesaplama ve portföy risk analizi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonteCarloService {

    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PortfolioRepository portfolioRepository;

    private static final int DEFAULT_SIMULATIONS = 10000;
    private static final double DEFAULT_CONFIDENCE = 0.95;
    private static final int TRADING_DAYS_PER_YEAR = 252;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    /**
     * Monte Carlo simülasyonu çalıştır
     * Geometric Brownian Motion (GBM) kullanarak fiyat yolları üretir
     *
     * @param symbol Enstrüman sembolü
     * @param days Tahmin günü
     * @param simulations Simülasyon sayısı
     * @param confidence Güven düzeyi (örn: 0.95 = %95)
     * @return Monte Carlo sonuçları
     */
    public MonteCarloResult runSimulation(String symbol, int days, int simulations, double confidence) {
        log.info("Monte Carlo simülasyonu başlatılıyor: {} - {} gün, {} simülasyon", 
                symbol, days, simulations);

        Optional<Instrument> instrumentOpt = instrumentRepository.findBySymbol(symbol);
        if (instrumentOpt.isEmpty()) {
            log.warn("Enstrüman bulunamadı: {}", symbol);
            return null;
        }

        Instrument instrument = instrumentOpt.get();
        BigDecimal currentPrice = instrument.getCurrentPrice();
        if (currentPrice == null) {
            log.warn("Mevcut fiyat bulunamadı: {}", symbol);
            return null;
        }

        // Tarihsel volatilite ve ortalama getiri hesapla
        double[] returns = calculateHistoricalReturns(symbol, 252);
        if (returns.length < 30) {
            log.warn("Yeterli tarihsel veri yok: {}", symbol);
            // Varsayılan değerler kullan
            returns = generateDefaultReturns();
        }

        double meanReturn = calculateMean(returns);
        double volatility = calculateStdDev(returns);
        double annualizedVolatility = volatility * Math.sqrt(TRADING_DAYS_PER_YEAR);
        double annualizedReturn = meanReturn * TRADING_DAYS_PER_YEAR;

        // Paralel Monte Carlo simülasyonu
        double[] finalPrices = runParallelSimulations(
                currentPrice.doubleValue(), 
                days, 
                simulations, 
                annualizedReturn, 
                annualizedVolatility
        );

        // Sonuçları analiz et
        Arrays.sort(finalPrices);
        
        double mean = calculateMean(finalPrices);
        int p5Index = (int) (simulations * 0.05);
        int p50Index = simulations / 2;
        int p95Index = (int) (simulations * 0.95);
        int varIndex = (int) (simulations * (1 - confidence));

        double p5 = finalPrices[p5Index];
        double p50 = finalPrices[p50Index];
        double p95 = finalPrices[p95Index];
        double var = currentPrice.doubleValue() - finalPrices[varIndex];

        // Histogram oluştur (20 bins)
        double[] histogram = createHistogram(finalPrices, 20);

        return new MonteCarloResult(
                symbol,
                days,
                simulations,
                currentPrice,
                BigDecimal.valueOf(mean).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(p5).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(p50).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(p95).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(Math.max(0, var)).setScale(2, RoundingMode.HALF_UP),
                confidence,
                histogram
        );
    }

    /**
     * VaR (Value at Risk) hesapla
     */
    public VaRResult calculateVaR(String symbol, int days, double confidence) {
        MonteCarloResult mcResult = runSimulation(symbol, days, DEFAULT_SIMULATIONS, confidence);
        if (mcResult == null) {
            return null;
        }

        double varAmount = mcResult.var().doubleValue();
        double varPercent = (varAmount / mcResult.currentPrice().doubleValue()) * 100;

        return new VaRResult(
                symbol,
                days,
                confidence,
                mcResult.currentPrice(),
                mcResult.var(),
                varPercent,
                "Monte Carlo (" + DEFAULT_SIMULATIONS + " simülasyon)"
        );
    }

    /**
     * Portföy risk analizi
     */
    public PortfolioRiskResult analyzePortfolioRisk(UUID portfolioId, int days, int simulations) {
        log.info("Portföy risk analizi: {} - {} gün", portfolioId, days);

        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(portfolioId);
        if (portfolioOpt.isEmpty()) {
            log.warn("Portföy bulunamadı: {}", portfolioId);
            return null;
        }

        Portfolio portfolio = portfolioOpt.get();
        List<PortfolioItem> items = portfolio.getItems();
        if (items == null || items.isEmpty()) {
            log.warn("Portföy boş: {}", portfolioId);
            return null;
        }

        // Her enstrüman için Monte Carlo çalıştır
        Map<String, MonteCarloResult> assetResults = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        Map<String, BigDecimal> assetValues = new HashMap<>();

        for (PortfolioItem item : items) {
            String symbol = item.getInstrument().getSymbol();
            BigDecimal quantity = item.getQuantity();
            BigDecimal price = item.getInstrument().getCurrentPrice();
            
            if (price != null) {
                BigDecimal value = price.multiply(quantity);
                totalValue = totalValue.add(value);
                assetValues.put(symbol, value);
                
                MonteCarloResult mcResult = runSimulation(symbol, days, simulations / 10, 0.95);
                if (mcResult != null) {
                    assetResults.put(symbol, mcResult);
                }
            }
        }

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // Portföy VaR hesapla (basitleştirilmiş - korelasyon yok sayılıyor)
        BigDecimal portfolioVaR = BigDecimal.ZERO;
        Map<String, Double> contributions = new HashMap<>();

        for (Map.Entry<String, MonteCarloResult> entry : assetResults.entrySet()) {
            String symbol = entry.getKey();
            MonteCarloResult mc = entry.getValue();
            BigDecimal assetValue = assetValues.get(symbol);
            
            double weight = assetValue.doubleValue() / totalValue.doubleValue();
            double assetVaR = mc.var().doubleValue() * weight;
            portfolioVaR = portfolioVaR.add(BigDecimal.valueOf(assetVaR));
            contributions.put(symbol, weight * 100);
        }

        // Beklenen getiri ve Sharpe ratio (basit hesaplama)
        double expectedReturn = 0;
        for (MonteCarloResult mc : assetResults.values()) {
            double ret = (mc.meanForecast().doubleValue() - mc.currentPrice().doubleValue()) 
                        / mc.currentPrice().doubleValue();
            expectedReturn += ret;
        }
        expectedReturn = expectedReturn / assetResults.size() * 100;

        double riskFreeRate = 0.05; // %5 risksiz oran
        double portfolioVolatility = portfolioVaR.doubleValue() / totalValue.doubleValue();
        double sharpeRatio = portfolioVolatility > 0 
                ? (expectedReturn / 100 - riskFreeRate) / portfolioVolatility 
                : 0;

        return new PortfolioRiskResult(
                portfolioId,
                portfolio.getName(),
                totalValue.setScale(2, RoundingMode.HALF_UP),
                portfolioVaR.setScale(2, RoundingMode.HALF_UP),
                expectedReturn,
                Math.round(sharpeRatio * 100.0) / 100.0,
                days,
                contributions
        );
    }

    // =================== PRIVATE HELPER METHODS ===================

    private double[] runParallelSimulations(double startPrice, int days, int simulations,
                                             double annualizedReturn, double annualizedVolatility) {
        double[] results = new double[simulations];
        double dt = 1.0 / TRADING_DAYS_PER_YEAR;
        double drift = (annualizedReturn - 0.5 * annualizedVolatility * annualizedVolatility) * dt;
        double diffusion = annualizedVolatility * Math.sqrt(dt);

        Random random = new Random();

        // Paralel simülasyon
        int batchSize = simulations / Runtime.getRuntime().availableProcessors();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int batch = 0; batch < simulations; batch += batchSize) {
            int start = batch;
            int end = Math.min(batch + batchSize, simulations);
            
            futures.add(CompletableFuture.runAsync(() -> {
                Random localRandom = new Random();
                for (int i = start; i < end; i++) {
                    double price = startPrice;
                    for (int day = 0; day < days; day++) {
                        double randomShock = localRandom.nextGaussian();
                        price = price * Math.exp(drift + diffusion * randomShock);
                    }
                    results[i] = price;
                }
            }, executor));
        }

        // Tüm simülasyonların bitmesini bekle
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return results;
    }

    private double[] calculateHistoricalReturns(String symbol, int days) {
        Optional<Instrument> instrumentOpt = instrumentRepository.findBySymbol(symbol);
        if (instrumentOpt.isEmpty()) {
            return new double[0];
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days * 2);

        List<PriceHistory> history = priceHistoryRepository
                .findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                        instrumentOpt.get().getId(), startDate, endDate);

        if (history.size() < 2) {
            return new double[0];
        }

        double[] returns = new double[history.size() - 1];
        for (int i = 1; i < history.size(); i++) {
            double prevPrice = history.get(i - 1).getClosePrice().doubleValue();
            double currPrice = history.get(i).getClosePrice().doubleValue();
            returns[i - 1] = Math.log(currPrice / prevPrice); // Log return
        }

        return returns;
    }

    private double[] generateDefaultReturns() {
        // Varsayılan değerler: %8 yıllık getiri, %25 volatilite
        Random random = new Random();
        double[] returns = new double[252];
        double dailyMean = 0.08 / 252;
        double dailyVol = 0.25 / Math.sqrt(252);
        
        for (int i = 0; i < 252; i++) {
            returns[i] = dailyMean + dailyVol * random.nextGaussian();
        }
        return returns;
    }

    private double calculateMean(double[] values) {
        return Arrays.stream(values).average().orElse(0);
    }

    private double calculateStdDev(double[] values) {
        double mean = calculateMean(values);
        double sumSquaredDiff = 0;
        for (double value : values) {
            sumSquaredDiff += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sumSquaredDiff / values.length);
    }

    private double[] createHistogram(double[] values, int bins) {
        double min = values[0];
        double max = values[values.length - 1];
        double binWidth = (max - min) / bins;
        
        double[] histogram = new double[bins];
        
        for (double value : values) {
            int binIndex = (int) ((value - min) / binWidth);
            if (binIndex >= bins) binIndex = bins - 1;
            if (binIndex < 0) binIndex = 0;
            histogram[binIndex]++;
        }
        
        // Normalize (yüzde olarak)
        for (int i = 0; i < bins; i++) {
            histogram[i] = (histogram[i] / values.length) * 100;
        }
        
        return histogram;
    }
}
