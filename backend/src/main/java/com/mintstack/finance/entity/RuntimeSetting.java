package com.mintstack.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "runtime_settings", indexes = {
    @Index(name = "idx_runtime_settings_key", columnList = "setting_key")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_runtime_settings_key", columnNames = "setting_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuntimeSetting extends BaseEntity {

    @NotBlank
    @Column(name = "setting_key", nullable = false, length = 160)
    private String key;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "restart_required", nullable = false)
    @Builder.Default
    private Boolean restartRequired = false;

    @Column(name = "updated_by", length = 160)
    private String updatedBy;
}
