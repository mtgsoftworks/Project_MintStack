package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private Long totalUsers;
    private Long activeUsers;
    private Long totalPortfolios;
    private Long totalInstruments;
    private Long activeInstruments;
    private Long totalNews;
    private Long totalCurrencyRates;
    private Integer activeAlerts;
    private Long totalWatchlists;
    private LocalDateTime lastUpdated;
}
