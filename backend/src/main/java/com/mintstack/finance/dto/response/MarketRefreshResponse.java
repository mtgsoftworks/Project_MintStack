package com.mintstack.finance.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MarketRefreshResponse(
    List<String> requestedDataTypes,
    List<String> refreshedDataTypes,
    List<String> skippedDataTypes,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {
}
