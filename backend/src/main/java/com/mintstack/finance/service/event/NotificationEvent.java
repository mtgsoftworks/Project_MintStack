package com.mintstack.finance.service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String id;
    private LocalDateTime timestamp;
    private String userId;
    private String type;
    private String title;
    private String message;
    private Map<String, Object> metadata;
}
