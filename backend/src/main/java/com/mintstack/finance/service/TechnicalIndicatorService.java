package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.BollingerBandsResult;
import com.mintstack.finance.dto.response.MACDResult;
import com.mintstack.finance.dto.response.StochasticResult;
import com.mintstack.finance.dto.response.TechnicalIndicatorsResult;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Teknik Analiz Göstergeleri Servisi
 * RSI, MACD, Bollinger Bands, SMA, EMA ve daha fazlası
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicalIndicatorService {

    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    private static final int DEFAULT_RSI_PERIOD = 14;
    private static final int DEFAULT_MACD_FAST = 12;
    private static final int DEFAULT_MACD_SLOW = 26;
    private static final int DEFAULT_MACD_SIGNAL = 9;
    private static final int DEFAULT_BOLLINGER_PERIOD = 20;
    private static final double DEFAULT_BOLLINGER_STD_DEV = 2.0;

    /**
     * RSI (Relative Strength Index) hesaplama
     * RSI = 100 - (100 / (1 + RS))
     * RS = Average Gain / Average Loss
     *
     * @param symbol Enstrüman sembolü
     * @param period Periyot (varsayılan 14)
     * @return RSI değeri (0-100 arası) veya null
     */
    public Double calculateRSI(String symbol, int period) {
        List<BigDecimal> closePrices = getClosePrices(symbol, period + 1);
        if (closePrices.size() < period + 1) {
            log.warn("RSI hesaplaması için yeterli veri yok: {} (gerekli: {}, mevcut: {})",
                    symbol, period + 1, closePrices.size());
            return null;
        }

        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();

        for (int i = 1; i < closePrices.size(); i++) {
            double change = closePrices.get(i).subtract(closePrices.get(i - 1)).doubleValue();
            if (change > 0) {
                gains.add(change);
                losses.add(0.0);
            } else {
                gains.add(0.0);
                losses.add(Math.abs(change));
            }
        }

        // İlk ortalama (SMA)
        double avgGain = gains.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgLoss = losses.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        if (avgLoss == 0) {
            return 100.0; // Tüm hareketler yukarı
        }

        double rs = avgGain / avgLoss;
        double rsi = 100 - (100 / (1 + rs));

        return Math.round(rsi * 100.0) / 100.0;
    }

    /**
     * MACD (Moving Average Convergence Divergence) hesaplama
     * MACD Line = 12-period EMA - 26-period EMA
     * Signal Line = 9-period EMA of MACD Line
     * Histogram = MACD Line - Signal Line
     */
    public MACDResult calculateMACD(String symbol) {
        return calculateMACD(symbol, DEFAULT_MACD_FAST, DEFAULT_MACD_SLOW, DEFAULT_MACD_SIGNAL);
    }

    public MACDResult calculateMACD(String symbol, int fastPeriod, int slowPeriod, int signalPeriod) {
        int requiredPeriod = slowPeriod + signalPeriod;
        List<BigDecimal> closePrices = getClosePrices(symbol, requiredPeriod);
        
        if (closePrices.size() < requiredPeriod) {
            log.warn("MACD hesaplaması için yeterli veri yok: {}", symbol);
            return null;
        }

        // EMA hesaplamaları
        double[] prices = closePrices.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        double fastEMA = calculateEMAFromPrices(prices, fastPeriod);
        double slowEMA = calculateEMAFromPrices(prices, slowPeriod);
        double macdLine = fastEMA - slowEMA;

        // Signal line için MACD değerlerini hesapla
        List<Double> macdValues = new ArrayList<>();
        for (int i = slowPeriod - 1; i < prices.length; i++) {
            double fEMA = calculateEMAFromPrices(java.util.Arrays.copyOf(prices, i + 1), fastPeriod);
            double sEMA = calculateEMAFromPrices(java.util.Arrays.copyOf(prices, i + 1), slowPeriod);
            macdValues.add(fEMA - sEMA);
        }

        double signalLine = calculateEMAFromDoubles(macdValues, signalPeriod);
        double histogram = macdLine - signalLine;

        return new MACDResult(
                round(macdLine),
                round(signalLine),
                round(histogram),
                fastPeriod,
                slowPeriod,
                signalPeriod
        );
    }

    /**
     * Bollinger Bands hesaplama
     * Middle Band = 20-period SMA
     * Upper Band = Middle Band + (2 * Standard Deviation)
     * Lower Band = Middle Band - (2 * Standard Deviation)
     */
    public BollingerBandsResult calculateBollingerBands(String symbol, int period, double stdDevMultiplier) {
        List<BigDecimal> closePrices = getClosePrices(symbol, period);
        
        if (closePrices.size() < period) {
            log.warn("Bollinger Bands hesaplaması için yeterli veri yok: {}", symbol);
            return null;
        }

        double[] prices = closePrices.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        
        // Middle Band (SMA)
        double middleBand = java.util.Arrays.stream(prices).average().orElse(0);
        
        // Standart sapma hesaplama
        double variance = 0;
        for (double price : prices) {
            variance += Math.pow(price - middleBand, 2);
        }
        double stdDev = Math.sqrt(variance / period);
        
        double upperBand = middleBand + (stdDevMultiplier * stdDev);
        double lowerBand = middleBand - (stdDevMultiplier * stdDev);
        
        // Bandwidth ve %B hesaplama
        double bandwidth = ((upperBand - lowerBand) / middleBand) * 100;
        double currentPrice = prices[prices.length - 1];
        double percentB = (currentPrice - lowerBand) / (upperBand - lowerBand) * 100;

        return new BollingerBandsResult(
                round(upperBand),
                round(middleBand),
                round(lowerBand),
                round(bandwidth),
                round(percentB),
                period,
                stdDevMultiplier
        );
    }

    public BollingerBandsResult calculateBollingerBands(String symbol) {
        return calculateBollingerBands(symbol, DEFAULT_BOLLINGER_PERIOD, DEFAULT_BOLLINGER_STD_DEV);
    }

    /**
     * Simple Moving Average (SMA) hesaplama
     */
    public Double calculateSMA(String symbol, int period) {
        List<BigDecimal> closePrices = getClosePrices(symbol, period);
        
        if (closePrices.size() < period) {
            log.warn("SMA hesaplaması için yeterli veri yok: {}", symbol);
            return null;
        }

        double sum = closePrices.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
        
        return round(sum / period);
    }

    /**
     * Exponential Moving Average (EMA) hesaplama
     */
    public Double calculateEMA(String symbol, int period) {
        List<BigDecimal> closePrices = getClosePrices(symbol, period * 2);
        
        if (closePrices.size() < period) {
            log.warn("EMA hesaplaması için yeterli veri yok: {}", symbol);
            return null;
        }

        double[] prices = closePrices.stream().mapToDouble(BigDecimal::doubleValue).toArray();
        return round(calculateEMAFromPrices(prices, period));
    }

    /**
     * Stochastic Oscillator hesaplama
     * %K = (Current Close - Lowest Low) / (Highest High - Lowest Low) * 100
     * %D = 3-period SMA of %K
     */
    public StochasticResult calculateStochastic(String symbol, int kPeriod, int dPeriod) {
        List<PriceHistory> priceHistory = getPriceHistory(symbol, kPeriod + dPeriod);
        
        if (priceHistory.size() < kPeriod) {
            log.warn("Stochastic hesaplaması için yeterli veri yok: {}", symbol);
            return null;
        }

        List<Double> kValues = new ArrayList<>();
        
        for (int i = kPeriod - 1; i < priceHistory.size(); i++) {
            double highestHigh = Double.MIN_VALUE;
            double lowestLow = Double.MAX_VALUE;
            
            for (int j = i - kPeriod + 1; j <= i; j++) {
                PriceHistory ph = priceHistory.get(j);
                highestHigh = Math.max(highestHigh, ph.getHighPrice().doubleValue());
                lowestLow = Math.min(lowestLow, ph.getLowPrice().doubleValue());
            }
            
            double currentClose = priceHistory.get(i).getClosePrice().doubleValue();
            double range = highestHigh - lowestLow;
            double k = range == 0 ? 50.0 : ((currentClose - lowestLow) / range) * 100;
            kValues.add(k);
        }

        if (kValues.isEmpty()) {
            return null;
        }

        double currentK = kValues.get(kValues.size() - 1);
        
        // %D hesaplama (SMA of K)
        double sumD = 0;
        int count = Math.min(dPeriod, kValues.size());
        for (int i = kValues.size() - count; i < kValues.size(); i++) {
            sumD += kValues.get(i);
        }
        double currentD = sumD / count;

        // Oversold/Overbought durumu
        String signal;
        if (currentK < 20 && currentD < 20) {
            signal = "OVERSOLD";
        } else if (currentK > 80 && currentD > 80) {
            signal = "OVERBOUGHT";
        } else if (currentK > currentD) {
            signal = "BULLISH";
        } else if (currentK < currentD) {
            signal = "BEARISH";
        } else {
            signal = "NEUTRAL";
        }

        return new StochasticResult(
                round(currentK),
                round(currentD),
                kPeriod,
                dPeriod,
                signal
        );
    }

    public StochasticResult calculateStochastic(String symbol) {
        return calculateStochastic(symbol, 14, 3);
    }

    public Double calculateATR(String symbol, int period) {
        List<PriceHistory> history = getPriceHistory(symbol, period + 1);
        if (history.size() < period + 1) {
            return null;
        }

        double totalTrueRange = 0;
        for (int i = 1; i < history.size(); i++) {
            PriceHistory current = history.get(i);
            double high = safeHigh(current);
            double low = safeLow(current);
            double previousClose = history.get(i - 1).getClosePrice().doubleValue();
            totalTrueRange += Math.max(high - low, Math.max(Math.abs(high - previousClose), Math.abs(low - previousClose)));
        }
        return round(totalTrueRange / period);
    }

    public Double calculateADX(String symbol, int period) {
        List<PriceHistory> history = getPriceHistory(symbol, period + 1);
        if (history.size() < period + 1) {
            return null;
        }

        double positiveDm = 0;
        double negativeDm = 0;
        double trueRangeSum = 0;
        for (int i = 1; i < history.size(); i++) {
            PriceHistory current = history.get(i);
            PriceHistory previous = history.get(i - 1);
            double upMove = safeHigh(current) - safeHigh(previous);
            double downMove = safeLow(previous) - safeLow(current);
            if (upMove > downMove && upMove > 0) {
                positiveDm += upMove;
            }
            if (downMove > upMove && downMove > 0) {
                negativeDm += downMove;
            }
            double previousClose = previous.getClosePrice().doubleValue();
            trueRangeSum += Math.max(
                safeHigh(current) - safeLow(current),
                Math.max(Math.abs(safeHigh(current) - previousClose), Math.abs(safeLow(current) - previousClose))
            );
        }
        if (trueRangeSum == 0) {
            return 0.0;
        }
        double positiveDi = 100 * (positiveDm / trueRangeSum);
        double negativeDi = 100 * (negativeDm / trueRangeSum);
        double denominator = positiveDi + negativeDi;
        return denominator == 0 ? 0.0 : round(100 * Math.abs(positiveDi - negativeDi) / denominator);
    }

    public Long calculateOBV(String symbol, int limit) {
        List<PriceHistory> history = getPriceHistory(symbol, limit);
        if (history.size() < 2) {
            return null;
        }

        long obv = 0L;
        for (int i = 1; i < history.size(); i++) {
            long volume = history.get(i).getVolume() != null ? history.get(i).getVolume() : 0L;
            int comparison = history.get(i).getClosePrice().compareTo(history.get(i - 1).getClosePrice());
            if (comparison > 0) {
                obv += volume;
            } else if (comparison < 0) {
                obv -= volume;
            }
        }
        return obv;
    }

    public Double calculateVWAP(String symbol, int period) {
        List<PriceHistory> history = getPriceHistory(symbol, period);
        if (history.isEmpty()) {
            return null;
        }

        double priceVolume = 0;
        double volumeSum = 0;
        for (PriceHistory item : history) {
            long volume = item.getVolume() != null ? item.getVolume() : 0L;
            priceVolume += typicalPrice(item) * volume;
            volumeSum += volume;
        }
        return volumeSum == 0 ? null : round(priceVolume / volumeSum);
    }

    public Double calculateCCI(String symbol, int period) {
        List<PriceHistory> history = getPriceHistory(symbol, period);
        if (history.size() < period) {
            return null;
        }

        double[] typicalPrices = history.stream().mapToDouble(this::typicalPrice).toArray();
        double sma = java.util.Arrays.stream(typicalPrices).average().orElse(0);
        double meanDeviation = java.util.Arrays.stream(typicalPrices)
            .map(value -> Math.abs(value - sma))
            .average()
            .orElse(0);
        return meanDeviation == 0 ? 0.0 : round((typicalPrices[typicalPrices.length - 1] - sma) / (0.015 * meanDeviation));
    }

    public Double calculateMFI(String symbol, int period) {
        List<PriceHistory> history = getPriceHistory(symbol, period + 1);
        if (history.size() < period + 1) {
            return null;
        }

        double positiveFlow = 0;
        double negativeFlow = 0;
        for (int i = 1; i < history.size(); i++) {
            double currentTypical = typicalPrice(history.get(i));
            double previousTypical = typicalPrice(history.get(i - 1));
            long volume = history.get(i).getVolume() != null ? history.get(i).getVolume() : 0L;
            double moneyFlow = currentTypical * volume;
            if (currentTypical > previousTypical) {
                positiveFlow += moneyFlow;
            } else if (currentTypical < previousTypical) {
                negativeFlow += moneyFlow;
            }
        }
        if (negativeFlow == 0) {
            return positiveFlow == 0 ? 50.0 : 100.0;
        }
        double ratio = positiveFlow / negativeFlow;
        return round(100 - (100 / (1 + ratio)));
    }

    public Double calculateWilliamsR(String symbol, int period) {
        List<PriceHistory> history = getPriceHistory(symbol, period);
        if (history.size() < period) {
            return null;
        }

        double highestHigh = history.stream().mapToDouble(this::safeHigh).max().orElse(0);
        double lowestLow = history.stream().mapToDouble(this::safeLow).min().orElse(0);
        double close = history.get(history.size() - 1).getClosePrice().doubleValue();
        double range = highestHigh - lowestLow;
        return range == 0 ? 0.0 : round(((highestHigh - close) / range) * -100);
    }

    /**
     * Tüm göstergeleri tek seferde hesapla
     */
    public TechnicalIndicatorsResult calculateAllIndicators(String symbol) {
        Double rsi = calculateRSI(symbol, DEFAULT_RSI_PERIOD);
        MACDResult macd = calculateMACD(symbol);
        BollingerBandsResult bollinger = calculateBollingerBands(symbol);
        Double sma50 = calculateSMA(symbol, 50);
        Double sma200 = calculateSMA(symbol, 200);
        Double ema20 = calculateEMA(symbol, 20);
        StochasticResult stochastic = calculateStochastic(symbol);
        Double atr14 = calculateATR(symbol, 14);
        Double adx14 = calculateADX(symbol, 14);
        Long obv = calculateOBV(symbol, 90);
        Double vwap20 = calculateVWAP(symbol, 20);
        Double cci20 = calculateCCI(symbol, 20);
        Double mfi14 = calculateMFI(symbol, 14);
        Double williamsR14 = calculateWilliamsR(symbol, 14);
        String dataQuality = evaluateDataQuality(symbol);

        // Genel sinyal değerlendirmesi
        String overallSignal = evaluateOverallSignal(rsi, macd, stochastic);

        return new TechnicalIndicatorsResult(
                symbol,
                rsi,
                macd,
                bollinger,
                sma50,
                sma200,
                ema20,
                stochastic,
                atr14,
                adx14,
                obv,
                vwap20,
                cci20,
                mfi14,
                williamsR14,
                dataQuality,
                overallSignal
        );
    }

    // ===================== HELPER METHODS =====================

    private List<BigDecimal> getClosePrices(String symbol, int limit) {
        Optional<Instrument> instrumentOpt = instrumentRepository.findBySymbol(symbol);
        if (instrumentOpt.isEmpty()) {
            return List.of();
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(limit * 2); // Buffer için 2x

        List<PriceHistory> history = priceHistoryRepository
                .findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                        instrumentOpt.get().getId(), startDate, endDate);

        // CRITICAL FIX: Get the LAST N prices (most recent), not FIRST N (oldest)
        // Technical indicators need the most recent data
        int size = history.size();
        if (size <= limit) {
            return history.stream()
                    .map(PriceHistory::getClosePrice)
                    .toList();
        }
        
        // Skip older records, take last 'limit' records (already in ASC order)
        return history.stream()
                .skip(size - limit)
                .map(PriceHistory::getClosePrice)
                .toList();
    }

    private List<PriceHistory> getPriceHistory(String symbol, int limit) {
        Optional<Instrument> instrumentOpt = instrumentRepository.findBySymbol(symbol);
        if (instrumentOpt.isEmpty()) {
            return List.of();
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(limit * 2);

        List<PriceHistory> fullHistory = priceHistoryRepository
                .findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                        instrumentOpt.get().getId(), startDate, endDate);

        // CRITICAL FIX: Get the LAST N records (most recent), not FIRST N
        int size = fullHistory.size();
        if (size <= limit) {
            return fullHistory;
        }
        
        return fullHistory.stream()
                .skip(size - limit)
                .toList();
    }

    private double calculateEMAFromPrices(double[] prices, int period) {
        if (prices.length < period) {
            return prices[prices.length - 1];
        }

        double multiplier = 2.0 / (period + 1);
        
        // İlk EMA = SMA
        double ema = 0;
        for (int i = 0; i < period; i++) {
            ema += prices[i];
        }
        ema /= period;

        // Sonraki günler için EMA
        for (int i = period; i < prices.length; i++) {
            ema = (prices[i] - ema) * multiplier + ema;
        }

        return ema;
    }

    private double calculateEMAFromDoubles(List<Double> values, int period) {
        if (values.size() < period) {
            return values.get(values.size() - 1);
        }

        double multiplier = 2.0 / (period + 1);
        
        double ema = values.stream().limit(period).mapToDouble(Double::doubleValue).average().orElse(0);

        for (int i = period; i < values.size(); i++) {
            ema = (values.get(i) - ema) * multiplier + ema;
        }

        return ema;
    }

    private String evaluateOverallSignal(Double rsi, MACDResult macd, StochasticResult stochastic) {
        int bullishSignals = 0;
        int bearishSignals = 0;

        // RSI değerlendirmesi
        if (rsi != null) {
            if (rsi < 30) bullishSignals++; // Oversold = alım fırsatı
            else if (rsi > 70) bearishSignals++; // Overbought = satış fırsatı
        }

        // MACD değerlendirmesi
        if (macd != null) {
            if (macd.histogram() > 0) bullishSignals++;
            else if (macd.histogram() < 0) bearishSignals++;
        }

        // Stochastic değerlendirmesi
        if (stochastic != null) {
            if ("BULLISH".equals(stochastic.signal()) || "OVERSOLD".equals(stochastic.signal())) {
                bullishSignals++;
            } else if ("BEARISH".equals(stochastic.signal()) || "OVERBOUGHT".equals(stochastic.signal())) {
                bearishSignals++;
            }
        }

        if (bullishSignals > bearishSignals) {
            return "BULLISH";
        } else if (bearishSignals > bullishSignals) {
            return "BEARISH";
        }
        return "NEUTRAL";
    }

    private String evaluateDataQuality(String symbol) {
        int sampleSize = getPriceHistory(symbol, 220).size();
        if (sampleSize >= 200) {
            return "COMPLETE";
        }
        if (sampleSize >= 50) {
            return "PARTIAL";
        }
        if (sampleSize > 0) {
            return "LIMITED";
        }
        return "NO_DATA";
    }

    private double typicalPrice(PriceHistory item) {
        return (safeHigh(item) + safeLow(item) + item.getClosePrice().doubleValue()) / 3;
    }

    private double safeHigh(PriceHistory item) {
        return item.getHighPrice() != null ? item.getHighPrice().doubleValue() : item.getClosePrice().doubleValue();
    }

    private double safeLow(PriceHistory item) {
        return item.getLowPrice() != null ? item.getLowPrice().doubleValue() : item.getClosePrice().doubleValue();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
