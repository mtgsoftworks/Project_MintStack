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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final UserApiConfigRepository userApiConfigRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ApiConfigResponse> getApiConfigs(UUID userId) {
        return userApiConfigRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiConfigResponse addApiConfig(UUID userId, ApiConfigRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Check if config for this provider already exists, if so update it or throw error?
        // For now, let's allow updating if exists, or just create new if not.
        // Actually, requirement is usually "Add", but let's check unique constraint logic.
        // Logic: If exists for provider, update it. Else create new.
        
        UserApiConfig config = userApiConfigRepository.findByUserIdAndProvider(userId, request.getProvider())
                .orElse(UserApiConfig.builder()
                        .user(user)
                        .provider(request.getProvider())
                        .build());

        config.setApiKey(request.getApiKey());
        config.setSecretKey(request.getSecretKey());
        config.setBaseUrl(request.getBaseUrl());
        config.setIsActive(request.getIsActive());

        UserApiConfig saved = userApiConfigRepository.save(config);
        log.info("Saved API config for user {} provider {}", userId, request.getProvider());
        
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteApiConfig(UUID userId, UUID configId) {
        UserApiConfig config = userApiConfigRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiConfig", "id", configId));

        if (!config.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("ApiConfig", "id", configId); // Don't expose existence
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
