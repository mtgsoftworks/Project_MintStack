package com.mintstack.finance.service;

import com.mintstack.finance.dto.cache.CurrencyRateData;
import com.mintstack.finance.dto.cache.IndexData;
import com.mintstack.finance.dto.cache.SimulationConfigData;
import com.mintstack.finance.dto.cache.StockPriceData;
import com.mintstack.finance.entity.SimulationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCacheService {

    private static final String STOCK_PRICES_KEY = "simulation:stock-prices";
    private static final String STOCK_SYMBOLS_KEY = "simulation:stock-symbols";
    private static final String CURRENCY_RATES_KEY = "simulation:currency-rates";
    private static final String CURRENCY_CODES_KEY = "simulation:currency-codes";
    private static final String INDEX_VALUES_KEY = "simulation:index-values";
    private static final String INDEX_SYMBOLS_KEY = "simulation:index-symbols";
    private static final String SIMULATION_CONFIG_KEY = "simulation:config";

    private final RedisTemplate<String, Object> redisTemplate;

    private boolean redisAvailable = true;

    public void saveStockPrice(String symbol, StockPriceData data) {
        if (!checkRedisConnection()) return;
        try {
            data.setTimestamp(LocalDateTime.now());
            redisTemplate.opsForHash().put(STOCK_PRICES_KEY, symbol, data);
            redisTemplate.opsForSet().add(STOCK_SYMBOLS_KEY, symbol);
            log.debug("Saved stock price to Redis: {}", symbol);
        } catch (Exception e) {
            log.error("Failed to save stock price to Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<StockPriceData> getStockPrice(String symbol) {
        if (!checkRedisConnection()) return Optional.empty();
        try {
            Object data = redisTemplate.opsForHash().get(STOCK_PRICES_KEY, symbol);
            if (data instanceof StockPriceData) {
                return Optional.of((StockPriceData) data);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get stock price from Redis: {}", e.getMessage());
            markRedisUnavailable();
            return Optional.empty();
        }
    }

    public Map<String, StockPriceData> getAllStockPrices() {
        Map<String, StockPriceData> result = new HashMap<>();
        if (!checkRedisConnection()) return result;
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(STOCK_PRICES_KEY);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                if (entry.getValue() instanceof StockPriceData) {
                    result.put(entry.getKey().toString(), (StockPriceData) entry.getValue());
                }
            }
        } catch (Exception e) {
            log.error("Failed to get all stock prices from Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
        return result;
    }

    public void deleteStockPrice(String symbol) {
        if (!checkRedisConnection()) return;
        try {
            redisTemplate.opsForHash().delete(STOCK_PRICES_KEY, symbol);
            redisTemplate.opsForSet().remove(STOCK_SYMBOLS_KEY, symbol);
        } catch (Exception e) {
            log.error("Failed to delete stock price from Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
    }

    public void saveCurrencyRate(String code, CurrencyRateData data) {
        if (!checkRedisConnection()) return;
        try {
            data.setTimestamp(LocalDateTime.now());
            redisTemplate.opsForHash().put(CURRENCY_RATES_KEY, code, data);
            redisTemplate.opsForSet().add(CURRENCY_CODES_KEY, code);
            log.debug("Saved currency rate to Redis: {}", code);
        } catch (Exception e) {
            log.error("Failed to save currency rate to Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<CurrencyRateData> getCurrencyRate(String code) {
        if (!checkRedisConnection()) return Optional.empty();
        try {
            Object data = redisTemplate.opsForHash().get(CURRENCY_RATES_KEY, code);
            if (data instanceof CurrencyRateData) {
                return Optional.of((CurrencyRateData) data);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get currency rate from Redis: {}", e.getMessage());
            markRedisUnavailable();
            return Optional.empty();
        }
    }

    public Map<String, CurrencyRateData> getAllCurrencyRates() {
        Map<String, CurrencyRateData> result = new HashMap<>();
        if (!checkRedisConnection()) return result;
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(CURRENCY_RATES_KEY);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                if (entry.getValue() instanceof CurrencyRateData) {
                    result.put(entry.getKey().toString(), (CurrencyRateData) entry.getValue());
                }
            }
        } catch (Exception e) {
            log.error("Failed to get all currency rates from Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
        return result;
    }

    public void saveIndexValue(String symbol, IndexData data) {
        if (!checkRedisConnection()) return;
        try {
            data.setTimestamp(LocalDateTime.now());
            redisTemplate.opsForHash().put(INDEX_VALUES_KEY, symbol, data);
            redisTemplate.opsForSet().add(INDEX_SYMBOLS_KEY, symbol);
            log.debug("Saved index value to Redis: {}", symbol);
        } catch (Exception e) {
            log.error("Failed to save index value to Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<IndexData> getIndexValue(String symbol) {
        if (!checkRedisConnection()) return Optional.empty();
        try {
            Object data = redisTemplate.opsForHash().get(INDEX_VALUES_KEY, symbol);
            if (data instanceof IndexData) {
                return Optional.of((IndexData) data);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get index value from Redis: {}", e.getMessage());
            markRedisUnavailable();
            return Optional.empty();
        }
    }

    public Map<String, IndexData> getAllIndexValues() {
        Map<String, IndexData> result = new HashMap<>();
        if (!checkRedisConnection()) return result;
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(INDEX_VALUES_KEY);
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                if (entry.getValue() instanceof IndexData) {
                    result.put(entry.getKey().toString(), (IndexData) entry.getValue());
                }
            }
        } catch (Exception e) {
            log.error("Failed to get all index values from Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
        return result;
    }

    public void saveSimulationConfig(SimulationConfig config) {
        if (!checkRedisConnection()) return;
        try {
            SimulationConfigData data = SimulationConfigData.fromEntity(config);
            redisTemplate.opsForValue().set(SIMULATION_CONFIG_KEY, data);
            log.debug("Saved simulation config to Redis");
        } catch (Exception e) {
            log.error("Failed to save simulation config to Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<SimulationConfig> getSimulationConfig() {
        if (!checkRedisConnection()) return Optional.empty();
        try {
            Object data = redisTemplate.opsForValue().get(SIMULATION_CONFIG_KEY);
            if (data instanceof SimulationConfigData) {
                return Optional.of(((SimulationConfigData) data).toEntity());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get simulation config from Redis: {}", e.getMessage());
            markRedisUnavailable();
            return Optional.empty();
        }
    }

    public void saveAllStockPrices(Map<String, StockPriceData> prices) {
        if (!checkRedisConnection() || prices == null || prices.isEmpty()) return;
        try {
            Map<String, Object> map = new HashMap<>();
            LocalDateTime now = LocalDateTime.now();
            for (Map.Entry<String, StockPriceData> entry : prices.entrySet()) {
                StockPriceData data = entry.getValue();
                data.setTimestamp(now);
                map.put(entry.getKey(), data);
                redisTemplate.opsForSet().add(STOCK_SYMBOLS_KEY, entry.getKey());
            }
            redisTemplate.opsForHash().putAll(STOCK_PRICES_KEY, map);
            log.info("Saved {} stock prices to Redis", prices.size());
        } catch (Exception e) {
            log.error("Failed to save all stock prices to Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
    }

    public void saveAllCurrencyRates(Map<String, CurrencyRateData> rates) {
        if (!checkRedisConnection() || rates == null || rates.isEmpty()) return;
        try {
            Map<String, Object> map = new HashMap<>();
            LocalDateTime now = LocalDateTime.now();
            for (Map.Entry<String, CurrencyRateData> entry : rates.entrySet()) {
                CurrencyRateData data = entry.getValue();
                data.setTimestamp(now);
                map.put(entry.getKey(), data);
                redisTemplate.opsForSet().add(CURRENCY_CODES_KEY, entry.getKey());
            }
            redisTemplate.opsForHash().putAll(CURRENCY_RATES_KEY, map);
            log.info("Saved {} currency rates to Redis", rates.size());
        } catch (Exception e) {
            log.error("Failed to save all currency rates to Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
    }

    public void saveAllIndexValues(Map<String, IndexData> indices) {
        if (!checkRedisConnection() || indices == null || indices.isEmpty()) return;
        try {
            Map<String, Object> map = new HashMap<>();
            LocalDateTime now = LocalDateTime.now();
            for (Map.Entry<String, IndexData> entry : indices.entrySet()) {
                IndexData data = entry.getValue();
                data.setTimestamp(now);
                map.put(entry.getKey(), data);
                redisTemplate.opsForSet().add(INDEX_SYMBOLS_KEY, entry.getKey());
            }
            redisTemplate.opsForHash().putAll(INDEX_VALUES_KEY, map);
            log.info("Saved {} index values to Redis", indices.size());
        } catch (Exception e) {
            log.error("Failed to save all index values to Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
    }

    public void clearAllCache() {
        if (!checkRedisConnection()) return;
        try {
            redisTemplate.delete(STOCK_PRICES_KEY);
            redisTemplate.delete(STOCK_SYMBOLS_KEY);
            redisTemplate.delete(CURRENCY_RATES_KEY);
            redisTemplate.delete(CURRENCY_CODES_KEY);
            redisTemplate.delete(INDEX_VALUES_KEY);
            redisTemplate.delete(INDEX_SYMBOLS_KEY);
            redisTemplate.delete(SIMULATION_CONFIG_KEY);
            log.info("Cleared all simulation cache from Redis");
        } catch (Exception e) {
            log.error("Failed to clear cache from Redis: {}", e.getMessage());
            markRedisUnavailable();
        }
    }

    public boolean isRedisAvailable() {
        return redisAvailable;
    }

    private boolean checkRedisConnection() {
        if (!redisAvailable) {
            log.debug("Redis is marked as unavailable, skipping operation");
            return false;
        }
        return true;
    }

    private void markRedisUnavailable() {
        this.redisAvailable = false;
        log.warn("Redis marked as unavailable, will use in-memory fallback");
    }

    public void resetRedisAvailability() {
        this.redisAvailable = true;
        log.info("Redis availability reset to true");
    }
}
