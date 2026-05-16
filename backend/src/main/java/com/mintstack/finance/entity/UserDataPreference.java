package com.mintstack.finance.entity;

import com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_data_preferences", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "data_type"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDataPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private DataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private ApiProvider provider;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DataType {
        CURRENCY_RATES,
        BIST_STOCKS,
        BIST_INDICES,
        BONDS,
        VIOP,
        US_STOCKS,
        FUNDS,
        CRYPTO,
        NEWS,
        TECHNICAL_INDICATORS,
        GLOSSARY
    }
}
