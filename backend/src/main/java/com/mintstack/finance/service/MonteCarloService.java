package com.mintstack.finance.service;

import com.mintstack.finance.dto.response.MonteCarloResult;
import com.mintstack.finance.dto.response.PortfolioRiskResult;
import com.mintstack.finance.dto.response.VaRResult;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Portfolio;
import com.mintstack.finance.entity.PortfolioItem;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.exception.BadRequestException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PortfolioRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class MonteCarloService {

    private final InstrumentRepository instrumentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PortfolioRepository portfolioRepository;
    private final Executor taskExecutor;

    private static final int DEFAULT_SIMULATIONS = 10000;
    private static final int TRADING_DAYS_PER_YEAR = 252;
    private static final double DEFAULT_CONFIDENCE = 0.95;
    private static final int MAX_SIMULATIONS = 10000;
    private static final int MAX_DAYS = 365;

    public MonteCarloService(
        InstrumentRepository instrumentRepository,
        PriceHistoryRepository priceHistoryRepository,
        PortfolioRepository portfolioRepository,
        @Qualifier("taskExecutor") Executor taskExecutor
    ) {
        this.instrumentRepository = instrumentRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.portfolioRepository = portfolioRepository;
        this.taskExecutor = taskExecutor;
    }

    public MonteCarloResult runSimulation(String symbol, int days, int simulations, double confidence) {
        validateSimulationParameters(days, simulations);

        log.info("Monte Carlo simulation started: {} - {} days, {} simulations", symbol, days, simulations);

        Optional<Instrument> instrumentOpt = instrumentRepository.findBySymbol(symbol);
        if (instrumentOpt.isEmpty()) {
            log.warn("Instrument not found: {}", symbol);
            return null;
        }

        Instrument instrument = instrumentOpt.get();
        BigDecimal currentPrice = instrument.getCurrentPrice();
        if (currentPrice == null) {
            log.warn("Current price not found: {}", symbol);
            return null;
        }

        double[] returns = calculateHistoricalReturns(symbol, 252);
        if (returns.length < 30) {
            log.warn("Insufficient historical data: {}", symbol);
            returns = MonteCarloStatisticsHelper.generateDefaultReturns();
        }

        double meanReturn = MonteCarloStatisticsHelper.calculateMean(returns);
        double volatility = MonteCarloStatisticsHelper.calculateStdDev(returns);
        double annualizedVolatility = volatility * Math.sqrt(TRADING_DAYS_PER_YEAR);
        double annualizedReturn = meanReturn * TRADING_DAYS_PER_YEAR;

        double[] finalPrices = runParallelSimulations(
            currentPrice.doubleValue(),
            days,
            simulations,
            annualizedReturn,
            annualizedVolatility
        );

        Arrays.sort(finalPrices);

        double mean = MonteCarloStatisticsHelper.calculateMean(finalPrices);
        int p5Index = (int) (simulations * 0.05);
        int p50Index = simulations / 2;
        int p95Index = (int) (simulations * 0.95);
        int varIndex = (int) (simulations * (1 - confidence));

        double p5 = finalPrices[p5Index];
        double p50 = finalPrices[p50Index];
        double p95 = finalPrices[p95Index];
        double var = currentPrice.doubleValue() - finalPrices[varIndex];

        double[] histogram = MonteCarloStatisticsHelper.createHistogram(finalPrices, 20);

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

    public VaRResult calculateVaR(String symbol, int days) {
        return calculateVaR(symbol, days, DEFAULT_CONFIDENCE);
    }

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
            "Monte Carlo (" + DEFAULT_SIMULATIONS + " simulations)"
        );
    }

    public PortfolioRiskResult analyzePortfolioRisk(UUID portfolioId, int days, int simulations) {
        log.info("Portfolio risk analysis: {} - {} days", portfolioId, days);

        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(portfolioId);
        if (portfolioOpt.isEmpty()) {
            log.warn("Portfolio not found: {}", portfolioId);
            return null;
        }

        Portfolio portfolio = portfolioOpt.get();
        List<PortfolioItem> items = portfolio.getItems();
        if (items == null || items.isEmpty()) {
            log.warn("Portfolio is empty: {}", portfolioId);
            return null;
        }

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

        List<String> symbols = new ArrayList<>(assetResults.keySet());
        int n = symbols.size();

        Map<String, String> sectorMap = new HashMap<>();
        for (PortfolioItem item : items) {
            String symbol = item.getInstrument().getSymbol();
            String exchange = item.getInstrument().getExchange();
            sectorMap.put(symbol, exchange != null ? exchange : "OTHER");
        }

        double[][] correlationMatrix = CorrelationMatrixHelper.calculateCorrelationMatrix(symbols, sectorMap);
        CorrelationMatrixHelper.choleskyDecomposition(correlationMatrix);

        double[] weights = new double[n];
        double[] individualVars = new double[n];
        Map<String, Double> contributions = new HashMap<>();

        for (int i = 0; i < n; i++) {
            String symbol = symbols.get(i);
            MonteCarloResult mc = assetResults.get(symbol);
            BigDecimal assetValue = assetValues.get(symbol);

            weights[i] = assetValue.doubleValue() / totalValue.doubleValue();
            individualVars[i] = mc.var().doubleValue() / mc.currentPrice().doubleValue();
            contributions.put(symbol, weights[i] * 100);
        }

        double portfolioVariance = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                portfolioVariance += weights[i] * weights[j]
                    * individualVars[i] * individualVars[j]
                    * correlationMatrix[i][j];
            }
        }

        BigDecimal portfolioVaR = BigDecimal.valueOf(Math.sqrt(portfolioVariance) * totalValue.doubleValue());

        double expectedReturn = 0;
        for (MonteCarloResult mc : assetResults.values()) {
            double ret = (mc.meanForecast().doubleValue() - mc.currentPrice().doubleValue())
                / mc.currentPrice().doubleValue();
            expectedReturn += ret;
        }
        expectedReturn = expectedReturn / assetResults.size() * 100;

        double riskFreeRate = 0.05;
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

    private double[] runParallelSimulations(
        double startPrice,
        int days,
        int simulations,
        double annualizedReturn,
        double annualizedVolatility
    ) {
        double[] results = new double[simulations];
        double dt = 1.0 / TRADING_DAYS_PER_YEAR;
        double drift = (annualizedReturn - 0.5 * annualizedVolatility * annualizedVolatility) * dt;
        double diffusion = annualizedVolatility * Math.sqrt(dt);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int batchSize = Math.max(1, simulations / availableProcessors);
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
            }, taskExecutor));
        }

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
                instrumentOpt.get().getId(),
                startDate,
                endDate
            );

        if (history.size() < 2) {
            return new double[0];
        }

        double[] returns = new double[history.size() - 1];
        for (int i = 1; i < history.size(); i++) {
            double prevPrice = history.get(i - 1).getClosePrice().doubleValue();
            double currPrice = history.get(i).getClosePrice().doubleValue();
            returns[i - 1] = Math.log(currPrice / prevPrice);
        }

        return returns;
    }

    private void validateSimulationParameters(int days, int simulations) {
        if (simulations > MAX_SIMULATIONS) {
            throw new BadRequestException(
                "Simulation count exceeds maximum limit of " + MAX_SIMULATIONS
                    + ". Requested: " + simulations
            );
        }
        if (simulations < 1) {
            throw new BadRequestException("Simulation count must be at least 1");
        }
        if (days > MAX_DAYS) {
            throw new BadRequestException(
                "Days parameter exceeds maximum limit of " + MAX_DAYS
                    + ". Requested: " + days
            );
        }
        if (days < 1) {
            throw new BadRequestException("Days parameter must be at least 1");
        }
    }
}
