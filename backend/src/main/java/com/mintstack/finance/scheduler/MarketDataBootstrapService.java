package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class MarketDataBootstrapService {

    private static final List<String> INITIAL_BIST_STOCKS = List.of(
        "THYAO", "GARAN", "AKBNK", "EREGL", "SISE",
        "KCHOL", "SAHOL", "TUPRS", "ASELS", "BIMAS",
        "TCELL", "PGSUS", "SASA", "TOASO", "FROTO"
    );

    private static final List<String> INITIAL_CRYPTO = List.of(
        "BTC-USD", "ETH-USD", "BNB-USD", "SOL-USD", "XRP-USD"
    );

    private final InstrumentRepository instrumentRepository;
    private final MarketDataProviderResolver providerResolver;

    void bootstrapStocksFromApi(
        ApiProvider preferredBistProvider,
        UserApiConfig yahooConfig,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig
    ) {
        if (!providerResolver.hasStockProviderForDataType(preferredBistProvider, yahooConfig, alphaConfig, finnhubConfig)) {
            log.warn("No active API configuration found for BIST bootstrap. Cannot bootstrap stocks.");
            return;
        }

        for (String symbol : INITIAL_BIST_STOCKS) {
            try {
                if (instrumentRepository.findBySymbolAndIsSimulated(symbol, false).isPresent()) {
                    continue;
                }

                Instrument lookupInstrument = Instrument.builder()
                    .symbol(symbol)
                    .exchange("BIST")
                    .build();

                BigDecimal price = providerResolver.fetchInstrumentPrice(
                    lookupInstrument,
                    DataType.BIST_STOCKS,
                    preferredBistProvider,
                    yahooConfig,
                    alphaConfig,
                    finnhubConfig
                );

                if (price == null) {
                    log.warn("Could not fetch price for {}, skipping bootstrap.", symbol);
                    continue;
                }

                Instrument stock = new Instrument();
                stock.setSymbol(symbol);
                stock.setName(symbol + " (BIST)");
                stock.setType(Instrument.InstrumentType.STOCK);
                stock.setExchange("BIST");
                stock.setCurrency("TRY");
                stock.setIsActive(true);
                stock.setCurrentPrice(price);
                stock.setPreviousClose(price);

                instrumentRepository.save(stock);
                log.info("Bootstrapped stock: {} - Price: {}", symbol, price);

                pauseForRateLimit(15000);
            } catch (Exception error) {
                log.error("Error bootstrapping stock {}", symbol, error);
            }
        }
    }

    void bootstrapCryptoInstruments(UserApiConfig finnhubConfig) {
        for (String symbol : INITIAL_CRYPTO) {
            try {
                if (instrumentRepository.findBySymbolAndIsSimulated(symbol, false).isPresent()) {
                    continue;
                }

                BigDecimal price = providerResolver.fetchCryptoPrice(symbol, finnhubConfig);
                if (price == null) {
                    continue;
                }

                Instrument crypto = new Instrument();
                crypto.setSymbol(symbol);
                crypto.setName(symbol);
                crypto.setType(Instrument.InstrumentType.CRYPTO);
                crypto.setExchange("CRYPTO");
                crypto.setCurrency("USD");
                crypto.setIsActive(true);
                crypto.setCurrentPrice(price);
                crypto.setPreviousClose(price);

                instrumentRepository.save(crypto);
                log.info("Bootstrapped crypto: {} - Price: {}", symbol, price);
            } catch (Exception error) {
                log.error("Error bootstrapping crypto {}", symbol, error);
            }
        }
    }

    private void pauseForRateLimit(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bootstrap interrupted", error);
        }
    }
}
