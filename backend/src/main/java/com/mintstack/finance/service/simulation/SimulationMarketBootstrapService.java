package com.mintstack.finance.service.simulation;

import com.mintstack.finance.dto.cache.CurrencyRateData;
import com.mintstack.finance.dto.cache.IndexData;
import com.mintstack.finance.dto.cache.StockPriceData;
import com.mintstack.finance.service.PriceCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimulationMarketBootstrapService {

    private final PriceCacheService priceCacheService;

    public void initializeCaches(
        Map<String, SimulatedStock> stockCache,
        Map<String, SimulatedCurrency> currencyCache,
        Map<String, SimulatedIndex> indexCache,
        Map<String, SimulatedCrypto> cryptoCache
    ) {
        stockCache.clear();
        currencyCache.clear();
        indexCache.clear();
        cryptoCache.clear();

        boolean loadedFromRedis = loadFromRedis(stockCache, currencyCache, indexCache);

        if (!loadedFromRedis) {
            log.info("No data in Redis, initializing with default values");
            initializeBistStocks(stockCache);
            initializeCurrencies(currencyCache);
            initializeIndices(indexCache);
            initializeCryptos(cryptoCache);
        }

        ensureBist100Alias(indexCache);

        log.info(
            "Simulation market data initialized: {} stocks, {} currencies, {} indices, {} cryptos (from Redis: {})",
            stockCache.size(),
            currencyCache.size(),
            indexCache.size(),
            cryptoCache.size(),
            loadedFromRedis
        );
    }

    private boolean loadFromRedis(
        Map<String, SimulatedStock> stockCache,
        Map<String, SimulatedCurrency> currencyCache,
        Map<String, SimulatedIndex> indexCache
    ) {
        if (!priceCacheService.isRedisAvailable()) {
            log.info("Redis not available, using in-memory defaults");
            return false;
        }

        try {
            Map<String, StockPriceData> stockPrices = priceCacheService.getAllStockPrices();
            Map<String, CurrencyRateData> currencyRates = priceCacheService.getAllCurrencyRates();
            Map<String, IndexData> indexValues = priceCacheService.getAllIndexValues();

            if (stockPrices.isEmpty() && currencyRates.isEmpty() && indexValues.isEmpty()) {
                log.info("Redis caches are empty");
                return false;
            }

            for (Map.Entry<String, StockPriceData> entry : stockPrices.entrySet()) {
                StockPriceData data = entry.getValue();
                SimulatedStock stock = new SimulatedStock(
                    data.getName(),
                    "BIST",
                    data.getPrice().doubleValue(),
                    0.02,
                    data.getSector()
                );
                stock.setNewDayPreviousClose();
                if (data.getPreviousClose() != null) {
                    stock.setPreviousClose(data.getPreviousClose());
                }
                stockCache.put(entry.getKey(), stock);
            }

            for (Map.Entry<String, CurrencyRateData> entry : currencyRates.entrySet()) {
                CurrencyRateData data = entry.getValue();
                SimulatedCurrency currency = new SimulatedCurrency(
                    data.getName(),
                    data.getBuyingRate().doubleValue(),
                    data.getSellingRate().doubleValue(),
                    0.01
                );
                currencyCache.put(entry.getKey(), currency);
            }

            for (Map.Entry<String, IndexData> entry : indexValues.entrySet()) {
                IndexData data = entry.getValue();
                SimulatedIndex index = new SimulatedIndex(
                    data.getName(),
                    data.getValue().doubleValue(),
                    0.015
                );
                index.setNewDayPreviousClose();
                if (data.getPreviousClose() != null) {
                    index.setPreviousClose(data.getPreviousClose());
                }
                indexCache.put(entry.getKey(), index);
            }

            log.info(
                "Loaded {} stocks, {} currencies, {} indices from Redis",
                stockPrices.size(),
                currencyRates.size(),
                indexValues.size()
            );
            return true;
        } catch (Exception error) {
            log.error("Failed to load from Redis: {}", error.getMessage());
            return false;
        }
    }

    private void initializeBistStocks(Map<String, SimulatedStock> stockCache) {
        stockCache.put("THYAO", new SimulatedStock("Turk Hava Yollari", "BIST", 285.50, 0.025, "HAVACILIK"));
        stockCache.put("GARAN", new SimulatedStock("Garanti BBVA", "BIST", 125.80, 0.022, "BANKA"));
        stockCache.put("AKBNK", new SimulatedStock("Akbank", "BIST", 58.40, 0.020, "BANKA"));
        stockCache.put("EREGL", new SimulatedStock("Eregli Demir Celik", "BIST", 52.30, 0.028, "METAL"));
        stockCache.put("SISE", new SimulatedStock("Sisecam", "BIST", 48.75, 0.024, "KIMYA"));
        stockCache.put("KCHOL", new SimulatedStock("Koc Holding", "BIST", 195.20, 0.018, "HOLDING"));
        stockCache.put("SAHOL", new SimulatedStock("Sabanci Holding", "BIST", 85.60, 0.019, "HOLDING"));
        stockCache.put("TUPRS", new SimulatedStock("Tupras", "BIST", 165.40, 0.023, "PETROL"));
        stockCache.put("ASELS", new SimulatedStock("Aselsan", "BIST", 68.90, 0.030, "TEKNOLOJI"));
        stockCache.put("BIMAS", new SimulatedStock("BIM Magazalar", "BIST", 385.00, 0.015, "PERAKENDE"));
        stockCache.put("TCELL", new SimulatedStock("Turkcell", "BIST", 95.25, 0.021, "TELEKOM"));
        stockCache.put("PGSUS", new SimulatedStock("Pegasus", "BIST", 920.50, 0.035, "HAVACILIK"));
        stockCache.put("SASA", new SimulatedStock("SASA Polyester", "BIST", 72.80, 0.040, "KIMYA"));
        stockCache.put("TOASO", new SimulatedStock("Tofas Otomobil", "BIST", 245.30, 0.022, "OTOMOTIV"));
        stockCache.put("FROTO", new SimulatedStock("Ford Otosan", "BIST", 1150.00, 0.020, "OTOMOTIV"));
        stockCache.put("YKBNK", new SimulatedStock("Yapi Kredi", "BIST", 32.45, 0.023, "BANKA"));
        stockCache.put("HALKB", new SimulatedStock("Halkbank", "BIST", 18.90, 0.028, "BANKA"));
        stockCache.put("VAKBN", new SimulatedStock("Vakifbank", "BIST", 22.15, 0.026, "BANKA"));
        stockCache.put("ISCTR", new SimulatedStock("Is Bankasi C", "BIST", 15.85, 0.021, "BANKA"));
        stockCache.put("KOZAL", new SimulatedStock("Koza Altin", "BIST", 145.60, 0.032, "MADEN"));
        stockCache.put("EKGYO", new SimulatedStock("Emlak Konut GYO", "BIST", 12.45, 0.025, "GYO"));
        stockCache.put("ENKAI", new SimulatedStock("Enka Insaat", "BIST", 42.80, 0.018, "INSAAT"));
        stockCache.put("PETKM", new SimulatedStock("Petkim", "BIST", 28.35, 0.029, "PETROKIMYA"));
        stockCache.put("TTKOM", new SimulatedStock("Turk Telekom", "BIST", 48.90, 0.019, "TELEKOM"));
        stockCache.put("TAVHL", new SimulatedStock("TAV Havalimanlari", "BIST", 98.50, 0.024, "INSAAT"));
        stockCache.put("XU100", new SimulatedStock("BIST 100", "BIST", 9850.00, 0.015, "ENDEKS"));
    }

    private void initializeCurrencies(Map<String, SimulatedCurrency> currencyCache) {
        currencyCache.put("USD", new SimulatedCurrency("ABD Dolari", 38.42, 38.58, 0.008));
        currencyCache.put("EUR", new SimulatedCurrency("Euro", 40.15, 40.38, 0.009));
        currencyCache.put("GBP", new SimulatedCurrency("Ingiliz Sterlini", 47.75, 48.05, 0.010));
        currencyCache.put("CHF", new SimulatedCurrency("Isvicre Frangi", 43.45, 43.72, 0.007));
        currencyCache.put("JPY", new SimulatedCurrency("Japon Yeni", 0.2445, 0.2478, 0.012));
        currencyCache.put("SAR", new SimulatedCurrency("Suudi Riyali", 10.22, 10.32, 0.005));
        currencyCache.put("AUD", new SimulatedCurrency("Avustralya Dolari", 24.28, 24.48, 0.011));
        currencyCache.put("CAD", new SimulatedCurrency("Kanada Dolari", 27.12, 27.32, 0.009));
        currencyCache.put("DKK", new SimulatedCurrency("Danimarka Kronu", 5.38, 5.42, 0.008));
        currencyCache.put("SEK", new SimulatedCurrency("Isvec Kronu", 3.52, 3.56, 0.009));
        currencyCache.put("NOK", new SimulatedCurrency("Norvec Kronu", 3.45, 3.49, 0.010));
        currencyCache.put("RUB", new SimulatedCurrency("Rus Rublesi", 0.385, 0.395, 0.015));
        currencyCache.put("CNY", new SimulatedCurrency("Cin Yuani", 5.28, 5.35, 0.006));
        currencyCache.put("KWD", new SimulatedCurrency("Kuveyt Dinari", 124.50, 125.80, 0.004));
        currencyCache.put("AED", new SimulatedCurrency("BAE Dirhemi", 10.45, 10.55, 0.005));
        currencyCache.put("BGN", new SimulatedCurrency("Bulgar Levasi", 20.55, 20.72, 0.007));
        currencyCache.put("RON", new SimulatedCurrency("Romen Leyi", 7.72, 7.82, 0.008));
        currencyCache.put("IRR", new SimulatedCurrency("Iran Riyali", 0.00091, 0.00095, 0.020));
        currencyCache.put("IQD", new SimulatedCurrency("Irak Dinari", 0.0293, 0.0298, 0.012));
        currencyCache.put("PKR", new SimulatedCurrency("Pakistan Rupisi", 0.138, 0.142, 0.014));
        currencyCache.put("QAR", new SimulatedCurrency("Katar Riyali", 10.55, 10.65, 0.005));
        currencyCache.put("XAU", new SimulatedCurrency("Altin (Ons)", 2850.00, 2865.00, 0.012));
    }

    private void initializeIndices(Map<String, SimulatedIndex> indexCache) {
        indexCache.put("XU100", new SimulatedIndex("BIST 100", 9850.00, 0.015));
        indexCache.put("XU030", new SimulatedIndex("BIST 30", 10200.00, 0.016));
        indexCache.put("XU100.IS", new SimulatedIndex("BIST 100", 9850.00, 0.015));
        indexCache.put("XUSIN", new SimulatedIndex("BIST Sinai", 8500.00, 0.014));
        indexCache.put("XBANK", new SimulatedIndex("BIST Banka", 6200.00, 0.020));
        indexCache.put("XHOLD", new SimulatedIndex("BIST Holding", 7800.00, 0.017));
        indexCache.put("XUTEK", new SimulatedIndex("BIST Teknoloji", 5400.00, 0.025));
        indexCache.put("XGIDA", new SimulatedIndex("BIST Gida", 4200.00, 0.013));
    }

    private void ensureBist100Alias(Map<String, SimulatedIndex> indexCache) {
        SimulatedIndex base = indexCache.get("XU100");
        SimulatedIndex alias = indexCache.get("XU100.IS");

        if (base == null && alias == null) {
            base = new SimulatedIndex("BIST 100", 9850.00, 0.015);
            indexCache.put("XU100", base);

            alias = new SimulatedIndex("BIST 100", 9850.00, 0.015);
            indexCache.put("XU100.IS", alias);
            return;
        }

        if (base == null && alias != null) {
            base = new SimulatedIndex(
                alias.getName(),
                alias.getCurrentValue().doubleValue(),
                alias.getBaseVolatility()
            );
            base.setPreviousClose(alias.getPreviousClose());
            indexCache.put("XU100", base);
        }

        if (alias == null && base != null) {
            alias = new SimulatedIndex(
                base.getName(),
                base.getCurrentValue().doubleValue(),
                base.getBaseVolatility()
            );
            alias.setPreviousClose(base.getPreviousClose());
            indexCache.put("XU100.IS", alias);
        }
    }

    private void initializeCryptos(Map<String, SimulatedCrypto> cryptoCache) {
        cryptoCache.put("BTC-USD", new SimulatedCrypto("Bitcoin", "BTC-USD", 95000.00, 0.05));
        cryptoCache.put("ETH-USD", new SimulatedCrypto("Ethereum", "ETH-USD", 3400.00, 0.06));
        cryptoCache.put("BNB-USD", new SimulatedCrypto("Binance Coin", "BNB-USD", 650.00, 0.07));
        cryptoCache.put("SOL-USD", new SimulatedCrypto("Solana", "SOL-USD", 180.00, 0.08));
        cryptoCache.put("XRP-USD", new SimulatedCrypto("Ripple", "XRP-USD", 2.50, 0.07));
        cryptoCache.put("DOGE-USD", new SimulatedCrypto("Dogecoin", "DOGE-USD", 0.35, 0.10));
        cryptoCache.put("ADA-USD", new SimulatedCrypto("Cardano", "ADA-USD", 0.80, 0.08));
        cryptoCache.put("AVAX-USD", new SimulatedCrypto("Avalanche", "AVAX-USD", 38.00, 0.09));
        cryptoCache.put("DOT-USD", new SimulatedCrypto("Polkadot", "DOT-USD", 7.50, 0.08));
        cryptoCache.put("LINK-USD", new SimulatedCrypto("Chainlink", "LINK-USD", 18.00, 0.08));
    }
}
