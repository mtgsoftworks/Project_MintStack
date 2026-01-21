package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesResponse {
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean priceAlerts;
    private Boolean portfolioUpdates;
    private Boolean compactView;
}
