package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.DataPreferenceRequest;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserApiConfig;
import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.repository.UserApiConfigRepository;
import com.mintstack.finance.repository.UserDataPreferenceRepository;
import com.mintstack.finance.repository.UserRepository;
import com.mintstack.finance.scheduler.MarketDataScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock
    private UserDataPreferenceRepository preferenceRepository;

    @Mock
    private UserApiConfigRepository apiConfigRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MarketDataScheduler marketDataScheduler;

    @InjectMocks
    private DataSourceService dataSourceService;

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId("test-keycloak-id");
        return user;
    }

    @Test
    void triggerDataFetch_ShouldTriggerAsyncFetch() throws InterruptedException {
        // Given
        User user = createTestUser();
        UUID configId = UUID.randomUUID();
        UserApiConfig config = new UserApiConfig();
        config.setId(configId);
        config.setUser(user);
        config.setProvider(ApiProvider.TCMB);
        config.setIsActive(true);

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(apiConfigRepository.findById(configId)).thenReturn(Optional.of(config));

        // When
        Map<String, Object> result = dataSourceService.triggerDataFetch("test-keycloak-id", configId);

        // Then
        assertThat(result.get("triggered")).isEqualTo(true);
        assertThat(result.get("fetchTriggered")).isEqualTo(true);
        verify(apiConfigRepository).save(config);
        
        // Wait briefly for async task
        // Verify with timeout for async execution protection
        verify(marketDataScheduler, timeout(1000)).fetchTcmbRates();
    }

    @Test
    void triggerDataFetch_ShouldThrowAccessDenied_WhenUserDoesNotOwnConfig() {
        // Given
        User user = createTestUser();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        
        UUID configId = UUID.randomUUID();
        UserApiConfig config = new UserApiConfig();
        config.setId(configId);
        config.setUser(otherUser); // Config belongs to other user

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(apiConfigRepository.findById(configId)).thenReturn(Optional.of(config));

        // When & Then
        assertThatThrownBy(() -> dataSourceService.triggerDataFetch("test-keycloak-id", configId))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("yetkiniz yok");
    }

    @Test
    void setPreference_ShouldFail_WhenProviderNotSupported() {
        // Given
        User user = createTestUser();
        DataPreferenceRequest request = new DataPreferenceRequest();
        request.setProvider(ApiProvider.TCMB);
        request.setDataType(DataType.US_STOCKS); // TCMB doesn't support US_STOCKS

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> dataSourceService.setPreference("test-keycloak-id", request))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
