package com.mintstack.finance.entity;

import com.mintstack.finance.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private ApiProvider provider;

    @Column(name = "api_key", nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String apiKey;

    @Column(name = "secret_key")
    @Convert(converter = EncryptedStringConverter.class)
    private String secretKey;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    public enum ApiProvider {
        YAHOO_FINANCE,
        ALPHA_VANTAGE,
        FINNHUB,
        TCMB,
        TEFAS,
        BIST_DATASTORE,
        FINTABLES,
        RSS,
        LLM_ENRICHMENT,
        OTHER
    }
}
