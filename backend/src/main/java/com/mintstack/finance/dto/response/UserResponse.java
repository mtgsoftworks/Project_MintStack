package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    
    private String email;
    
    private String firstName;
    
    private String lastName;
    
    private String fullName;
    
    private Boolean isActive;
    
    private int portfolioCount;
    
    private LocalDateTime createdAt;
}
