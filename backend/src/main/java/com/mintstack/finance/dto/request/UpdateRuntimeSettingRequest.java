package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRuntimeSettingRequest {

    @NotBlank
    private String value;

    @Size(max = 500)
    private String description;

    private Boolean restartRequired;
}
