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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final UserDataPreferenceRepository preferenceRepository;
    private final UserApiConfigRepository apiConfigRepository;
    private final UserRepository userRepository;
    private final MarketDataScheduler marketDataScheduler;

    // Provider capabilities matrix (Updated based on official API docs)
    // TCMB: https://evds2.tcmb.gov.tr/
    // Yahoo Finance: https://ranaroussi.github.io/yfinance/
    // Alpha Vantage: https://www.alphavantage.co/documentation/
    // Finnhub: https://finnhub.io/docs/api
    private static final Map<ApiProvider, Set<DataType>> PROVIDER_CAPABILITIES = Map.of(
        ApiProvider.TCMB, Set.of(DataType.CURRENCY_RATES),
        ApiProvider.YAHOO_FINANCE, Set.of(DataType.CURRENCY_RATES, DataType.BIST_STOCKS, DataType.US_STOCKS, DataType.CRYPTO, DataType.NEWS),
        ApiProvider.ALPHA_VANTAGE, Set.of(DataType.CURRENCY_RATES, DataType.US_STOCKS, DataType.CRYPTO, DataType.NEWS),
        ApiProvider.FINNHUB, Set.of(DataType.CURRENCY_RATES, DataType.US_STOCKS, DataType.CRYPTO, DataType.NEWS)
    );

    // Data type labels (Turkish)
    private static final Map<DataType, String> DATA_TYPE_LABELS = Map.of(
        DataType.CURRENCY_RATES, "Döviz Kurları",
        DataType.BIST_STOCKS, "BIST Hisseleri",
        DataType.US_STOCKS, "ABD Hisseleri",
        DataType.CRYPTO, "Kripto Paralar",
        DataType.NEWS, "Haberler"
    );

    // Provider labels
    private static final Map<ApiProvider, String> PROVIDER_LABELS = Map.of(
        ApiProvider.TCMB, "TCMB (Merkez Bankası)",
        ApiProvider.YAHOO_FINANCE, "Yahoo Finance",
        ApiProvider.ALPHA_VANTAGE, "Alpha Vantage",
        ApiProvider.FINNHUB, "Finnhub",
        ApiProvider.OTHER, "Diğer"
    );

    /**
     * Get all provider capabilities
     */
    public Map<String, Object> getProviderCapabilities() {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<ApiProvider, Set<DataType>> entry : PROVIDER_CAPABILITIES.entrySet()) {
            Map<String, Object> providerInfo = new HashMap<>();
            providerInfo.put("label", PROVIDER_LABELS.getOrDefault(entry.getKey(), entry.getKey().name()));
            providerInfo.put("dataTypes", entry.getValue().stream()
                .map(dt -> Map.of(
                    "type", dt.name(),
                    "label", DATA_TYPE_LABELS.getOrDefault(dt, dt.name())
                ))
                .toList());
            result.put(entry.getKey().name(), providerInfo);
        }
        
        return result;
    }

    /**
     * Get user's data preferences
     */
    public List<DataPreferenceResponse> getUserPreferences(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<UserDataPreference> preferences = preferenceRepository.findByUserId(user.getId());
        
        return preferences.stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Set user's data preference for a specific data type
     */
    @Transactional
    public DataPreferenceResponse setPreference(String keycloakId, DataPreferenceRequest request) {
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate provider supports this data type
        Set<DataType> capabilities = PROVIDER_CAPABILITIES.get(request.getProvider());
        if (capabilities == null || !capabilities.contains(request.getDataType())) {
            throw new IllegalArgumentException(
                "Provider " + request.getProvider() + " does not support " + request.getDataType()
            );
        }

        // Check if user has API config for this provider
        Optional<UserApiConfig> apiConfig = apiConfigRepository
            .findByUserIdAndProviderAndIsActiveTrue(user.getId(), request.getProvider());
        
        if (apiConfig.isEmpty()) {
            throw new IllegalArgumentException(
                "You need to add an API key for " + PROVIDER_LABELS.get(request.getProvider()) + " first"
            );
        }

        // Create or update preference
        UserDataPreference preference = preferenceRepository
            .findByUserIdAndDataType(user.getId(), request.getDataType())
            .orElse(UserDataPreference.builder()
                .user(user)
                .dataType(request.getDataType())
                .build());

        preference.setProvider(request.getProvider());
        preference.setIsEnabled(request.getIsEnabled());
        
        preference = preferenceRepository.save(preference);
        
        log.info("User {} set {} data source to {}", 
            user.getId(), request.getDataType(), request.getProvider());
        
        return toResponse(preference);
    }

    /**
     * Trigger immediate data fetch when API key is activated
     * FIX: Now runs async to prevent HTTP response blocking
     */
    @Transactional
    @CacheEvict(value = {"currencyRates", "instruments", "stockPrices"}, allEntries = true)
    public Map<String, Object> triggerDataFetch(String keycloakId, UUID apiConfigId) {
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserApiConfig config = apiConfigRepository.findById(apiConfigId)
            .orElseThrow(() -> new RuntimeException("API config not found"));

        if (!config.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bu API yapılandırmasına erişim yetkiniz yok");
        }

        // Update last triggered timestamp
        config.setLastTriggeredAt(LocalDateTime.now());
        apiConfigRepository.save(config);

        // Auto-create preferences for this provider's capabilities
        Set<DataType> capabilities = PROVIDER_CAPABILITIES.getOrDefault(config.getProvider(), Set.of());
        List<String> createdPreferences = new ArrayList<>();
        
        for (DataType dataType : capabilities) {
            Optional<UserDataPreference> existing = preferenceRepository
                .findByUserIdAndDataType(user.getId(), dataType);
            
            if (existing.isEmpty()) {
                // Auto-assign this provider for this data type
                UserDataPreference pref = UserDataPreference.builder()
                    .user(user)
                    .dataType(dataType)
                    .provider(config.getProvider())
                    .isEnabled(true)
                    .build();
                preferenceRepository.save(pref);
                createdPreferences.add(DATA_TYPE_LABELS.getOrDefault(dataType, dataType.name()));
            }
        }

        // FIX: Trigger data fetch ASYNCHRONOUSLY to prevent HTTP blocking
        ApiProvider provider = config.getProvider();
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting async data fetch for provider: {}", provider);
                switch (provider) {
                    case TCMB:
                        marketDataScheduler.fetchTcmbRates();
                        break;
                    case YAHOO_FINANCE:
                    case ALPHA_VANTAGE:
                    case FINNHUB:
                        marketDataScheduler.fetchStockPrices();
                        marketDataScheduler.fetchNonTcmbForexRates();
                        marketDataScheduler.fetchCryptoPrices();
                        break;
                    default:
                        log.info("No immediate fetch handler for provider: {}", provider);
                }
                log.info("Async data fetch completed for provider: {}", provider);
            } catch (Exception e) {
                log.error("Error during async data fetch for {}: {}", provider, e.getMessage());
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("triggered", true);
        result.put("provider", config.getProvider().name());
        result.put("providerLabel", PROVIDER_LABELS.getOrDefault(config.getProvider(), config.getProvider().name()));
        result.put("autoCreatedPreferences", createdPreferences);
        result.put("fetchTriggered", true);  // Always true now since we trigger async
        result.put("message", "Veri çekme işlemi arka planda başlatıldı");
        
        return result;
    }

    /**
     * Get which providers support a specific data type
     */
    public List<Map<String, Object>> getProvidersForDataType(DataType dataType) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<ApiProvider, Set<DataType>> entry : PROVIDER_CAPABILITIES.entrySet()) {
            if (entry.getValue().contains(dataType)) {
                result.add(Map.of(
                    "provider", entry.getKey().name(),
                    "label", PROVIDER_LABELS.getOrDefault(entry.getKey(), entry.getKey().name())
                ));
            }
        }
        
        return result;
    }

    private DataPreferenceResponse toResponse(UserDataPreference pref) {
        return DataPreferenceResponse.builder()
            .id(pref.getId())
            .dataType(pref.getDataType())
            .provider(pref.getProvider())
            .isEnabled(pref.getIsEnabled())
            .dataTypeLabel(DATA_TYPE_LABELS.getOrDefault(pref.getDataType(), pref.getDataType().name()))
            .providerLabel(PROVIDER_LABELS.getOrDefault(pref.getProvider(), pref.getProvider().name()))
            .build();
    }
}
