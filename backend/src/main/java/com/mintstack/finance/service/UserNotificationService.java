package com.mintstack.finance.service;

import com.mintstack.finance.entity.User;
import com.mintstack.finance.entity.UserNotification;
import com.mintstack.finance.entity.UserNotification.NotificationType;
import com.mintstack.finance.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserNotificationRepository notificationRepository;
    private final ClusterWebSocketPublisher webSocketPublisher;

    @Transactional
    public UserNotification createAndDispatch(
            User user,
            NotificationType type,
            String title,
            String message,
            UUID relatedEntityId,
            String relatedEntityType) {
        UserNotification notification = UserNotification.builder()
            .user(user)
            .type(type)
            .title(title)
            .message(message)
            .isRead(false)
            .relatedEntityId(relatedEntityId)
            .relatedEntityType(relatedEntityType)
            .build();

        UserNotification saved = notificationRepository.save(notification);
        dispatch(user, saved);
        return saved;
    }

    public void dispatch(User user, UserNotification notification) {
        if (user == null || notification == null || user.getKeycloakId() == null) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notification.getId());
        payload.put("type", notification.getType().name());
        payload.put("title", notification.getTitle());
        payload.put("message", notification.getMessage());
        payload.put("isRead", Boolean.TRUE.equals(notification.getIsRead()));
        payload.put("createdAt", notification.getCreatedAt());
        payload.put("timestamp", LocalDateTime.now());

        try {
            webSocketPublisher.broadcastToUser(
                    user.getKeycloakId(),
                    "/queue/notifications",
                    payload
            );
        } catch (Exception error) {
            log.warn("Notification WebSocket dispatch failed for user {}: {}", user.getKeycloakId(), error.getMessage());
        }
    }
}
