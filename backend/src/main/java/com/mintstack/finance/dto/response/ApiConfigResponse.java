package com.mintstack.finance.dto.response;

import com.mintstack.finance.entity.UserApiConfig;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApiConfigResponse {
    private UUID id;
    private UserApiConfig.ApiProvider provider;
    private String apiKey; // Masked in production ideally
    private String baseUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
