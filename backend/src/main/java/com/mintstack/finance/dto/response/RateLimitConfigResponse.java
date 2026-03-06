package com.mintstack.finance.dto.response;

import lombok.Builder;

@Builder
public record RateLimitConfigResponse(
    boolean enabled,
    int anonymousRequestsPerMinute,
    int authenticatedRequestsPerMinute,
    int adminRequestsPerMinute,
    int bucketCount
) {
}
