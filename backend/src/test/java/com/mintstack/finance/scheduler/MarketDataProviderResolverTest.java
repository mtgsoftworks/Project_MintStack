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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataProviderResolver Unit Tests")
class MarketDataProviderResolverTest {

    @Mock
    private YahooFinanceClient yahooFinanceClient;

    @Mock
    private AlphaVantageClient alphaVantageClient;

    @Mock
    private FinnhubClient finnhubClient;

    @Mock
    private UserApiConfigRepository userApiConfigRepository;

    @Mock
    private UserDataPreferenceRepository preferenceRepository;

    @InjectMocks
    private MarketDataProviderResolver resolver;

    @Test
    @DisplayName("hasStockProviderForDataType should return true for Yahoo even without user config")
    void hasStockProviderForDataType_ShouldReturnTrue_ForYahooWithoutConfig() {
        boolean result = resolver.hasStockProviderForDataType(ApiProvider.YAHOO_FINANCE, null, null, null);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("fetchInstrumentPrice should fallback to Yahoo when preferred Alpha fails")
    void fetchInstrumentPrice_ShouldFallbackToYahoo_WhenPreferredAlphaFails() {
        Instrument instrument = Instrument.builder()
            .symbol("THYAO")
            .exchange("BIST")
            .build();
        UserApiConfig alphaConfig = new UserApiConfig();

        when(alphaVantageClient.fetchGlobalQuote("THYAO.IS"))
            .thenThrow(new RuntimeException("Alpha rate limit"));
        when(yahooFinanceClient.fetchStockPrice("THYAO", null, null))
            .thenReturn(new BigDecimal("287.40"));

        BigDecimal result = resolver.fetchInstrumentPrice(
            instrument,
            DataType.BIST_STOCKS,
            ApiProvider.ALPHA_VANTAGE,
            null,
            alphaConfig,
            null
        );

        assertThat(result).isEqualByComparingTo("287.40");
        verify(alphaVantageClient).fetchGlobalQuote("THYAO.IS");
        verify(yahooFinanceClient).fetchStockPrice("THYAO", null, null);
    }

    @Test
    @DisplayName("fetchInstrumentPrice should use Yahoo direct endpoint when no Yahoo config is present")
    void fetchInstrumentPrice_ShouldUseYahooDirect_WhenYahooConfigMissing() {
        Instrument instrument = Instrument.builder()
            .symbol("AKBNK")
            .exchange("BIST")
            .build();

        when(yahooFinanceClient.fetchStockPrice("AKBNK", null, null))
            .thenReturn(new BigDecimal("66.75"));

        BigDecimal result = resolver.fetchInstrumentPrice(
            instrument,
            DataType.BIST_STOCKS,
            null,
            null,
            null,
            null
        );

        assertThat(result).isEqualByComparingTo("66.75");
        verify(yahooFinanceClient).fetchStockPrice("AKBNK", null, null);
        verify(alphaVantageClient, never()).fetchGlobalQuote("AKBNK.IS");
        verify(finnhubClient, never()).fetchStockQuote("AKBNK");
    }

    @Test
    @DisplayName("fetchForexRate should fallback to Finnhub when preferred TCMB is unsupported")
    void fetchForexRate_ShouldFallback_WhenPreferredProviderUnsupported() {
        CurrencyRate finnhubRate = CurrencyRate.builder()
            .currencyCode("USD")
            .buyingRate(new BigDecimal("38.10"))
            .sellingRate(new BigDecimal("38.20"))
            .source(CurrencyRate.RateSource.FINNHUB)
            .build();

        when(finnhubClient.fetchForexRate("USD", "TRY")).thenReturn(finnhubRate);

        CurrencyRate result = resolver.fetchForexRate(
            "USD",
            "TRY",
            ApiProvider.TCMB,
            null,
            new UserApiConfig()
        );

        assertThat(result).isNotNull();
        assertThat(result.getSource()).isEqualTo(CurrencyRate.RateSource.FINNHUB);
        verify(finnhubClient).fetchForexRate("USD", "TRY");
    }

    @Test
    @DisplayName("fetchForexRate should ignore zero-rate response and fallback to next provider")
    void fetchForexRate_ShouldIgnoreZeroRatesAndFallback() {
        CurrencyRate invalidAlphaRate = CurrencyRate.builder()
            .currencyCode("USD")
            .buyingRate(BigDecimal.ZERO)
            .sellingRate(BigDecimal.ZERO)
            .source(CurrencyRate.RateSource.ALPHA_VANTAGE)
            .build();

        CurrencyRate finnhubRate = CurrencyRate.builder()
            .currencyCode("USD")
            .buyingRate(new BigDecimal("38.30"))
            .sellingRate(new BigDecimal("38.40"))
            .source(CurrencyRate.RateSource.FINNHUB)
            .build();

        when(alphaVantageClient.fetchExchangeRate("USD", "TRY")).thenReturn(invalidAlphaRate);
        when(finnhubClient.fetchForexRate("USD", "TRY")).thenReturn(finnhubRate);

        CurrencyRate result = resolver.fetchForexRate(
            "USD",
            "TRY",
            ApiProvider.ALPHA_VANTAGE,
            new UserApiConfig(),
            new UserApiConfig()
        );

        assertThat(result).isNotNull();
        assertThat(result.getSource()).isEqualTo(CurrencyRate.RateSource.FINNHUB);
    }

    @Test
    @DisplayName("fetchCryptoPrice should return null when provider returns non-positive value")
    void fetchCryptoPrice_ShouldReturnNull_WhenProviderReturnsZero() {
        when(finnhubClient.fetchCryptoPrice("BTC-USD")).thenReturn(BigDecimal.ZERO);

        BigDecimal result = resolver.fetchCryptoPrice("BTC-USD", new UserApiConfig());

        assertThat(result).isNull();
        verify(finnhubClient).fetchCryptoPrice("BTC-USD");
    }
}
