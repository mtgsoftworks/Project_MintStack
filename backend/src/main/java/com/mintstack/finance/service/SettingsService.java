package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.ApiConfigRequest;
import com.mintstack.finance.dto.response.ApiConfigResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final UserApiConfigRepository userApiConfigRepository;
    private final UserRepository userRepository;
    private final ApiKeyValidationService apiKeyValidationService;

    @Transactional(readOnly = true)
    public List<ApiConfigResponse> getApiConfigs(UUID userId) {
        return userApiConfigRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get default URLs for all providers
     */
    public Map<UserApiConfig.ApiProvider, String> getDefaultUrls() {
        return apiKeyValidationService.getAllDefaultUrls();
    }

    /**
     * Validate API key without saving - returns validation result
     */
    public ApiKeyValidationService.ValidationResult validateApiKey(ApiConfigRequest request) {
        return apiKeyValidationService.validateApiKey(
                request.getProvider(),
                request.getApiKey(),
                request.getBaseUrl()
        );
    }

    @Transactional
    public ApiConfigResponse addApiConfig(UUID userId, ApiConfigRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Check if this is an update (existing config)
        UserApiConfig existingConfig = userApiConfigRepository
                .findByUserIdAndProvider(userId, request.getProvider())
                .orElse(null);
        
        boolean isUpdate = existingConfig != null;
        boolean apiKeyChanged = request.getApiKey() != null && !request.getApiKey().isEmpty();
        
        // 1. Validate API key only if it's new or changed
        if (!isUpdate || apiKeyChanged) {
            ApiKeyValidationService.ValidationResult validation = apiKeyValidationService.validateApiKey(
                    request.getProvider(),
                    request.getApiKey(),
                    request.getBaseUrl()
            );

            if (!validation.isValid()) {
                throw new IllegalArgumentException("API anahtarı geçersiz: " + validation.getMessage());
            }
        }

        // 2. Apply default URL if not provided
        String effectiveUrl = request.getBaseUrl();
        if (effectiveUrl == null || effectiveUrl.isEmpty()) {
            effectiveUrl = isUpdate && existingConfig.getBaseUrl() != null 
                    ? existingConfig.getBaseUrl() 
                    : apiKeyValidationService.getDefaultUrl(request.getProvider());
        }

        // 3. Save or update config
        UserApiConfig config = existingConfig != null ? existingConfig : UserApiConfig.builder()
                .user(user)
                .provider(request.getProvider())
                .build();

        // Only update API key if provided (for edit mode, keep existing if empty)
        if (apiKeyChanged) {
            config.setApiKey(request.getApiKey());
        }
        
        // Update secret key only if provided
        if (request.getSecretKey() != null && !request.getSecretKey().isEmpty()) {
            config.setSecretKey(request.getSecretKey());
        }
        
        config.setBaseUrl(effectiveUrl);
        config.setIsActive(request.getIsActive());

        UserApiConfig saved = userApiConfigRepository.save(config);
        log.info("{} API config for user {} provider {}", isUpdate ? "Updated" : "Created", userId, request.getProvider());
        
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteApiConfig(UUID userId, UUID configId) {
        UserApiConfig config = userApiConfigRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiConfig", "id", configId));

        if (!config.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("ApiConfig", "id", configId);
        }

        userApiConfigRepository.delete(config);
        log.info("Deleted API config {} for user {}", configId, userId);
    }

    private ApiConfigResponse mapToResponse(UserApiConfig config) {
        return ApiConfigResponse.builder()
                .id(config.getId())
                .provider(config.getProvider())
                .apiKey(maskApiKey(config.getApiKey()))
                .baseUrl(config.getBaseUrl())
                .isActive(config.getIsActive())
                .createdAt(config.getCreatedAt())
                .build();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}

