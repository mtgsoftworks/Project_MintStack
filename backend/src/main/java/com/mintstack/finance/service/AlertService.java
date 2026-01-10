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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final PriceAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;
    private final EmailService emailService;
    private final PriceUpdateService priceUpdateService;

    private static final int MAX_ACTIVE_ALERTS_PER_USER = 20;

    @Transactional(readOnly = true)
    public List<AlertResponse> getUserAlerts(String keycloakId) {
        User user = findUserByKeycloakId(keycloakId);
        return alertRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getActiveAlerts(String keycloakId) {
        User user = findUserByKeycloakId(keycloakId);
        return alertRepository.findByUserIdAndIsActiveTrue(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AlertResponse createAlert(String keycloakId, CreateAlertRequest request) {
        User user = findUserByKeycloakId(keycloakId);

        // Check max alerts limit
        long activeCount = alertRepository.countByUserIdAndIsActiveTrue(user.getId());
        if (activeCount >= MAX_ACTIVE_ALERTS_PER_USER) {
            throw new BusinessException("Maksimum aktif alarm sayısına ulaştınız (" + MAX_ACTIVE_ALERTS_PER_USER + ")");
        }

        Instrument instrument = instrumentRepository.findBySymbol(request.getSymbol())
                .orElseThrow(() -> new ResourceNotFoundException("Enstrüman", "symbol", request.getSymbol()));

        PriceAlert alert = PriceAlert.builder()
                .user(user)
                .instrument(instrument)
                .alertType(request.getAlertType())
                .targetValue(request.getTargetValue())
                .currentValueAtCreation(instrument.getCurrentPrice())
                .notes(request.getNotes())
                .build();

        alert = alertRepository.save(alert);
        log.info("Created {} alert for {} at {} for user {}", 
                request.getAlertType(), request.getSymbol(), request.getTargetValue(), keycloakId);
        
        return mapToResponse(alert);
    }

    @Transactional
    public void deleteAlert(String keycloakId, UUID alertId) {
        User user = findUserByKeycloakId(keycloakId);
        PriceAlert alert = alertRepository.findByIdAndUserId(alertId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Alarm", "id", alertId));

        alertRepository.delete(alert);
        log.info("Deleted alert {} for user {}", alertId, keycloakId);
    }

    @Transactional
    public void deactivateAlert(String keycloakId, UUID alertId) {
        User user = findUserByKeycloakId(keycloakId);
        PriceAlert alert = alertRepository.findByIdAndUserId(alertId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Alarm", "id", alertId));

        alert.setIsActive(false);
        alertRepository.save(alert);
        log.info("Deactivated alert {} for user {}", alertId, keycloakId);
    }

    /**
     * Check all active alerts for a specific symbol and trigger if conditions are met
     */
    @Transactional
    public void checkAlertsForSymbol(String symbol, BigDecimal currentPrice) {
        List<PriceAlert> activeAlerts = alertRepository.findActiveAlertsBySymbol(symbol);

        for (PriceAlert alert : activeAlerts) {
            if (alert.shouldTrigger(currentPrice)) {
                triggerAlert(alert, currentPrice);
            }
        }
    }

    /**
     * Trigger an alert and send notification
     */
    private void triggerAlert(PriceAlert alert, BigDecimal currentPrice) {
        alert.trigger(currentPrice);
        alertRepository.save(alert);

        User user = alert.getUser();
        Instrument instrument = alert.getInstrument();

        log.info("Alert triggered: {} {} at {} (target: {})", 
                alert.getAlertType(), instrument.getSymbol(), currentPrice, alert.getTargetValue());

        // Send email notification
        emailService.sendPriceAlertEmail(
                user.getEmail(),
                user.getFirstName() != null ? user.getFirstName() : user.getEmail(),
                instrument.getSymbol(),
                getAlertTypeLabel(alert.getAlertType()),
                alert.getTargetValue().toString(),
                currentPrice.toString()
        ).thenAccept(success -> {
            if (success) {
                alert.setNotificationSent(true);
                alertRepository.save(alert);
            }
        });
    }

    private String getAlertTypeLabel(AlertType type) {
        return switch (type) {
            case PRICE_ABOVE -> "Fiyat Üstünde";
            case PRICE_BELOW -> "Fiyat Altında";
            case PERCENT_UP -> "Yüzde Artış";
            case PERCENT_DOWN -> "Yüzde Düşüş";
        };
    }

    private User findUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", "keycloakId", keycloakId));
    }

    private AlertResponse mapToResponse(PriceAlert alert) {
        Instrument inst = alert.getInstrument();
        return AlertResponse.builder()
                .id(alert.getId())
                .symbol(inst.getSymbol())
                .instrumentName(inst.getName())
                .alertType(alert.getAlertType().name())
                .targetValue(alert.getTargetValue())
                .currentValueAtCreation(alert.getCurrentValueAtCreation())
                .isActive(alert.getIsActive())
                .isTriggered(alert.getIsTriggered())
                .triggeredAt(alert.getTriggeredAt())
                .triggeredValue(alert.getTriggeredValue())
                .notes(alert.getNotes())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
