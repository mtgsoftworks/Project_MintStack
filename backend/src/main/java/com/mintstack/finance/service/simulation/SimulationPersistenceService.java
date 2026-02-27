package com.mintstack.finance.service.simulation;

import com.mintstack.finance.dto.cache.CurrencyRateData;
import com.mintstack.finance.dto.cache.IndexData;
import com.mintstack.finance.dto.cache.StockPriceData;
import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.NewsRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.service.PriceCacheService;
import com.mintstack.finance.service.PriceUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimulationPersistenceService {

    private final InstrumentRepository instrumentRepository;
    private final CurrencyRateRepository currencyRateRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final NewsRepository newsRepository;
    private final PriceUpdateService priceUpdateService;
    private final PriceCacheService priceCacheService;

    public void saveAndBroadcastIndex(String symbol, SimulatedIndex index, BigDecimal newPrice) {
        try {
            IndexData indexData = IndexData.builder()
                .symbol(symbol)
                .name(index.getName())
                .value(newPrice)
                .changePercent(index.getChangePercent())
                .previousClose(index.getPreviousClose())
                .build();
            priceCacheService.saveIndexValue(symbol, indexData);

            Instrument instrument = instrumentRepository.findBySymbolAndIsSimulated(symbol, true)
                .orElseGet(() -> Instrument.builder()
                    .symbol(symbol)
                    .name(index.getName())
                    .type(Instrument.InstrumentType.INDEX)
                    .exchange("BIST")
                    .currency("TRY")
                    .currentPrice(newPrice)
                    .previousClose(index.getPreviousClose())
                    .isActive(true)
                    .isSimulated(true)
                    .build());

            if (instrument.getId() == null) {
                instrumentRepository.save(instrument);
            }

            instrument.setPreviousClose(index.getPreviousClose());
            instrument.setCurrentPrice(newPrice);
            instrumentRepository.save(instrument);

            BigDecimal change = newPrice.subtract(index.getPreviousClose());
            BigDecimal changePercent = index.getChangePercent();

            priceUpdateService.broadcastStockUpdate(
                symbol,
                newPrice,
                index.getPreviousClose(),
                change,
                changePercent
            );
        } catch (Exception error) {
            log.error("Index kaydedilemedi {}: {}", symbol, error.getMessage());
        }
    }

    @Transactional
    public void saveAndBroadcastStock(String symbol, SimulatedStock stock, BigDecimal previousClose, BigDecimal newPrice) {
        StockPriceData stockData = StockPriceData.builder()
            .symbol(symbol)
            .name(stock.getName())
            .price(newPrice)
            .open(stock.getOpenPrice())
            .high(stock.getHighPrice())
            .low(stock.getLowPrice())
            .volume(stock.getVolume())
            .changePercent(stock.getChangePercent())
            .sector(stock.getSector())
            .previousClose(previousClose)
            .build();
        priceCacheService.saveStockPrice(symbol, stockData);

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
                .isSimulated(true)
                .build();
        }

        instrumentRepository.save(instrument);

        stock.updateVolume();

        saveDailyPriceHistory(symbol, stock);

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

    public void saveCurrencyRate(String code, SimulatedCurrency currency) {
        CurrencyRateData currencyData = CurrencyRateData.builder()
            .code(code)
            .name(currency.getName())
            .buyingRate(currency.getBuyingRate())
            .sellingRate(currency.getSellingRate())
            .midRate(currency.getMidRate())
            .build();
        priceCacheService.saveCurrencyRate(code, currencyData);

        CurrencyRate rate = CurrencyRate.builder()
            .currencyCode(code)
            .currencyName(currency.getName())
            .buyingRate(currency.getBuyingRate())
            .sellingRate(currency.getSellingRate())
            .effectiveBuyingRate(currency.getBuyingRate())
            .effectiveSellingRate(currency.getSellingRate())
            .source(CurrencyRate.RateSource.MANUAL)
            .fetchedAt(LocalDateTime.now())
            .rateDate(LocalDateTime.now())
            .build();
        currencyRateRepository.save(rate);
    }

    public void broadcastCryptoUpdate(String symbol, BigDecimal newPrice, SimulatedCrypto crypto) {
        priceUpdateService.broadcastCryptoUpdate(
            symbol,
            newPrice,
            crypto.getPreviousClose(),
            crypto.getChangePercent(),
            crypto.getHigh24h(),
            crypto.getLow24h(),
            crypto.getVolume24h()
        );
    }

    @Transactional
    public Map<String, Object> deleteSimulationData() {
        List<Instrument> simulatedInstruments = instrumentRepository.findByIsSimulated(true);
        long deletedInstrumentCount = 0L;
        long deactivatedInstrumentCount = 0L;
        if (!simulatedInstruments.isEmpty()) {
            simulatedInstruments.forEach(instrument -> instrument.setIsActive(false));
            instrumentRepository.saveAll(simulatedInstruments);
            deactivatedInstrumentCount = simulatedInstruments.size();
        }

        List<CurrencyRate> simulatedRates = currencyRateRepository.findBySource(CurrencyRate.RateSource.MANUAL);
        long currencyCount = simulatedRates.size();
        currencyRateRepository.deleteAll(simulatedRates);

        try {
            newsRepository.deleteBySourceName("Simulasyon");
        } catch (Exception error) {
            log.warn("Simulasyon haberleri silinemedi: {}", error.getMessage());
        }

        log.info("Simulation data cleanup completed: {} instrument deleted, {} instrument deactivated, {} currency rate deleted",
            deletedInstrumentCount, deactivatedInstrumentCount, currencyCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deletedInstruments", deletedInstrumentCount);
        result.put("deactivatedInstruments", deactivatedInstrumentCount);
        result.put("deletedCurrencyRates", currencyCount);
        return result;
    }

    private void saveDailyPriceHistory(String symbol, SimulatedStock stock) {
        try {
            Instrument instrument = instrumentRepository.findBySymbolAndIsSimulated(symbol, true).orElse(null);
            if (instrument == null) {
                return;
            }

            LocalDate today = LocalDate.now();

            PriceHistory history = priceHistoryRepository.findByInstrumentIdAndPriceDate(instrument.getId(), today)
                .map((existing) -> {
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
        } catch (Exception error) {
            log.warn("Price history kaydedilemedi: {}", error.getMessage());
        }
    }
}
