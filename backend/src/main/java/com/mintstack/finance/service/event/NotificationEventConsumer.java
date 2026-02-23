package com.mintstack.finance.service.event;

import com.mintstack.finance.config.KafkaConfig;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserNotification;
import com.mintstack.finance.entity.UserNotification.NotificationType;
import com.mintstack.finance.repository.UserNotificationRepository;
import com.mintstack.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEventConsumer {

    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_NOTIFICATIONS,
            groupId = "notification-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeNotification(NotificationEvent event) {
        log.info("Received notification event: {} for user {}", event.getType(), event.getUserId());

        try {
            // Find user by keycloakId
            Optional<User> userOpt = userRepository.findByKeycloakId(event.getUserId());
            
            if (userOpt.isEmpty()) {
                log.warn("User not found for notification: {}", event.getUserId());
                return;
            }

            User user = userOpt.get();

            // Check user notification preferences
            if (!shouldSendNotification(user, event.getType())) {
                log.debug("User {} has disabled notifications for type {}", user.getId(), event.getType());
                return;
            }

            // Save notification to database
            NotificationType notificationType = parseNotificationType(event.getType());
            UserNotification notification = UserNotification.builder()
                    .user(user)
                    .type(notificationType)
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);

            // Send real-time notification via WebSocket
            messagingTemplate.convertAndSendToUser(
                    event.getUserId(),
                    "/queue/notifications",
                    Map.of(
                            "id", notification.getId(),
                            "type", event.getType(),
                            "title", event.getTitle(),
                            "message", event.getMessage(),
                            "timestamp", event.getTimestamp()
                    )
            );

            log.info("Notification saved and sent to user {}", user.getId());

        } catch (Exception e) {
            log.error("Failed to process notification event: {}", e.getMessage(), e);
        }
    }

    private boolean shouldSendNotification(User user, String type) {
        return switch (type) {
            case "PRICE_ALERT", "ALERT" -> Boolean.TRUE.equals(user.getPriceAlerts());
            case "PORTFOLIO_UPDATE", "PORTFOLIO" -> Boolean.TRUE.equals(user.getPortfolioUpdates());
            case "EMAIL" -> Boolean.TRUE.equals(user.getEmailNotifications());
            case "PUSH" -> Boolean.TRUE.equals(user.getPushNotifications());
            default -> true;
        };
    }

    private NotificationType parseNotificationType(String type) {
        return switch (type.toUpperCase()) {
            case "PRICE_ALERT", "ALERT" -> NotificationType.ALERT;
            case "PORTFOLIO_UPDATE", "PORTFOLIO" -> NotificationType.PORTFOLIO;
            case "NEWS" -> NotificationType.NEWS;
            default -> NotificationType.SYSTEM;
        };
    }
}
