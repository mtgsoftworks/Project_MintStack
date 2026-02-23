package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.repository.UserDataPreferenceRepository;
import com.mintstack.finance.service.external.AlphaVantageClient;
import com.mintstack.finance.service.external.FinnhubClient;
import com.mintstack.finance.service.external.YahooFinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;

@Slf4j
@Component
@RequiredArgsConstructor
class MarketDataProviderResolver {

    private final YahooFinanceClient yahooFinanceClient;
    private final AlphaVantageClient alphaVantageClient;
    private final FinnhubClient finnhubClient;
    private final UserApiConfigRepository userApiConfigRepository;
    private final UserDataPreferenceRepository preferenceRepository;

    EnumMap<DataType, ApiProvider> resolvePreferredProviders() {
        EnumMap<DataType, ApiProvider> map = new EnumMap<>(DataType.class);
        for (DataType dataType : DataType.values()) {
            preferenceRepository.findFirstByDataTypeAndIsEnabledTrueOrderByUpdatedAtDesc(dataType)
                .ifPresent((pref) -> map.put(dataType, pref.getProvider()));
        }
        return map;
    }

    boolean hasStockProviderForDataType(
        ApiProvider preferredProvider,
        UserApiConfig yahooConfig,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig
    ) {
        if (preferredProvider != null) {
            return isProviderConfigured(preferredProvider, yahooConfig, alphaConfig, finnhubConfig);
        }
        return yahooConfig != null || alphaConfig != null || finnhubConfig != null;
    }

    DataType resolveDataTypeForInstrument(Instrument.InstrumentType type, Instrument instrument) {
        if (type == Instrument.InstrumentType.STOCK) {
            String exchange = instrument.getExchange();
            if (exchange != null && exchange.equalsIgnoreCase("BIST")) {
                return DataType.BIST_STOCKS;
            }
            return DataType.US_STOCKS;
        }

        if (type == Instrument.InstrumentType.CRYPTO) {
            return DataType.CRYPTO;
        }

        return DataType.BIST_STOCKS;
    }

    BigDecimal fetchInstrumentPrice(
        Instrument instrument,
        DataType dataType,
        ApiProvider preferredProvider,
        UserApiConfig yahooConfig,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig
    ) {
        if (preferredProvider != null) {
            return fetchPriceForProvider(preferredProvider, instrument, yahooConfig, alphaConfig, finnhubConfig);
        }

        BigDecimal price = fetchPriceForProvider(ApiProvider.YAHOO_FINANCE, instrument, yahooConfig, alphaConfig, finnhubConfig);
        if (price == null) {
            price = fetchPriceForProvider(ApiProvider.ALPHA_VANTAGE, instrument, yahooConfig, alphaConfig, finnhubConfig);
        }
        if (price == null) {
            price = fetchPriceForProvider(ApiProvider.FINNHUB, instrument, yahooConfig, alphaConfig, finnhubConfig);
        }
        if (price == null) {
            log.warn("No price fetched for {} using any provider for {}", instrument.getSymbol(), dataType);
        }
        return price;
    }

    CurrencyRate fetchForexRate(
        String fromCurrency,
        String toCurrency,
        ApiProvider preferredProvider,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig
    ) {
        if (preferredProvider != null) {
            return fetchForexRateForProvider(preferredProvider, fromCurrency, toCurrency, alphaConfig, finnhubConfig);
        }

        CurrencyRate rate = fetchForexRateForProvider(ApiProvider.ALPHA_VANTAGE, fromCurrency, toCurrency, alphaConfig, finnhubConfig);
        if (rate == null) {
            rate = fetchForexRateForProvider(ApiProvider.FINNHUB, fromCurrency, toCurrency, alphaConfig, finnhubConfig);
        }
        return rate;
    }

    BigDecimal fetchCryptoPrice(String symbol, UserApiConfig finnhubConfig) {
        if (finnhubConfig == null) {
            return null;
        }
        try {
            return finnhubClient.fetchCryptoPrice(symbol);
        } catch (Exception error) {
            log.warn("FINNHUB crypto fetch failed for {}: {}", symbol, error.getMessage());
            return null;
        }
    }

    UserApiConfig getActiveConfig(ApiProvider provider) {
        return userApiConfigRepository.findByProviderAndIsActiveTrue(provider)
            .stream()
            .findFirst()
            .orElse(null);
    }

    private boolean isProviderConfigured(
        ApiProvider provider,
        UserApiConfig yahooConfig,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig
    ) {
        return switch (provider) {
            case YAHOO_FINANCE -> yahooConfig != null;
            case ALPHA_VANTAGE -> alphaConfig != null;
            case FINNHUB -> finnhubConfig != null;
            default -> false;
        };
    }

    private BigDecimal fetchPriceForProvider(
        ApiProvider provider,
        Instrument instrument,
        UserApiConfig yahooConfig,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig
    ) {
        try {
            return switch (provider) {
                case YAHOO_FINANCE -> fetchFromYahoo(instrument, yahooConfig);
                case ALPHA_VANTAGE -> fetchFromAlpha(instrument, alphaConfig);
                case FINNHUB -> fetchFromFinnhub(instrument, finnhubConfig);
                default -> null;
            };
        } catch (Exception error) {
            log.warn("{} fetch failed for {}: {}", provider, instrument.getSymbol(), error.getMessage());
            return null;
        }
    }

    private CurrencyRate fetchForexRateForProvider(
        ApiProvider provider,
        String fromCurrency,
        String toCurrency,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig
    ) {
        try {
            return switch (provider) {
                case ALPHA_VANTAGE -> alphaConfig == null ? null : alphaVantageClient.fetchExchangeRate(fromCurrency, toCurrency);
                case FINNHUB -> finnhubConfig == null ? null : finnhubClient.fetchForexRate(fromCurrency, toCurrency);
                default -> null;
            };
        } catch (Exception error) {
            log.warn("{} forex fetch failed for {}/{}: {}", provider, fromCurrency, toCurrency, error.getMessage());
            return null;
        }
    }

    private BigDecimal fetchFromYahoo(Instrument instrument, UserApiConfig yahooConfig) {
        if (yahooConfig == null) {
            return null;
        }
        return yahooFinanceClient.fetchStockPrice(instrument.getSymbol(), yahooConfig.getApiKey(), yahooConfig.getBaseUrl());
    }

    private BigDecimal fetchFromAlpha(Instrument instrument, UserApiConfig alphaConfig) {
        if (alphaConfig == null) {
            return null;
        }
        String symbol = buildAlphaSymbol(instrument.getSymbol());
        return alphaVantageClient.fetchGlobalQuote(symbol);
    }

    private BigDecimal fetchFromFinnhub(Instrument instrument, UserApiConfig finnhubConfig) {
        if (finnhubConfig == null) {
            return null;
        }
        return finnhubClient.fetchStockQuote(instrument.getSymbol());
    }

    private String buildAlphaSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return symbol;
        }
        return symbol.contains(".") ? symbol : symbol + ".IS";
    }
}
