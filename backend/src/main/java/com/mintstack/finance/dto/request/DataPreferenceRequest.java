package com.mintstack.finance.dto.request;

import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DataPreferenceRequest {
    
    @NotNull(message = "Data type is required")
    private DataType dataType;
    
    @NotNull(message = "Provider is required")
    private ApiProvider provider;
    
    private Boolean isEnabled = true;
}
