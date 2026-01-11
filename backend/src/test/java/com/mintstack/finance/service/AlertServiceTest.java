package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.CreateAlertRequest;
import com.mintstack.finance.dto.response.AlertResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceAlert;
import com.mintstack.finance.entity.PriceAlert.AlertType;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.exception.BusinessException;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceAlertRepository;
import com.mintstack.finance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private PriceAlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PriceUpdateService priceUpdateService;

    @InjectMocks
    private AlertService alertService;

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId("test-keycloak-id");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        return user;
    }

    private Instrument createTestInstrument() {
        Instrument instrument = new Instrument();
        instrument.setId(UUID.randomUUID());
        instrument.setSymbol("USD/TRY");
        instrument.setName("US Dollar");
        instrument.setCurrentPrice(BigDecimal.valueOf(34.50));
        return instrument;
    }

    private PriceAlert createTestAlert(User user, Instrument instrument) {
        PriceAlert alert = PriceAlert.builder()
            .user(user)
            .instrument(instrument)
            .alertType(AlertType.PRICE_ABOVE)
            .targetValue(BigDecimal.valueOf(35.00))
            .currentValueAtCreation(BigDecimal.valueOf(34.50))
            .isActive(true)
            .isTriggered(false)
            .build();
        alert.setId(UUID.randomUUID());
        return alert;
    }

    @Test
    void getUserAlerts_ShouldReturnAlerts() {
        // Given
        User user = createTestUser();
        Instrument instrument = createTestInstrument();
        PriceAlert alert = createTestAlert(user, instrument);

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(alert));

        // When
        List<AlertResponse> result = alertService.getUserAlerts("test-keycloak-id");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSymbol()).isEqualTo("USD/TRY");
    }

    @Test
    void getActiveAlerts_ShouldReturnOnlyActiveAlerts() {
        // Given
        User user = createTestUser();
        Instrument instrument = createTestInstrument();
        PriceAlert alert = createTestAlert(user, instrument);

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(alertRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(List.of(alert));

        // When
        List<AlertResponse> result = alertService.getActiveAlerts("test-keycloak-id");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    void createAlert_ShouldCreateAlert() {
        // Given
        User user = createTestUser();
        Instrument instrument = createTestInstrument();
        CreateAlertRequest request = new CreateAlertRequest();
        request.setSymbol("USD/TRY");
        request.setAlertType(AlertType.PRICE_ABOVE);
        request.setTargetValue(BigDecimal.valueOf(35.00));

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(alertRepository.countByUserIdAndIsActiveTrue(user.getId())).thenReturn(5L);
        when(instrumentRepository.findBySymbol("USD/TRY")).thenReturn(Optional.of(instrument));
        when(alertRepository.save(any(PriceAlert.class))).thenAnswer(i -> {
            PriceAlert saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        // When
        AlertResponse result = alertService.createAlert("test-keycloak-id", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("USD/TRY");
        verify(alertRepository).save(any(PriceAlert.class));
    }

    @Test
    void createAlert_ShouldThrowWhenMaxAlertsReached() {
        // Given
        User user = createTestUser();
        CreateAlertRequest request = new CreateAlertRequest();
        request.setSymbol("USD/TRY");
        request.setAlertType(AlertType.PRICE_ABOVE);
        request.setTargetValue(BigDecimal.valueOf(35.00));

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(alertRepository.countByUserIdAndIsActiveTrue(user.getId())).thenReturn(20L);

        // When & Then
        assertThatThrownBy(() -> alertService.createAlert("test-keycloak-id", request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Maksimum");
    }

    @Test
    void deleteAlert_ShouldDeleteAlert() {
        // Given
        User user = createTestUser();
        Instrument instrument = createTestInstrument();
        PriceAlert alert = createTestAlert(user, instrument);

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(alertRepository.findByIdAndUserId(alert.getId(), user.getId())).thenReturn(Optional.of(alert));

        // When
        alertService.deleteAlert("test-keycloak-id", alert.getId());

        // Then
        verify(alertRepository).delete(alert);
    }

    @Test
    void deactivateAlert_ShouldDeactivateAlert() {
        // Given
        User user = createTestUser();
        Instrument instrument = createTestInstrument();
        PriceAlert alert = createTestAlert(user, instrument);

        when(userRepository.findByKeycloakId("test-keycloak-id")).thenReturn(Optional.of(user));
        when(alertRepository.findByIdAndUserId(alert.getId(), user.getId())).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(PriceAlert.class))).thenReturn(alert);

        // When
        alertService.deactivateAlert("test-keycloak-id", alert.getId());

        // Then
        verify(alertRepository).save(alert);
        assertThat(alert.getIsActive()).isFalse();
    }

    @Test
    void getUserAlerts_ShouldThrowWhenUserNotFound() {
        // Given
        when(userRepository.findByKeycloakId("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> alertService.getUserAlerts("unknown"))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
