package com.mintstack.finance.service.simulation;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.entity.SimulationConfig;
import com.mintstack.finance.entity.SimulationConfig.MarketTrend;
import com.mintstack.finance.entity.SimulationConfig.VolatilityLevel;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.repository.SimulationConfigRepository;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.service.PriceUpdateService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationDataService {

    private final SimulationConfigRepository configRepository;
    private final InstrumentRepository instrumentRepository;
    private final CurrencyRateRepository currencyRateRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final UserApiConfigRepository userApiConfigRepository; // New dependency
    private final PriceUpdateService priceUpdateService;
    private final PriceSimulationEngine priceEngine;

    // Simüle edilen veriler için in-memory cache
    private final Map<String, SimulatedStock> stockCache = new ConcurrentHashMap<>();
    private final Map<String, SimulatedCurrency> currencyCache = new ConcurrentHashMap<>();
    private final Map<String, SimulatedIndex> indexCache = new ConcurrentHashMap<>();

    // ==================== TÜRK PİYASASI VERİLERİ ====================

    @Getter
    public static class SimulatedStock {
        private final String name;
        private final String exchange;
        private BigDecimal currentPrice;
        private BigDecimal previousClose;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private Long volume;
        private final double baseVolatility;
        private final String sector;
        
        // Teknik göstergeler
        private Double rsi;
        private Double macd;
        private Double bollingerUpper;
        private Double bollingerLower;
        
        // Order book depth
        private final Map<Integer, BigDecimal> bidLevels = new ConcurrentHashMap<>();
        private final Map<Integer, BigDecimal> askLevels = new ConcurrentHashMap<>();

        public SimulatedStock(String name, String exchange, double initialPrice, double baseVolatility) {
            this(name, exchange, initialPrice, baseVolatility, null);
        }

        public SimulatedStock(String name, String exchange, double initialPrice, double baseVolatility, String sector) {
            this.name = name;
            this.exchange = exchange;
            this.currentPrice = BigDecimal.valueOf(initialPrice);
            this.previousClose = BigDecimal.valueOf(initialPrice);
            this.openPrice = BigDecimal.valueOf(initialPrice);
            this.highPrice = BigDecimal.valueOf(initialPrice);
            this.lowPrice = BigDecimal.valueOf(initialPrice);
            this.baseVolatility = baseVolatility;
            this.sector = sector;
            this.volume = (long) (1000000 + Math.random() * 5000000); // 1M-6M başlangıç hacmi
            
            // Order book depth başlat
            initializeOrderBook(initialPrice);
        }

        private void initializeOrderBook(double price) {
            for (int i = 1; i <= 5; i++) {
                double spread = price * 0.001 * i; // %0.1 spread
                bidLevels.put(i, BigDecimal.valueOf(price - spread));
                askLevels.put(i, BigDecimal.valueOf(price + spread));
            }
        }

        public void updateOrderBook(BigDecimal price) {
            double p = price.doubleValue();
            for (int i = 1; i <= 5; i++) {
                double spread = p * 0.001 * i;
                bidLevels.put(i, BigDecimal.valueOf(p - spread));
                askLevels.put(i, BigDecimal.valueOf(p + spread));
            }
        }

        public void updatePrice(BigDecimal newPrice) {
            this.previousClose = this.currentPrice;
            this.currentPrice = newPrice;
            
            // OHLC güncelle
            if (this.highPrice == null || newPrice.compareTo(this.highPrice) > 0) {
                this.highPrice = newPrice;
            }
            if (this.lowPrice == null || newPrice.compareTo(this.lowPrice) < 0) {
                this.lowPrice = newPrice;
            }
            
            // Order book güncelle
            updateOrderBook(newPrice);
        }

        public void resetDailyOHLC(BigDecimal price) {
            this.openPrice = price;
            this.highPrice = price;
            this.lowPrice = price;
        }

        public BigDecimal getChangePercent() {
            if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return currentPrice.subtract(previousClose)
                    .divide(previousClose, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        public void updateVolume() {
            // Rastgele hacim değişimi (±%20)
            double change = 0.8 + Math.random() * 0.4; // 0.8 - 1.2
            this.volume = (long) (this.volume * change);
        }

        public void calculateTechnicalIndicators() {
            // RSI (görsel amaçlı)
            double change = getChangePercent().doubleValue();
            this.rsi = 50.0 + (change * 10); // Basit RSI simülasyonu
            this.rsi = Math.max(0, Math.min(100, this.rsi));

            // MACD (görsel amaçlı)
            this.macd = change * 5;

            // Bollinger Bands
            double volatility = this.baseVolatility * 100;
            this.bollingerUpper = currentPrice.doubleValue() * (1 + volatility * 0.02);
            this.bollingerLower = currentPrice.doubleValue() * (1 - volatility * 0.02);
        }
    }

    @Getter
    public static class SimulatedCurrency {
        private final String name;
        private BigDecimal buyingRate;
        private BigDecimal sellingRate;
        private final BigDecimal meanRate;
        private final double baseVolatility;
        private final double spreadPercent;

        public SimulatedCurrency(String name, double buyingRate, double sellingRate, double baseVolatility) {
            this.name = name;
            this.buyingRate = BigDecimal.valueOf(buyingRate);
            this.sellingRate = BigDecimal.valueOf(sellingRate);
            this.meanRate = BigDecimal.valueOf((buyingRate + sellingRate) / 2);
            this.baseVolatility = baseVolatility;
            this.spreadPercent = (sellingRate - buyingRate) / ((buyingRate + sellingRate) / 2);
        }

        public void updateRates(BigDecimal buying, BigDecimal selling) {
            this.buyingRate = buying;
            this.sellingRate = selling;
        }

        public BigDecimal getMidRate() {
            return buyingRate.add(sellingRate).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
        }
    }

    @Getter
    public static class SimulatedIndex {
        private final String name;
        private BigDecimal currentValue;
        private BigDecimal previousClose;
        private final double baseVolatility;

        public SimulatedIndex(String name, double initialValue, double baseVolatility) {
            this.name = name;
            this.currentValue = BigDecimal.valueOf(initialValue);
            this.previousClose = BigDecimal.valueOf(initialValue);
            this.baseVolatility = baseVolatility;
        }

        public void updateValue(BigDecimal newValue) {
            this.previousClose = this.currentValue;
            this.currentValue = newValue;
        }

        public BigDecimal getChangePercent() {
            if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return currentValue.subtract(previousClose)
                    .divide(previousClose, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    @PostConstruct
    public void initializeMarketData() {
        initializeBistStocks();
        initializeCurrencies();
        initializeIndices();
        log.info("Simulation market data initialized: {} stocks, {} currencies, {} indices",
                stockCache.size(), currencyCache.size(), indexCache.size());
    }

    private void initializeBistStocks() {
        // BIST 30 Hisseleri - Gerçekçi fiyatlar ve volatiliteler
        stockCache.put("THYAO", new SimulatedStock("Türk Hava Yolları", "BIST", 285.50, 0.025, "HAVACILIK"));
        stockCache.put("GARAN", new SimulatedStock("Garanti BBVA", "BIST", 125.80, 0.022, "BANKA"));
        stockCache.put("AKBNK", new SimulatedStock("Akbank", "BIST", 58.40, 0.020, "BANKA"));
        stockCache.put("EREGL", new SimulatedStock("Ereğli Demir Çelik", "BIST", 52.30, 0.028, "METAL"));
        stockCache.put("SISE", new SimulatedStock("Şişecam", "BIST", 48.75, 0.024, "KIMYA"));
        stockCache.put("KCHOL", new SimulatedStock("Koç Holding", "BIST", 195.20, 0.018, "HOLDING"));
        stockCache.put("SAHOL", new SimulatedStock("Sabancı Holding", "BIST", 85.60, 0.019, "HOLDING"));
        stockCache.put("TUPRS", new SimulatedStock("Tüpraş", "BIST", 165.40, 0.023, "PETROL"));
        stockCache.put("ASELS", new SimulatedStock("Aselsan", "BIST", 68.90, 0.030, "TEKNOLOJI"));
        stockCache.put("BIMAS", new SimulatedStock("BİM Mağazalar", "BIST", 385.00, 0.015, "PERAKENDE"));
        stockCache.put("TCELL", new SimulatedStock("Turkcell", "BIST", 95.25, 0.021, "TELEKOM"));
        stockCache.put("PGSUS", new SimulatedStock("Pegasus", "BIST", 920.50, 0.035, "HAVACILIK"));
        stockCache.put("SASA", new SimulatedStock("SASA Polyester", "BIST", 72.80, 0.040, "KIMYA"));
        stockCache.put("TOASO", new SimulatedStock("Tofaş Otomobil", "BIST", 245.30, 0.022, "OTOMOTIV"));
        stockCache.put("FROTO", new SimulatedStock("Ford Otosan", "BIST", 1150.00, 0.020, "OTOMOTIV"));
        stockCache.put("YKBNK", new SimulatedStock("Yapı Kredi", "BIST", 32.45, 0.023, "BANKA"));
        stockCache.put("HALKB", new SimulatedStock("Halkbank", "BIST", 18.90, 0.028, "BANKA"));
        stockCache.put("VAKBN", new SimulatedStock("Vakıfbank", "BIST", 22.15, 0.026, "BANKA"));
        stockCache.put("ISCTR", new SimulatedStock("İş Bankası C", "BIST", 15.85, 0.021, "BANKA"));
        stockCache.put("KOZAL", new SimulatedStock("Koza Altın", "BIST", 145.60, 0.032, "MADEN"));
        stockCache.put("EKGYO", new SimulatedStock("Emlak Konut GYO", "BIST", 12.45, 0.025, "GYO"));
        stockCache.put("ENKAI", new SimulatedStock("Enka İnşaat", "BIST", 42.80, 0.018, "INSaat"));
        stockCache.put("PETKM", new SimulatedStock("Petkim", "BIST", 28.35, 0.029, "PETROKIMYA"));
        stockCache.put("TTKOM", new SimulatedStock("Türk Telekom", "BIST", 48.90, 0.019, "TELEKOM"));
        stockCache.put("TAVHL", new SimulatedStock("TAV Havalimanları", "BIST", 98.50, 0.024, "INSaat"));
        stockCache.put("XU100", new SimulatedStock("BIST 100", "BIST", 9850.00, 0.015, "ENDEKS"));
    }

    private void initializeCurrencies() {
        // TCMB Döviz Kurları - Gerçekçi değerler
        currencyCache.put("USD", new SimulatedCurrency("ABD Doları", 38.42, 38.58, 0.008));
        currencyCache.put("EUR", new SimulatedCurrency("Euro", 40.15, 40.38, 0.009));
        currencyCache.put("GBP", new SimulatedCurrency("İngiliz Sterlini", 47.75, 48.05, 0.010));
        currencyCache.put("CHF", new SimulatedCurrency("İsviçre Frangı", 43.45, 43.72, 0.007));
        currencyCache.put("JPY", new SimulatedCurrency("Japon Yeni", 0.2445, 0.2478, 0.012));
        currencyCache.put("SAR", new SimulatedCurrency("Suudi Riyali", 10.22, 10.32, 0.005));
        currencyCache.put("AUD", new SimulatedCurrency("Avustralya Doları", 24.28, 24.48, 0.011));
        currencyCache.put("CAD", new SimulatedCurrency("Kanada Doları", 27.12, 27.32, 0.009));
        currencyCache.put("DKK", new SimulatedCurrency("Danimarka Kronu", 5.38, 5.42, 0.008));
        currencyCache.put("SEK", new SimulatedCurrency("İsveç Kronu", 3.52, 3.56, 0.009));
        currencyCache.put("NOK", new SimulatedCurrency("Norveç Kronu", 3.45, 3.49, 0.010));
        currencyCache.put("RUB", new SimulatedCurrency("Rus Rublesi", 0.385, 0.395, 0.015));
        currencyCache.put("CNY", new SimulatedCurrency("Çin Yuanı", 5.28, 5.35, 0.006));
        currencyCache.put("KWD", new SimulatedCurrency("Kuveyt Dinarı", 124.50, 125.80, 0.004));
        currencyCache.put("AED", new SimulatedCurrency("BAE Dirhemi", 10.45, 10.55, 0.005));
        currencyCache.put("BGN", new SimulatedCurrency("Bulgar Levası", 20.55, 20.72, 0.007));
        currencyCache.put("RON", new SimulatedCurrency("Romen Leyi", 7.72, 7.82, 0.008));
        currencyCache.put("IRR", new SimulatedCurrency("İran Riyali", 0.00091, 0.00095, 0.020));
        currencyCache.put("IQD", new SimulatedCurrency("Irak Dinarı", 0.0293, 0.0298, 0.012));
        currencyCache.put("PKR", new SimulatedCurrency("Pakistan Rupisi", 0.138, 0.142, 0.014));
        currencyCache.put("QAR", new SimulatedCurrency("Katar Riyali", 10.55, 10.65, 0.005));
        currencyCache.put("XAU", new SimulatedCurrency("Altın (Ons)", 2850.00, 2865.00, 0.012));
    }

    private void initializeIndices() {
        // BIST Endeksleri
        indexCache.put("XU100", new SimulatedIndex("BIST 100", 9850.00, 0.015));
        indexCache.put("XU030", new SimulatedIndex("BIST 30", 10200.00, 0.016));
        indexCache.put("XU100.IS", new SimulatedIndex("BIST 100", 9850.00, 0.015));
        indexCache.put("XUSIN", new SimulatedIndex("BIST Sınai", 8500.00, 0.014));
        indexCache.put("XBANK", new SimulatedIndex("BIST Banka", 6200.00, 0.020));
        indexCache.put("XHOLD", new SimulatedIndex("BIST Holding", 7800.00, 0.017));
        indexCache.put("XUTEK", new SimulatedIndex("BIST Teknoloji", 5400.00, 0.025));
        indexCache.put("XGIDA", new SimulatedIndex("BIST Gıda", 4200.00, 0.013));
    }

    // ==================== SİMÜLASYON İŞLEMLERİ ====================

    public boolean isSimulationEnabled() {
        return configRepository.getOrCreateDefault().getIsEnabled();
    }

    public SimulationConfig getConfig() {
        return configRepository.getOrCreateDefault();
    }

    @Transactional
    public SimulationConfig updateConfig(Boolean enabled, VolatilityLevel volatility,
                                          Integer updateInterval, MarketTrend trend,
                                          Boolean enableRandomEvents) {
        
        // API Key Check Logic
        if (Boolean.TRUE.equals(enabled)) {
            // Check if any API provider is active
            boolean hasActiveApi = !userApiConfigRepository.findByProviderAndIsActiveTrue(com.mintstack.finance.entity.UserApiConfig.ApiProvider.ALPHA_VANTAGE).isEmpty() ||
                                   !userApiConfigRepository.findByProviderAndIsActiveTrue(com.mintstack.finance.entity.UserApiConfig.ApiProvider.YAHOO_FINANCE).isEmpty();
            
            if (hasActiveApi) {
                throw new IllegalStateException("Cannot enable simulation while active API configurations exist. Please disable API integrations first.");
            }
        }

        SimulationConfig config = configRepository.getOrCreateDefault();

        if (enabled != null) config.setIsEnabled(enabled);
        if (volatility != null) config.setVolatilityLevel(volatility);
        if (updateInterval != null) config.setUpdateIntervalSeconds(updateInterval);
        if (trend != null) config.setMarketTrend(trend);
        if (enableRandomEvents != null) config.setEnableRandomEvents(enableRandomEvents);

        SimulationConfig saved = configRepository.save(config);
        
        if (Boolean.TRUE.equals(enabled)) {
            log.info("🎮 Simülasyon modu AKTİF - Volatilite: {}, Trend: {}", volatility, trend);
        } else if (Boolean.FALSE.equals(enabled)) {
            log.info("🎮 Simülasyon modu KAPALI");
        }
        
        return saved;
    }

    /**
     * Tüm piyasa verilerini simüle et ve güncelle
     */
    // @Transactional removed to allow partial updates and prevent long locking
    public void simulateAllPrices() {
        SimulationConfig config = getConfig();
        if (!config.getIsEnabled()) return;

        VolatilityLevel volatility = config.getVolatilityLevel();
        MarketTrend trend = config.getMarketTrend();
        boolean randomEvents = config.getEnableRandomEvents();
        int intervalSeconds = config.getUpdateIntervalSeconds();

        // Piyasa olayı kontrolü
        double eventMultiplier = priceEngine.simulateMarketEvent(randomEvents);

        // Haber etkisi simülasyonu
        String newsSector = randomEvents ? simulateNewsImpact() : null;

        // Sektör korelasyonu
        Map<String, Double> sectorMovements = simulateSectorCorrelation(volatility, trend, intervalSeconds);

        // Hisse senetlerini simüle et
        simulateStocks(volatility, trend, intervalSeconds, eventMultiplier, newsSector, sectorMovements);

        // Döviz kurlarını simüle et
        simulateCurrencies(volatility, intervalSeconds);

        // Endeksleri simüle et (hisse ortalamalarından)
        simulateIndices(volatility, trend, intervalSeconds, eventMultiplier);
    }

    /**
     * Haber etkisi simülasyonu
     * @return Etkilen sektör (null yoksa)
     */
    private String simulateNewsImpact() {
        // %5 olasılıkla haber
        if (Math.random() < 0.05) {
            String[] sectors = {"BANKA", "HAVACILIK", "TEKNOLOJI", "OTOMOTIV", "METAL", "PETROL"};
            String sector = sectors[(int) (Math.random() * sectors.length)];
            
            boolean positive = Math.random() > 0.5;
            double impact = 0.01 + Math.random() * 0.02; // %1-%3
            
            log.info("📰 {} sektörü {} haber: {}%", sector, positive ? "POZİTİF" : "NEGATİF", String.format("%.2f", impact * 100));
            
            return positive ? sector : "-" + sector;
        }
        return null;
    }

    /**
     * Sektör korelasyonu simülasyonu
     * Aynı sektördeki hisseler birlikte hareket eder
     */
    private Map<String, Double> simulateSectorCorrelation(VolatilityLevel volatility, MarketTrend trend, int intervalSeconds) {
        Map<String, Double> sectorMovements = new HashMap<>();
        
        // Her sektör için temel hareket
        for (String sector : new String[]{"BANKA", "HAVACILIK", "TEKNOLOJI", "OTOMOTIV", "METAL", "PETROL", "HOLDING"}) {
            double movement = priceEngine.simulateGBM(
                    sector,
                    BigDecimal.ONE,
                    0.01, // Sektör volatilitesi
                    volatility,
                    trend,
                    intervalSeconds
            ).doubleValue() - 1.0;
            sectorMovements.put(sector, movement);
        }
        
        return sectorMovements;
    }

    private void simulateStocks(VolatilityLevel volatility, MarketTrend trend,
                                 int intervalSeconds, double eventMultiplier, String newsSector, Map<String, Double> sectorMovements) {
        for (Map.Entry<String, SimulatedStock> entry : stockCache.entrySet()) {
            String symbol = entry.getKey();
            SimulatedStock stock = entry.getValue();

            // Sektör korelasyonu
            double sectorMovement = 0.0;
            if (stock.getSector() != null && sectorMovements.containsKey(stock.getSector())) {
                sectorMovement = sectorMovements.get(stock.getSector());
            }

            // Haber etkisi
            double newsImpact = 1.0;
            if (newsSector != null && stock.getSector() != null) {
                if (newsSector.equals(stock.getSector())) {
                    newsImpact = 1.0 + (0.01 + Math.random() * 0.02); // Pozitif haber
                } else if (newsSector.startsWith("-") && newsSector.substring(1).equals(stock.getSector())) {
                    newsImpact = 1.0 - (0.01 + Math.random() * 0.02); // Negatif haber
                }
            }

            BigDecimal basePrice = priceEngine.simulateGBM(
                    symbol,
                    stock.getCurrentPrice(),
                    stock.getBaseVolatility(),
                    volatility,
                    trend,
                    intervalSeconds
            );

            // Sektör ve haber etkisi uygula
            BigDecimal newPrice = basePrice
                    .multiply(BigDecimal.valueOf(1.0 + sectorMovement))
                    .multiply(BigDecimal.valueOf(newsImpact))
                    .multiply(BigDecimal.valueOf(eventMultiplier))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal previousClose = stock.getCurrentPrice();
            stock.updatePrice(newPrice);

            // Veritabanına kaydet ve WebSocket'e yayınla
            saveAndBroadcastStock(symbol, stock, previousClose, newPrice);
        }
    }

    private void simulateCurrencies(VolatilityLevel volatility, int intervalSeconds) {
        for (Map.Entry<String, SimulatedCurrency> entry : currencyCache.entrySet()) {
            String code = entry.getKey();
            SimulatedCurrency currency = entry.getValue();

            // XAU (Altın) için farklı işlem
            if ("XAU".equals(code)) {
                BigDecimal newMid = priceEngine.simulateGBM(
                        code,
                        currency.getMidRate(),
                        currency.getBaseVolatility(),
                        volatility,
                        MarketTrend.NEUTRAL,
                        intervalSeconds
                );
                BigDecimal[] spread = priceEngine.simulateSpread(newMid, currency.getSpreadPercent());
                currency.updateRates(spread[0], spread[1]);
            } else {
                // Döviz için mean reversion
                BigDecimal newMid = priceEngine.simulateMeanReversion(
                        code,
                        currency.getMidRate(),
                        currency.getMeanRate(),
                        currency.getBaseVolatility(),
                        volatility,
                        0.1, // reversion speed
                        intervalSeconds
                );
                BigDecimal[] spread = priceEngine.simulateSpread(newMid, currency.getSpreadPercent());
                currency.updateRates(spread[0], spread[1]);
            }

            // WebSocket'e yayınla
            priceUpdateService.broadcastCurrencyUpdate(
                    code,
                    currency.getBuyingRate(),
                    currency.getSellingRate()
            );

            // Veritabanına kaydet
            saveCurrencyRate(code, currency);
        }
    }

    private void simulateIndices(VolatilityLevel volatility, MarketTrend trend,
                                  int intervalSeconds, double eventMultiplier) {
        for (Map.Entry<String, SimulatedIndex> entry : indexCache.entrySet()) {
            String symbol = entry.getKey();
            SimulatedIndex index = entry.getValue();

            BigDecimal newValue = priceEngine.simulateGBM(
                    symbol,
                    index.getCurrentValue(),
                    index.getBaseVolatility(),
                    volatility,
                    trend,
                    intervalSeconds
            );

            if (eventMultiplier != 1.0) {
                newValue = newValue.multiply(BigDecimal.valueOf(eventMultiplier))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            index.updateValue(newValue);

            // WebSocket'e yayınla
            BigDecimal change = newValue.subtract(index.getPreviousClose());
            BigDecimal changePercent = index.getChangePercent();

            priceUpdateService.broadcastStockUpdate(
                    symbol,
                    newValue,
                    index.getPreviousClose(),
                    change,
                    changePercent
            );
        }
    }

    @Transactional
    private void saveAndBroadcastStock(String symbol, SimulatedStock stock,
                                        BigDecimal previousClose, BigDecimal newPrice) {
        // Veritabanında varsa güncelle, yoksa oluştur (SİMÜLASYON VERİSİ)
        Optional<Instrument> existing = instrumentRepository.findBySymbolAndIsSimulated(symbol, true);
        Instrument instrument;

        if (existing.isPresent()) {
            instrument = existing.get();
            instrument.setPreviousClose(previousClose);
            instrument.setCurrentPrice(newPrice);
        } else {
            instrument = Instrument.builder()
                    .symbol(symbol)
                    .name(stock.getName())
                    .type(Instrument.InstrumentType.STOCK)
                    .exchange(stock.getExchange())
                    .currency("TRY")
                    .currentPrice(newPrice)
                    .previousClose(previousClose)
                    .isActive(true)
                    .isSimulated(true) // Set simulated flag
                    .build();
        }
        instrumentRepository.save(instrument);

        // Hacmi güncelle
        stock.updateVolume();

        // Price History kaydet (günlük OHLC)
        saveDailyPriceHistory(symbol, stock);

        // WebSocket yayını
        BigDecimal change = newPrice.subtract(previousClose);
        BigDecimal changePercent = stock.getChangePercent();

        priceUpdateService.broadcastStockUpdate(
                symbol,
                newPrice,
                previousClose,
                change,
                changePercent
        );
    }

    private void saveDailyPriceHistory(String symbol, SimulatedStock stock) {
        try {
            Instrument instrument = instrumentRepository.findBySymbolAndIsSimulated(symbol, true).orElse(null);
            if (instrument == null) return;

            LocalDate today = LocalDate.now();
            
            // Check if record exists for today
            PriceHistory history = priceHistoryRepository.findByInstrumentIdAndPriceDate(instrument.getId(), today)
                    .map(existing -> {
                        // Update existing record
                        existing.setHighPrice(existing.getHighPrice().max(stock.getHighPrice()));
                        existing.setLowPrice(existing.getLowPrice().min(stock.getLowPrice()));
                        existing.setClosePrice(stock.getCurrentPrice());
                        existing.setVolume(stock.getVolume());
                        return existing;
                    })
                    .orElseGet(() -> PriceHistory.builder()
                            .instrument(instrument)
                            .priceDate(today)
                            .openPrice(stock.getOpenPrice())
                            .highPrice(stock.getHighPrice())
                            .lowPrice(stock.getLowPrice())
                            .closePrice(stock.getCurrentPrice())
                            .volume(stock.getVolume())
                            .build());

            priceHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("Price history kaydedilemedi: {}", e.getMessage());
        }
    }

    private void saveCurrencyRate(String code, SimulatedCurrency currency) {
        CurrencyRate rate = CurrencyRate.builder()
                .currencyCode(code)
                .currencyName(currency.getName())
                .buyingRate(currency.getBuyingRate())
                .sellingRate(currency.getSellingRate())
                .effectiveBuyingRate(currency.getBuyingRate())
                .effectiveSellingRate(currency.getSellingRate())
                .source(CurrencyRate.RateSource.MANUAL) // Simülasyon kaynağı
                .fetchedAt(LocalDateTime.now())
                .rateDate(LocalDateTime.now())
                .build();
        currencyRateRepository.save(rate);
    }

    /**
     * Simülasyonu sıfırla - tüm fiyatları başlangıç değerlerine döndür
     */
    public void resetSimulation() {
        priceEngine.clearState();
        initializeMarketData();
        log.info("🔄 Simülasyon sıfırlandı - tüm fiyatlar başlangıç değerlerine döndü");
    }

    // ==================== VERİ ERİŞİMİ ====================

    public Map<String, SimulatedStock> getStocks() {
        return Collections.unmodifiableMap(stockCache);
    }

    public Map<String, SimulatedCurrency> getCurrencies() {
        return Collections.unmodifiableMap(currencyCache);
    }

    public Map<String, SimulatedIndex> getIndices() {
        return Collections.unmodifiableMap(indexCache);
    }

    public SimulatedStock getStock(String symbol) {
        return stockCache.get(symbol);
    }

    public SimulatedCurrency getCurrency(String code) {
        return currencyCache.get(code);
    }

    public SimulatedIndex getIndex(String symbol) {
        return indexCache.get(symbol);
    }
}
