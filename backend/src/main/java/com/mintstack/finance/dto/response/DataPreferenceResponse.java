package com.mintstack.finance.dto.response;

import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class DataPreferenceResponse {
    private UUID id;
    private DataType dataType;
    private ApiProvider provider;
    private Boolean isEnabled;
    private String dataTypeLabel;
    private String providerLabel;
}
