package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.BacktestResult;
import com.mintstack.finance.dto.response.TradeRecord;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.service.strategy.MovingAverageCrossoverStrategy;
import com.mintstack.finance.service.strategy.RSIStrategy;
import com.mintstack.finance.service.strategy.Signal;
import com.mintstack.finance.service.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Backtesting Servisi
 * Geçmiş veriler üzerinde trading stratejilerini test eder
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestingService {

    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    private final Map<String, TradingStrategy> strategies = new HashMap<>();

    {
        // Varsayılan stratejileri kaydet
        MovingAverageCrossoverStrategy ma50200 = new MovingAverageCrossoverStrategy(50, 200);
        MovingAverageCrossoverStrategy ma2050 = new MovingAverageCrossoverStrategy(20, 50);
        RSIStrategy rsi14 = new RSIStrategy(14, 30, 70);
        RSIStrategy rsi14Conservative = new RSIStrategy(14, 25, 75);

        strategies.put("MA_CROSSOVER", ma50200);
        strategies.put("MA_CROSSOVER_50_200", ma50200);
        strategies.put("MA_CROSSOVER_20_50", ma2050);
        strategies.put("RSI", rsi14);
        strategies.put("RSI_14", rsi14);
        strategies.put("RSI_CONSERVATIVE", rsi14Conservative);
    }

    /**
     * Backtest çalıştır
     * 
     * @param strategyName Strateji adı
     * @param symbol Enstrüman sembolü
     * @param startDate Başlangıç tarihi
     * @param endDate Bitiş tarihi
     * @param initialCapital Başlangıç sermayesi
     * @return Backtest sonuçları
     */
    public BacktestResult runBacktest(String strategyName, String symbol, 
                                       LocalDate startDate, LocalDate endDate,
                                       BigDecimal initialCapital) {
        log.info("Backtest başlatılıyor: {} - {} ({} - {})", 
                strategyName, symbol, startDate, endDate);

        TradingStrategy strategy = strategies.get(strategyName.toUpperCase());
        if (strategy == null) {
            log.warn("Strateji bulunamadı: {}", strategyName);
            return null;
        }

        Optional<Instrument> instrumentOpt = instrumentRepository.findBySymbol(symbol);
        if (instrumentOpt.isEmpty()) {
            log.warn("Enstrüman bulunamadı: {}", symbol);
            return null;
        }

        Instrument instrument = instrumentOpt.get();
        
        // Tarihleri genişlet (strateji için gerekli geçmiş veri)
        LocalDate extendedStartDate = startDate.minusDays(strategy.getRequiredHistoryLength());
        
        List<PriceHistory> allHistory = priceHistoryRepository
                .findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
                        instrument.getId(), extendedStartDate, endDate);

        if (allHistory.size() < strategy.getRequiredHistoryLength()) {
            log.warn("Yeterli tarihsel veri yok: {} (gerekli: {}, mevcut: {})",
                    symbol, strategy.getRequiredHistoryLength(), allHistory.size());
            return null;
        }

        // Backtest simülasyonu
        return executeBacktest(strategy, symbol, allHistory, startDate, endDate, initialCapital);
    }

    /**
     * Mevcut stratejilerin listesini döndür
     */
    public List<StrategyInfo> getAvailableStrategies() {
        List<StrategyInfo> list = new ArrayList<>();
        for (Map.Entry<String, TradingStrategy> entry : strategies.entrySet()) {
            list.add(new StrategyInfo(
                    entry.getKey(),
                    entry.getValue().getName(),
                    entry.getValue().getDescription()
            ));
        }
        return list;
    }

    // =================== PRIVATE METHODS ===================

    private BacktestResult executeBacktest(TradingStrategy strategy, String symbol,
                                            List<PriceHistory> allHistory,
                                            LocalDate startDate, LocalDate endDate,
                                            BigDecimal initialCapital) {
        
        double cash = initialCapital.doubleValue();
        int position = 0; // Sahip olunan hisse sayısı
        List<TradeRecord> trades = new ArrayList<>();
        
        // Performans metrikleri için
        double peakValue = cash;
        double maxDrawdown = 0;
        int winningTrades = 0;
        int losingTrades = 0;
        double totalProfitFromWins = 0;
        double totalLossFromLosses = 0;
        
        List<Double> dailyReturns = new ArrayList<>();
        double previousValue = cash;

        // Her gün için stratejiyi çalıştır
        for (int i = 0; i < allHistory.size(); i++) {
            PriceHistory current = allHistory.get(i);
            
            // Başlangıç tarihinden önce sadece geçmiş biriktir
            if (current.getPriceDate().isBefore(startDate)) {
                continue;
            }

            // Geçmiş veriyi al
            List<PriceHistory> historicalData = allHistory.subList(0, i + 1);
            
            // Sinyal üret
            Signal signal = strategy.generateSignal(current, historicalData, position);
            double price = current.getClosePrice().doubleValue();

            // İşlem yap
            if (signal == Signal.BUY && position == 0 && cash > price) {
                // AL - Tüm sermaye ile
                int quantity = (int) (cash / price);
                double cost = quantity * price;
                cash -= cost;
                position = quantity;
                
                trades.add(new TradeRecord(
                        current.getPriceDate(),
                        Signal.BUY,
                        BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP),
                        quantity,
                        BigDecimal.valueOf(cost).setScale(2, RoundingMode.HALF_UP)
                ));
                
            } else if (signal == Signal.SELL && position > 0) {
                // SAT - Tüm pozisyonu
                double revenue = position * price;
                double buyPrice = trades.get(trades.size() - 1).price().doubleValue();
                double profit = (price - buyPrice) * position;
                
                if (profit > 0) {
                    winningTrades++;
                    totalProfitFromWins += profit;
                } else {
                    losingTrades++;
                    totalLossFromLosses += Math.abs(profit);
                }
                
                cash += revenue;
                
                trades.add(new TradeRecord(
                        current.getPriceDate(),
                        Signal.SELL,
                        BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP),
                        position,
                        BigDecimal.valueOf(revenue).setScale(2, RoundingMode.HALF_UP)
                ));
                
                position = 0;
            }

            // Günlük portföy değeri
            double portfolioValue = cash + (position * price);
            
            // Max drawdown hesapla
            if (portfolioValue > peakValue) {
                peakValue = portfolioValue;
            }
            double drawdown = (peakValue - portfolioValue) / peakValue;
            maxDrawdown = Math.max(maxDrawdown, drawdown);
            
            // Günlük getiri
            if (previousValue > 0) {
                double dailyReturn = (portfolioValue - previousValue) / previousValue;
                dailyReturns.add(dailyReturn);
            }
            previousValue = portfolioValue;
        }

        // Son pozisyonu kapat
        double finalPrice = allHistory.get(allHistory.size() - 1).getClosePrice().doubleValue();
        double finalValue = cash + (position * finalPrice);
        
        // Metrikleri hesapla
        double totalReturn = ((finalValue - initialCapital.doubleValue()) / initialCapital.doubleValue()) * 100;
        int totalTrades = trades.size() / 2; // Her trade çifti (buy + sell) bir trade
        double winRate = totalTrades > 0 ? (double) winningTrades / (winningTrades + losingTrades) * 100 : 0;
        double sharpeRatio = calculateSharpeRatio(dailyReturns);
        double profitFactor = totalLossFromLosses > 0 ? totalProfitFromWins / totalLossFromLosses : 0;

        return new BacktestResult(
                strategy.getName(),
                strategy.getDescription(),
                symbol,
                startDate,
                endDate,
                initialCapital,
                BigDecimal.valueOf(finalValue).setScale(2, RoundingMode.HALF_UP),
                totalReturn,
                sharpeRatio,
                maxDrawdown * 100,
                totalTrades,
                winningTrades,
                losingTrades,
                winRate,
                profitFactor,
                trades
        );
    }

    private double calculateSharpeRatio(List<Double> dailyReturns) {
        if (dailyReturns.isEmpty()) {
            return 0;
        }

        double meanReturn = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = dailyReturns.stream()
                .mapToDouble(r -> Math.pow(r - meanReturn, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0) {
            return 0;
        }

        // Yıllık Sharpe (252 işlem günü)
        double annualizedReturn = meanReturn * 252;
        double annualizedStdDev = stdDev * Math.sqrt(252);
        double riskFreeRate = 0.05; // %5

        return (annualizedReturn - riskFreeRate) / annualizedStdDev;
    }

    // =================== INNER CLASSES ===================

    public record StrategyInfo(
            String key,
            String name,
            String description
    ) {}
}
