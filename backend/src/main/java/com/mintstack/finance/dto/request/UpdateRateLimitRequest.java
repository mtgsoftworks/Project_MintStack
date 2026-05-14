package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRateLimitRequest {

    private Boolean enabled;

    @Min(value = 1, message = "anonymousRequestsPerMinute en az 1 olmalidir")
    private Integer anonymousRequestsPerMinute;

    @Min(value = 1, message = "authenticatedRequestsPerMinute en az 1 olmalidir")
    private Integer authenticatedRequestsPerMinute;

    @Min(value = 1, message = "adminRequestsPerMinute en az 1 olmalidir")
    private Integer adminRequestsPerMinute;

    @Pattern(regexp = "redis|memory", flags = Pattern.Flag.CASE_INSENSITIVE, message = "store redis veya memory olmali")
    private String store;

    private Boolean clearBuckets;
}
