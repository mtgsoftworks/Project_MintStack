package com.mintstack.finance.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record RuntimeSettingResponse(
    UUID id,
    String key,
    String value,
    String description,
    Boolean restartRequired,
    String updatedBy,
    LocalDateTime updatedAt
) {
}
