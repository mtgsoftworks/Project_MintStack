package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.PriceUpdateService;
import com.mintstack.finance.service.event.EventPublisher;
import com.mintstack.finance.service.external.TcmbApiClient;
import com.mintstack.finance.service.simulation.SimulationDataService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataScheduler {

    private static final List<String> FOREX_PAIRS = List.of(
        "USD/TRY", "EUR/TRY", "GBP/TRY", "CHF/TRY", "JPY/TRY"
    );

    private final TcmbApiClient tcmbApiClient;
    private final MarketDataService marketDataService;
    private final PriceUpdateService priceUpdateService;
    private final InstrumentRepository instrumentRepository;
    private final EventPublisher eventPublisher;
    private final SimulationDataService simulationDataService;
    private final MarketDataProviderResolver providerResolver;
    private final MarketDataInstrumentUpdateService instrumentUpdateService;
    private final MarketDataBootstrapService bootstrapService;

    @Observed(name = "scheduler.market-data.tcmb", contextualName = "fetch-tcmb-rates")
    @Scheduled(cron = "${app.scheduler.tcmb-rates-cron}")
    public void fetchTcmbRates() {
        if (simulationDataService.isSimulationEnabled()) {
            log.debug("Simulation mode active. Skipping TCMB rates fetch.");
            return;
        }

        UserApiConfig tcmbConfig = providerResolver.getActiveConfig(ApiProvider.TCMB);
        if (tcmbConfig == null) {
            log.debug("TCMB API not configured. Skipping currency rates fetch.");
            return;
        }

        log.info("Starting TCMB rates fetch job");
        try {
            List<CurrencyRate> rates = tcmbApiClient.fetchTodayRates();
            marketDataService.saveCurrencyRates(rates);
            rates.forEach(this::broadcastAndPublishCurrencyRate);
            log.info("TCMB rates fetch completed: {} rates saved", rates.size());
        } catch (Exception error) {
            log.error("TCMB rates fetch failed", error);
        }
    }

    @Observed(name = "scheduler.market-data.stock", contextualName = "fetch-stock-prices")
    @Scheduled(cron = "${app.scheduler.stock-prices-cron}")
    public void fetchStockPrices() {
        updateInstrumentPricesByType(Instrument.InstrumentType.STOCK, true);
    }

    @Observed(name = "scheduler.market-data.bond", contextualName = "fetch-bond-prices")
    @Scheduled(cron = "${app.scheduler.bond-prices-cron}")
    public void fetchBondPrices() {
        updateInstrumentPricesByType(Instrument.InstrumentType.BOND, false);
    }

    @Observed(name = "scheduler.market-data.fund", contextualName = "fetch-fund-prices")
    @Scheduled(cron = "${app.scheduler.fund-prices-cron}")
    public void fetchFundPrices() {
        updateInstrumentPricesByType(Instrument.InstrumentType.FUND, false);
    }

    @Observed(name = "scheduler.market-data.viop", contextualName = "fetch-viop-prices")
    @Scheduled(cron = "${app.scheduler.viop-prices-cron}")
    public void fetchViopPrices() {
        updateInstrumentPricesByType(Instrument.InstrumentType.VIOP, false);
    }

    @Observed(name = "scheduler.market-data.initial-load", contextualName = "initial-data-load")
    @Scheduled(initialDelay = 5000, fixedDelay = 60000)
    public void initialDataLoad() {
        if (simulationDataService.isSimulationEnabled()) {
            log.debug("Simulation mode active. Skipping initial data load from external APIs.");
            return;
        }

        UserApiConfig tcmbConfig = providerResolver.getActiveConfig(ApiProvider.TCMB);
        if (tcmbConfig != null) {
            try {
                List<CurrencyRate> rates = tcmbApiClient.fetchTodayRates();
                if (rates != null && !rates.isEmpty()) {
                    marketDataService.saveCurrencyRates(rates);
                    log.info("TCMB rates loaded: {} currencies", rates.size());
                }
            } catch (Exception error) {
                log.debug("TCMB load: {}", error.getMessage());
            }
        }

        long stockCount = instrumentRepository.countRealInstruments();
        if (stockCount == 0) {
            UserApiConfig alphaConfig = providerResolver.getActiveConfig(ApiProvider.ALPHA_VANTAGE);
            UserApiConfig yahooConfig = providerResolver.getActiveConfig(ApiProvider.YAHOO_FINANCE);
            UserApiConfig finnhubConfig = providerResolver.getActiveConfig(ApiProvider.FINNHUB);
            EnumMap<DataType, ApiProvider> preferredProviders = providerResolver.resolvePreferredProviders();
            ApiProvider preferredBistProvider = preferredProviders.get(DataType.BIST_STOCKS);

            if (providerResolver.hasStockProviderForDataType(preferredBistProvider, yahooConfig, alphaConfig, finnhubConfig)) {
                log.info("Database empty & API key found. Bootstrapping stocks from API...");
                bootstrapService.bootstrapStocksFromApi(preferredBistProvider, yahooConfig, alphaConfig, finnhubConfig);
            } else {
                log.debug("Database empty but no API key configured yet. Waiting...");
            }
        }
    }

    @Observed(name = "scheduler.market-data.forex", contextualName = "fetch-non-tcmb-forex-rates")
    @Scheduled(cron = "${app.scheduler.forex-rates-cron}")
    public void fetchNonTcmbForexRates() {
        if (simulationDataService.isSimulationEnabled()) {
            log.debug("Simulation mode active. Skipping non-TCMB forex rates fetch.");
            return;
        }

        UserApiConfig alphaConfig = providerResolver.getActiveConfig(ApiProvider.ALPHA_VANTAGE);
        UserApiConfig finnhubConfig = providerResolver.getActiveConfig(ApiProvider.FINNHUB);
        EnumMap<DataType, ApiProvider> preferredProviders = providerResolver.resolvePreferredProviders();
        ApiProvider preferredForexProvider = preferredProviders.get(DataType.CURRENCY_RATES);

        if (alphaConfig == null && finnhubConfig == null) {
            log.debug("No forex provider configured. Skipping non-TCMB forex rates fetch.");
            return;
        }

        log.info("Starting non-TCMB forex rates fetch job");
        int batchSize = 2;

        for (int index = 0; index < FOREX_PAIRS.size(); index += batchSize) {
            int end = Math.min(index + batchSize, FOREX_PAIRS.size());
            List<String> batch = FOREX_PAIRS.subList(index, end);

            for (String pair : batch) {
                String[] currencies = pair.split("/");
                String from = currencies[0];
                String to = currencies[1];

                try {
                    CurrencyRate rate = providerResolver.fetchForexRate(from, to, preferredForexProvider, alphaConfig, finnhubConfig);
                    if (rate != null) {
                        marketDataService.saveCurrencyRates(List.of(rate));
                        broadcastAndPublishCurrencyRate(rate);
                    }
                } catch (Exception error) {
                    log.error("Failed to fetch forex rate for {}: {}", pair, error.getMessage());
                }
            }

        }
        log.info("Non-TCMB forex rates fetch completed.");
    }

    @Observed(name = "scheduler.market-data.crypto", contextualName = "fetch-crypto-prices")
    @Scheduled(cron = "${app.scheduler.crypto-prices-cron}")
    public void fetchCryptoPrices() {
        if (simulationDataService.isSimulationEnabled()) {
            log.debug("Simulation mode active. Skipping crypto prices fetch.");
            return;
        }

        UserApiConfig finnhubConfig = providerResolver.getActiveConfig(ApiProvider.FINNHUB);
        if (finnhubConfig == null) {
            log.debug("No crypto provider configured. Skipping crypto prices fetch.");
            return;
        }

        log.info("Starting crypto prices fetch job");
        List<Instrument> cryptoInstruments = instrumentRepository
            .findByTypeAndIsActiveTrueAndIsSimulated(Instrument.InstrumentType.CRYPTO, false);

        if (cryptoInstruments.isEmpty()) {
            log.info("No crypto instruments found. Bootstrapping crypto instruments.");
            bootstrapService.bootstrapCryptoInstruments(finnhubConfig);
            return;
        }

        int batchSize = 3;
        for (int index = 0; index < cryptoInstruments.size(); index += batchSize) {
            int end = Math.min(index + batchSize, cryptoInstruments.size());
            List<Instrument> batch = cryptoInstruments.subList(index, end);

            for (Instrument instrument : batch) {
                try {
                    BigDecimal price = providerResolver.fetchCryptoPrice(instrument.getSymbol(), finnhubConfig);
                    if (price != null) {
                        instrumentUpdateService.updateInstrumentPrice(instrument, price, null);
                    }
                } catch (Exception error) {
                    log.error("Failed to update crypto price for {}: {}", instrument.getSymbol(), error.getMessage());
                }
            }

        }
        log.info("Crypto prices fetch completed.");
    }

    private void broadcastAndPublishCurrencyRate(CurrencyRate rate) {
        priceUpdateService.broadcastCurrencyUpdate(
            rate.getCurrencyCode(),
            rate.getBuyingRate(),
            rate.getSellingRate()
        );

        eventPublisher.publishMarketDataEvent(
            rate.getCurrencyCode(),
            "CURRENCY_RATE",
            Map.of(
                "buyingRate", rate.getBuyingRate(),
                "sellingRate", rate.getSellingRate(),
                "date", rate.getFetchedAt() != null ? rate.getFetchedAt().toString() : LocalDateTime.now().toString()
            )
        );
    }

    private void updateInstrumentPricesByType(Instrument.InstrumentType type, boolean bootstrapStocksWhenEmpty) {
        if (simulationDataService.isSimulationEnabled()) {
            log.debug("Simulation mode active. Skipping {} prices fetch.", type);
            return;
        }

        log.info("Starting {} prices fetch job", type);
        try {
            ProviderContext providerContext = resolveProviderContext();
            ApiProvider preferredBistProvider = providerContext.preferredProviders().get(DataType.BIST_STOCKS);

            if (bootstrapStocksWhenEmpty) {
                long instrumentCount = instrumentRepository.countRealInstruments();
                if (instrumentCount == 0) {
                    log.info("No real instruments found. Bootstrapping stock universe before updates.");
                    if (providerResolver.hasStockProviderForDataType(
                        preferredBistProvider,
                        providerContext.yahooConfig(),
                        providerContext.alphaConfig(),
                        providerContext.finnhubConfig())) {
                        bootstrapService.bootstrapStocksFromApi(
                            preferredBistProvider,
                            providerContext.yahooConfig(),
                            providerContext.alphaConfig(),
                            providerContext.finnhubConfig()
                        );
                    } else {
                        log.info("No suitable provider config found for stock bootstrap. Skipping bootstrap.");
                    }
                    return;
                }
            }

            instrumentUpdateService.updatePricesForType(
                type,
                providerContext.yahooConfig(),
                providerContext.alphaConfig(),
                providerContext.finnhubConfig(),
                providerContext.preferredProviders()
            );
            log.info("{} prices fetch completed.", type);
        } catch (Exception error) {
            log.error("{} prices fetch failed", type, error);
        }
    }

    private ProviderContext resolveProviderContext() {
        UserApiConfig yahooConfig = providerResolver.getActiveConfig(ApiProvider.YAHOO_FINANCE);
        UserApiConfig alphaConfig = providerResolver.getActiveConfig(ApiProvider.ALPHA_VANTAGE);
        UserApiConfig finnhubConfig = providerResolver.getActiveConfig(ApiProvider.FINNHUB);
        EnumMap<DataType, ApiProvider> preferredProviders = providerResolver.resolvePreferredProviders();
        return new ProviderContext(yahooConfig, alphaConfig, finnhubConfig, preferredProviders);
    }

    private record ProviderContext(
        UserApiConfig yahooConfig,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig,
        EnumMap<DataType, ApiProvider> preferredProviders
    ) {
    }

}
