package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalıdır")
    private String firstName;

    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalıdır")
    private String lastName;

    private String phoneNumber;

    @Size(max = 500, message = "Bio en fazla 500 karakter olabilir")
    private String bio;

    private String location;

    private Boolean emailNotifications;

    private Boolean pushNotifications;

    private Boolean priceAlerts;

    private Boolean portfolioUpdates;

    private Boolean compactView;
}
