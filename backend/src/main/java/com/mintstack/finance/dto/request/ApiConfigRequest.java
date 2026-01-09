package com.mintstack.finance.dto.request;

import com.mintstack.finance.entity.UserApiConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApiConfigRequest {
    @NotNull(message = "Provider cannot be null")
    private UserApiConfig.ApiProvider provider;

    @NotBlank(message = "API Key cannot be blank")
    private String apiKey;

    private String secretKey;
    private String baseUrl;
    private Boolean isActive = true;
}
