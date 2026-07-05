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
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;

@Slf4j
@Component
@RequiredArgsConstructor
@Observed(name = "market-data.provider-resolver", contextualName = "market-data-provider-resolver")
class MarketDataProviderResolver {

    private final YahooFinanceClient yahooFinanceClient;
    private final AlphaVantageClient alphaVantageClient;
    private final FinnhubClient finnhubClient;
    private final UserApiConfigRepository userApiConfigRepository;
    private final UserDataPreferenceRepository preferenceRepository;

    @Observed(name = "market-data.resolve-preferred-providers", contextualName = "resolve-preferred-providers")
    public EnumMap<DataType, ApiProvider> resolvePreferredProviders() {
        EnumMap<DataType, ApiProvider> map = new EnumMap<>(DataType.class);
        for (DataType dataType : DataType.values()) {
            preferenceRepository.findFirstByDataTypeAndIsEnabledTrueOrderByUpdatedAtDesc(dataType)
                .ifPresent((pref) -> map.put(dataType, pref.getProvider()));
        }
        return map;
    }

    public boolean hasStockProviderForDataType(
        ApiProvider preferredProvider,
        UserApiConfig yahooConfig,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig
    ) {
        if (preferredProvider != null && isProviderConfigured(preferredProvider, yahooConfig, alphaConfig, finnhubConfig)) {
            return true;
        }
        return isProviderConfigured(ApiProvider.YAHOO_FINANCE, yahooConfig, alphaConfig, finnhubConfig)
            || isProviderConfigured(ApiProvider.ALPHA_VANTAGE, yahooConfig, alphaConfig, finnhubConfig)
            || isProviderConfigured(ApiProvider.FINNHUB, yahooConfig, alphaConfig, finnhubConfig);
    }

    public DataType resolveDataTypeForInstrument(Instrument.InstrumentType type, Instrument instrument) {
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

        if (type == Instrument.InstrumentType.INDEX) {
            return DataType.BIST_INDICES;
        }

        return null;
    }

    @Observed(name = "market-data.fetch-instrument-price", contextualName = "fetch-instrument-price")
    public BigDecimal fetchInstrumentPrice(
        Instrument instrument,
        DataType dataType,
        ApiProvider preferredProvider,
        UserApiConfig yahooConfig,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig
    ) {
        BigDecimal price = null;

        if (preferredProvider != null) {
            price = fetchPriceForProvider(preferredProvider, instrument, yahooConfig, alphaConfig, finnhubConfig);
            if (price != null) {
                return price;
            }
            log.warn("Preferred provider {} returned no price for {}. Trying fallback providers.",
                preferredProvider, instrument.getSymbol());
        }

        for (ApiProvider provider : ApiProvider.values()) {
            if (!isMarketPriceProvider(provider) || provider == preferredProvider) {
                continue;
            }
            price = fetchPriceForProvider(provider, instrument, yahooConfig, alphaConfig, finnhubConfig);
            if (price != null) {
                return price;
            }
        }

        if (price == null) {
            log.warn("No price fetched for {} using any provider for {}", instrument.getSymbol(), dataType);
        }
        return price;
    }

    public Long resolveLatestVolume(Instrument instrument) {
        if (instrument == null || instrument.getSymbol() == null) {
            return null;
        }
        return yahooFinanceClient.getLatestVolume(instrument.getSymbol());
    }

    @Observed(name = "market-data.fetch-forex-rate", contextualName = "fetch-forex-rate")
    public CurrencyRate fetchForexRate(
        String fromCurrency,
        String toCurrency,
        ApiProvider preferredProvider,
        UserApiConfig alphaConfig,
        UserApiConfig finnhubConfig
    ) {
        if (preferredProvider != null) {
            CurrencyRate preferredRate = fetchForexRateForProvider(
                preferredProvider,
                fromCurrency,
                toCurrency,
                alphaConfig,
                finnhubConfig
            );
            if (isValidForexRate(preferredRate)) {
                return preferredRate;
            }
            log.warn(
                "Preferred forex provider {} returned invalid/no rate for {}/{}. Falling back.",
                preferredProvider,
                fromCurrency,
                toCurrency
            );
        }

        CurrencyRate rate = fetchForexRateForProvider(ApiProvider.ALPHA_VANTAGE, fromCurrency, toCurrency, alphaConfig, finnhubConfig);
        if (!isValidForexRate(rate)) {
            rate = fetchForexRateForProvider(ApiProvider.FINNHUB, fromCurrency, toCurrency, alphaConfig, finnhubConfig);
        }
        return isValidForexRate(rate) ? rate : null;
    }

    @Observed(name = "market-data.fetch-crypto-price", contextualName = "fetch-crypto-price")
    public BigDecimal fetchCryptoPrice(String symbol, UserApiConfig finnhubConfig) {
        if (finnhubConfig == null) {
            return null;
        }
        try {
            BigDecimal price = finnhubClient.fetchCryptoPrice(symbol);
            if (price == null) {
                log.debug("FINNHUB returned no crypto price for {}", symbol);
                return null;
            }
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("FINNHUB crypto returned invalid price for {}: {}", symbol, price);
                return null;
            }
            return price;
        } catch (Exception error) {
            log.warn("FINNHUB crypto fetch failed for {}: {}", symbol, error.getMessage());
            return null;
        }
    }

    public UserApiConfig getActiveConfig(ApiProvider provider) {
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
        // STRICT: Every provider requires an explicit active config entry in the DB.
        // No config = not configured. No exceptions.
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
            BigDecimal price = switch (provider) {
                case YAHOO_FINANCE -> fetchFromYahoo(instrument, yahooConfig);
                case ALPHA_VANTAGE -> fetchFromAlpha(instrument, alphaConfig);
                case FINNHUB -> fetchFromFinnhub(instrument, finnhubConfig);
                default -> null;
            };
            if (price == null) {
                return null;
            }
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("{} returned invalid price for {}: {}", provider, instrument.getSymbol(), price);
                return null;
            }
            return price;
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

    private boolean isValidForexRate(CurrencyRate rate) {
        if (rate == null || rate.getBuyingRate() == null || rate.getSellingRate() == null) {
            return false;
        }
        return rate.getBuyingRate().compareTo(BigDecimal.ZERO) > 0
            && rate.getSellingRate().compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal fetchFromYahoo(Instrument instrument, UserApiConfig yahooConfig) {
        String apiKey = yahooConfig != null ? yahooConfig.getApiKey() : null;
        String baseUrl = yahooConfig != null ? yahooConfig.getBaseUrl() : null;
        return yahooFinanceClient.fetchStockPrice(instrument.getSymbol(), apiKey, baseUrl);
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

    private boolean isMarketPriceProvider(ApiProvider provider) {
        return provider == ApiProvider.YAHOO_FINANCE
            || provider == ApiProvider.ALPHA_VANTAGE
            || provider == ApiProvider.FINNHUB;
    }
}
