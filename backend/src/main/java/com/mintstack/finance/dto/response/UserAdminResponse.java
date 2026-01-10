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
public class UserAdminResponse {

    private UUID id;
    private String keycloakId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private Integer portfolioCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
