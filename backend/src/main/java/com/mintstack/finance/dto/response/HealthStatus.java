package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatus {
    
    private boolean simulationEnabled;
    private boolean redisConnected;
    private boolean websocketConnected;
    private boolean schedulerRunning;
    
    private String status;

    public static String calculateStatus(boolean simulationEnabled, boolean redisConnected, 
                                          boolean websocketConnected, boolean schedulerRunning) {
        if (!schedulerRunning) {
            return "UNHEALTHY";
        }
        
        if (!redisConnected || !websocketConnected) {
            return "DEGRADED";
        }
        
        return "HEALTHY";
    }
}
