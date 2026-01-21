package com.mintstack.finance.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferencesRequest {

    private Boolean emailNotifications;

    private Boolean pushNotifications;

    private Boolean priceAlerts;

    private Boolean portfolioUpdates;

    private Boolean compactView;
}
