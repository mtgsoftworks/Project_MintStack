package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private UUID id;
    private String symbol;
    private String instrumentName;
    private String alertType;
    private BigDecimal targetValue;
    private BigDecimal currentValueAtCreation;
    private Boolean isActive;
    private Boolean isTriggered;
    private LocalDateTime triggeredAt;
    private BigDecimal triggeredValue;
    private String notes;
    private LocalDateTime createdAt;
}
