package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.DataPreferenceRequest;
import com.mintstack.finance.dto.response.DataPreferenceResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.entity.UserDataPreference;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.repository.UserDataPreferenceRepository;
import com.mintstack.finance.repository.UserRepository;
import com.mintstack.finance.scheduler.MarketDataScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final UserDataPreferenceRepository preferenceRepository;
    private final UserApiConfigRepository apiConfigRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<MarketDataScheduler> marketDataSchedulerProvider;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    @Value("${app.external-api.fintables.enabled:false}")
    private boolean fintablesEnabled;

    private static final Map<ApiProvider, Set<DataType>> PROVIDER_CAPABILITIES = new EnumMap<>(ApiProvider.class);
    private static final Map<DataType, String> DATA_TYPE_LABELS = new EnumMap<>(DataType.class);
    private static final Map<ApiProvider, String> PROVIDER_LABELS = new EnumMap<>(ApiProvider.class);

    static {
        PROVIDER_CAPABILITIES.put(ApiProvider.TCMB, Set.of(DataType.CURRENCY_RATES));
        PROVIDER_CAPABILITIES.put(ApiProvider.TEFAS, Set.of(DataType.FUNDS));
        PROVIDER_CAPABILITIES.put(ApiProvider.RSS, Set.of(DataType.NEWS));
        PROVIDER_CAPABILITIES.put(ApiProvider.FINTABLES, Set.of(
            DataType.BIST_STOCKS,
            DataType.BIST_INDICES,
            DataType.FUNDS,
            DataType.TECHNICAL_INDICATORS
        ));
        PROVIDER_CAPABILITIES.put(ApiProvider.YAHOO_FINANCE, Set.of(
            DataType.CURRENCY_RATES,
            DataType.BIST_STOCKS,
            DataType.BIST_INDICES,
            DataType.US_STOCKS
        ));
        PROVIDER_CAPABILITIES.put(ApiProvider.ALPHA_VANTAGE, Set.of(
            DataType.CURRENCY_RATES,
            DataType.US_STOCKS
        ));
        PROVIDER_CAPABILITIES.put(ApiProvider.FINNHUB, Set.of(
            DataType.CURRENCY_RATES,
            DataType.US_STOCKS
        ));
        PROVIDER_CAPABILITIES.put(ApiProvider.LLM_ENRICHMENT, Set.of(DataType.NEWS));

        DATA_TYPE_LABELS.put(DataType.CURRENCY_RATES, "Doviz Kurlari");
        DATA_TYPE_LABELS.put(DataType.BIST_STOCKS, "BIST Hisseleri");
        DATA_TYPE_LABELS.put(DataType.BIST_INDICES, "BIST Endeksleri");
        DATA_TYPE_LABELS.put(DataType.US_STOCKS, "ABD Hisseleri");
        DATA_TYPE_LABELS.put(DataType.FUNDS, "Yatirim Fonlari");
        DATA_TYPE_LABELS.put(DataType.NEWS, "Haberler");
        DATA_TYPE_LABELS.put(DataType.TECHNICAL_INDICATORS, "Teknik Gostergeler");
        DATA_TYPE_LABELS.put(DataType.GLOSSARY, "Kavram Sozlugu");

        PROVIDER_LABELS.put(ApiProvider.TCMB, "TCMB (Merkez Bankasi)");
        PROVIDER_LABELS.put(ApiProvider.TEFAS, "TEFAS");
        PROVIDER_LABELS.put(ApiProvider.FINTABLES, "Fintables");
        PROVIDER_LABELS.put(ApiProvider.RSS, "RSS Haber Akislari");
        PROVIDER_LABELS.put(ApiProvider.LLM_ENRICHMENT, "LLM Haber Zenginlestirme");
        PROVIDER_LABELS.put(ApiProvider.YAHOO_FINANCE, "Yahoo Finance");
        PROVIDER_LABELS.put(ApiProvider.ALPHA_VANTAGE, "Alpha Vantage");
        PROVIDER_LABELS.put(ApiProvider.FINNHUB, "Finnhub");
        PROVIDER_LABELS.put(ApiProvider.OTHER, "Diger");
    }

    public Map<String, Object> getProviderCapabilities() {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<ApiProvider, Set<DataType>> entry : PROVIDER_CAPABILITIES.entrySet()) {
            Map<String, Object> providerInfo = new HashMap<>();
            boolean providerEnabled = isProviderEnabled(entry.getKey());
            providerInfo.put("label", PROVIDER_LABELS.getOrDefault(entry.getKey(), entry.getKey().name()));
            providerInfo.put("requiresApiKey", requiresUserApiConfig(entry.getKey()));
            providerInfo.put("passiveByDefault", entry.getKey() == ApiProvider.FINTABLES || entry.getKey() == ApiProvider.LLM_ENRICHMENT);
            providerInfo.put("enabled", providerEnabled);
            providerInfo.put("status", providerEnabled ? "active" : "policy_disabled");
            providerInfo.put("dataTypes", entry.getValue().stream()
                .map(dataType -> Map.of(
                    "type", dataType.name(),
                    "label", DATA_TYPE_LABELS.getOrDefault(dataType, dataType.name())
                ))
                .toList());
            result.put(entry.getKey().name(), providerInfo);
        }

        return result;
    }

    public List<DataPreferenceResponse> getUserPreferences(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return preferenceRepository.findByUserId(user.getId()).stream()
            .filter(preference -> preference.getDataType() != DataType.NEWS)
            .filter(preference -> preference.getDataType() != DataType.TECHNICAL_INDICATORS)
            .filter(preference -> preference.getDataType() != DataType.GLOSSARY)
            .filter(preference -> preference.getDataType() != DataType.CRYPTO)
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    @CacheEvict(value = {"currencyRates", "instruments", "stockPrices"}, allEntries = true)
    public DataPreferenceResponse setPreference(String keycloakId, DataPreferenceRequest request) {
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getDataType() == DataType.NEWS) {
            throw new IllegalArgumentException("Haber kaynagi RSS olarak sabittir; secim yapilamaz.");
        }
        if (request.getDataType() == DataType.CRYPTO) {
            throw new IllegalArgumentException("Kripto para veri kaynagi sistemden kaldirildi; secim yapilamaz.");
        }
        if (request.getDataType() == DataType.TECHNICAL_INDICATORS || request.getDataType() == DataType.GLOSSARY) {
            throw new IllegalArgumentException("Bu veri tipi sistem tarafindan uretilir; kullanici kaynak tercihi yapilamaz.");
        }

        Set<DataType> capabilities = PROVIDER_CAPABILITIES.get(request.getProvider());
        if (capabilities == null || !capabilities.contains(request.getDataType())) {
            throw new IllegalArgumentException(
                "Provider " + request.getProvider() + " does not support " + request.getDataType()
            );
        }
        if (!isProviderEnabled(request.getProvider())) {
            throw new IllegalStateException("Provider " + request.getProvider() + " policy geregi pasif durumda");
        }

        if (requiresUserApiConfig(request.getProvider())) {
            Optional<UserApiConfig> apiConfig = apiConfigRepository
                .findByUserIdAndProviderAndIsActiveTrue(user.getId(), request.getProvider());

            if (apiConfig.isEmpty()) {
                throw new IllegalArgumentException(
                    "You need to add an API key for " + PROVIDER_LABELS.get(request.getProvider()) + " first"
                );
            }
        }

        UserDataPreference preference = preferenceRepository
            .findByUserIdAndDataType(user.getId(), request.getDataType())
            .orElse(UserDataPreference.builder()
                .user(user)
                .dataType(request.getDataType())
                .build());

        preference.setProvider(request.getProvider());
        preference.setIsEnabled(request.getIsEnabled());

        preference = preferenceRepository.save(preference);
        log.info("User {} set {} data source to {}", user.getId(), request.getDataType(), request.getProvider());

        return toResponse(preference);
    }

    @Transactional
    @CacheEvict(value = {"currencyRates", "instruments", "stockPrices"}, allEntries = true)
    public Map<String, Object> triggerDataFetch(String keycloakId, UUID apiConfigId) {
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserApiConfig config = apiConfigRepository.findById(apiConfigId)
            .orElseThrow(() -> new RuntimeException("API config not found"));

        if (!config.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bu API yapilandirmasina erisim yetkiniz yok");
        }
        if (!isProviderEnabled(config.getProvider())) {
            throw new IllegalStateException("Provider " + config.getProvider() + " policy geregi pasif durumda");
        }

        config.setLastTriggeredAt(LocalDateTime.now());
        apiConfigRepository.save(config);

        Set<DataType> capabilities = PROVIDER_CAPABILITIES.getOrDefault(config.getProvider(), Set.of());
        List<String> createdPreferences = new ArrayList<>();
        List<String> advisoryNotes = new ArrayList<>();

        for (DataType dataType : capabilities) {
            if (dataType == DataType.NEWS || dataType == DataType.TECHNICAL_INDICATORS || dataType == DataType.GLOSSARY) {
                continue;
            }
            Optional<UserDataPreference> existing = preferenceRepository
                .findByUserIdAndDataType(user.getId(), dataType);

            if (existing.isEmpty()) {
                UserDataPreference preference = UserDataPreference.builder()
                    .user(user)
                    .dataType(dataType)
                    .provider(config.getProvider())
                    .isEnabled(true)
                    .build();
                preferenceRepository.save(preference);
                createdPreferences.add(DATA_TYPE_LABELS.getOrDefault(dataType, dataType.name()));
            }
        }

        if (config.getProvider() == ApiProvider.ALPHA_VANTAGE) {
            advisoryNotes.add("Alpha Vantage BIST hisse verisinde kisitli olabilir; sistem Yahoo public endpoint fallback'i dener.");
            boolean hasYahoo = !apiConfigRepository
                .findByUserIdAndProviderAndIsActiveTrue(user.getId(), ApiProvider.YAHOO_FINANCE)
                .isEmpty();
            if (!hasYahoo) {
                advisoryNotes.add("Daha tutarli BIST verisi icin Yahoo Finance public kaynak veya Finnhub provider tercihi onerilir.");
            }
        }
        if (config.getProvider() == ApiProvider.FINNHUB) {
            advisoryNotes.add("Finnhub anahtari gecerli olsa bile Forex endpoint erisimi plana gore kisitli olabilir.");
        }
        if (config.getProvider() == ApiProvider.FINTABLES) {
            if (!fintablesEnabled) {
                advisoryNotes.add("Fintables provider policy geregi pasif. APP_EXTERNAL_API_FINTABLES_ENABLED=true olmadan aktif edilmez.");
            } else {
                advisoryNotes.add("Fintables etkin; endpoint ve lisans/kota bilgisi dogrulandiktan sonra production'da kullanin.");
            }
        }

        ApiProvider provider = config.getProvider();
        Executor asyncExecutor = taskExecutor != null ? taskExecutor : Runnable::run;

        CompletableFuture.runAsync(() -> {
            try {
                MarketDataScheduler scheduler = marketDataSchedulerProvider.getIfAvailable();
                if (scheduler == null) {
                    log.warn("MarketDataScheduler bean not available, skipping async fetch for provider: {}", provider);
                    return;
                }

                log.info("Starting async data fetch for provider: {}", provider);
                switch (provider) {
                    case TCMB -> scheduler.fetchTcmbRates();
                    case TEFAS -> scheduler.fetchFundPrices();
                    case YAHOO_FINANCE, ALPHA_VANTAGE, FINNHUB -> {
                        scheduler.fetchStockPrices();
                        scheduler.fetchNonTcmbForexRates();
                    }
                    default -> log.info("No immediate fetch handler for provider: {}", provider);
                }
                log.info("Async data fetch completed for provider: {}", provider);
            } catch (Exception error) {
                log.error("Error during async data fetch for {}: {}", provider, error.getMessage());
            }
        }, asyncExecutor);

        Map<String, Object> result = new HashMap<>();
        result.put("triggered", true);
        result.put("provider", config.getProvider().name());
        result.put("providerLabel", PROVIDER_LABELS.getOrDefault(config.getProvider(), config.getProvider().name()));
        result.put("autoCreatedPreferences", createdPreferences);
        result.put("advisoryNotes", advisoryNotes);
        result.put("fetchTriggered", true);
        result.put("message", "Veri cekme islemi arka planda baslatildi");

        return result;
    }

    public List<Map<String, Object>> getProvidersForDataType(DataType dataType) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<ApiProvider, Set<DataType>> entry : PROVIDER_CAPABILITIES.entrySet()) {
            if (entry.getValue().contains(dataType) && isProviderEnabled(entry.getKey())) {
                result.add(Map.of(
                    "provider", entry.getKey().name(),
                    "label", PROVIDER_LABELS.getOrDefault(entry.getKey(), entry.getKey().name()),
                    "requiresApiKey", requiresUserApiConfig(entry.getKey())
                ));
            }
        }

        return result;
    }

    private boolean requiresUserApiConfig(ApiProvider provider) {
        return provider != ApiProvider.TCMB
            && provider != ApiProvider.TEFAS
            && provider != ApiProvider.RSS
            && provider != ApiProvider.YAHOO_FINANCE;
    }

    private boolean isProviderEnabled(ApiProvider provider) {
        if (provider == ApiProvider.FINTABLES) {
            return fintablesEnabled;
        }
        return true;
    }

    private DataPreferenceResponse toResponse(UserDataPreference preference) {
        return DataPreferenceResponse.builder()
            .id(preference.getId())
            .dataType(preference.getDataType())
            .provider(preference.getProvider())
            .isEnabled(preference.getIsEnabled())
            .dataTypeLabel(DATA_TYPE_LABELS.getOrDefault(preference.getDataType(), preference.getDataType().name()))
            .providerLabel(PROVIDER_LABELS.getOrDefault(preference.getProvider(), preference.getProvider().name()))
            .build();
    }
}
