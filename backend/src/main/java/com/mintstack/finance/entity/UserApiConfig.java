package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_api_configs", indexes = {
    @Index(name = "idx_user_api_configs_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserApiConfig extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private ApiProvider provider;

    @Column(name = "api_key", nullable = false)
    private String apiKey;

    @Column(name = "secret_key")
    private String secretKey;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    public enum ApiProvider {
        YAHOO_FINANCE,
        ALPHA_VANTAGE,
        FINNHUB,
        TCMB,
        OTHER
    }
}
