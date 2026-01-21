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
public class LogEvent {
    private String id;
    private LocalDateTime timestamp;
    private String level;
    private String message;
    private String logger;
    private String service;
    private String traceId;
    private String spanId;
    private Map<String, Object> context;
}
