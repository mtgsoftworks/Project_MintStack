package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.ApiConfigRequest;
import com.mintstack.finance.dto.response.ApiConfigResponse;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private UserApiConfigRepository userApiConfigRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApiKeyValidationService apiKeyValidationService;

    @InjectMocks
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        // Configure validation service to return valid by default
        lenient().when(apiKeyValidationService.validateApiKey(any(), anyString(), any()))
                .thenReturn(new ApiKeyValidationService.ValidationResult(true, "Valid"));
        lenient().when(apiKeyValidationService.getDefaultUrl(any()))
                .thenReturn("https://default-url.com");
    }

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        return user;
    }

    private UserApiConfig createTestConfig(User user) {
        UserApiConfig config = new UserApiConfig();
        config.setId(UUID.randomUUID());
        config.setUser(user);
        config.setProvider(ApiProvider.ALPHA_VANTAGE);
        config.setApiKey("test-api-key-12345678");
        config.setIsActive(true);
        return config;
    }

    @Test
    void getApiConfigs_ShouldReturnConfigs() {
        // Given
        User user = createTestUser();
        UserApiConfig config = createTestConfig(user);
        
        when(userApiConfigRepository.findByUserId(user.getId())).thenReturn(List.of(config));

        // When
        List<ApiConfigResponse> result = settingsService.getApiConfigs(user.getId());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProvider()).isEqualTo(ApiProvider.ALPHA_VANTAGE);
    }

    @Test
    void getApiConfigs_ShouldMaskApiKey() {
        // Given
        User user = createTestUser();
        UserApiConfig config = createTestConfig(user);
        
        when(userApiConfigRepository.findByUserId(user.getId())).thenReturn(List.of(config));

        // When
        List<ApiConfigResponse> result = settingsService.getApiConfigs(user.getId());

        // Then
        assertThat(result.get(0).getApiKey()).isEqualTo("test****5678");
    }

    @Test
    void addApiConfig_ShouldCreateNewConfig() {
        // Given
        User user = createTestUser();
        ApiConfigRequest request = new ApiConfigRequest();
        request.setProvider(ApiProvider.YAHOO_FINANCE);
        request.setApiKey("new-api-key");
        request.setIsActive(true);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userApiConfigRepository.findByUserIdAndProvider(user.getId(), ApiProvider.YAHOO_FINANCE)).thenReturn(Optional.empty());
        when(userApiConfigRepository.save(any(UserApiConfig.class))).thenAnswer(i -> {
            UserApiConfig saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        // When
        ApiConfigResponse result = settingsService.addApiConfig(user.getId(), request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProvider()).isEqualTo(ApiProvider.YAHOO_FINANCE);
        verify(userApiConfigRepository).save(any(UserApiConfig.class));
    }

    @Test
    void addApiConfig_ShouldUpdateExistingConfig() {
        // Given
        User user = createTestUser();
        UserApiConfig existingConfig = createTestConfig(user);
        
        ApiConfigRequest request = new ApiConfigRequest();
        request.setProvider(ApiProvider.ALPHA_VANTAGE);
        request.setApiKey("updated-api-key-12345678");
        request.setIsActive(true);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userApiConfigRepository.findByUserIdAndProvider(user.getId(), ApiProvider.ALPHA_VANTAGE)).thenReturn(Optional.of(existingConfig));
        when(userApiConfigRepository.save(any(UserApiConfig.class))).thenReturn(existingConfig);

        // When
        ApiConfigResponse result = settingsService.addApiConfig(user.getId(), request);

        // Then
        assertThat(result).isNotNull();
        verify(userApiConfigRepository).save(existingConfig);
    }

    @Test
    void addApiConfig_ShouldThrowWhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        ApiConfigRequest request = new ApiConfigRequest();
        request.setProvider(ApiProvider.OTHER);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settingsService.addApiConfig(userId, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteApiConfig_ShouldDeleteConfig() {
        // Given
        User user = createTestUser();
        UserApiConfig config = createTestConfig(user);

        when(userApiConfigRepository.findById(config.getId())).thenReturn(Optional.of(config));

        // When
        settingsService.deleteApiConfig(user.getId(), config.getId());

        // Then
        verify(userApiConfigRepository).delete(config);
    }

    @Test
    void deleteApiConfig_ShouldThrowWhenConfigNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID configId = UUID.randomUUID();

        when(userApiConfigRepository.findById(configId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> settingsService.deleteApiConfig(userId, configId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteApiConfig_ShouldThrowWhenConfigBelongsToDifferentUser() {
        // Given
        User user = createTestUser();
        User anotherUser = createTestUser(); // Different user
        UserApiConfig config = createTestConfig(anotherUser);

        when(userApiConfigRepository.findById(config.getId())).thenReturn(Optional.of(config));

        // When & Then
        assertThatThrownBy(() -> settingsService.deleteApiConfig(user.getId(), config.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getApiConfigs_ShouldReturnEmptyList_WhenNoConfigs() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userApiConfigRepository.findByUserId(userId)).thenReturn(List.of());

        // When
        List<ApiConfigResponse> result = settingsService.getApiConfigs(userId);

        // Then
        assertThat(result).isEmpty();
    }
}
