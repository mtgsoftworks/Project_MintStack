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
@Table(name = "glossary_terms", indexes = {
    @Index(name = "idx_glossary_terms_slug", columnList = "slug"),
    @Index(name = "idx_glossary_terms_category", columnList = "category"),
    @Index(name = "idx_glossary_terms_locale", columnList = "locale"),
    @Index(name = "idx_glossary_terms_active", columnList = "is_active")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_glossary_terms_slug_locale", columnNames = {"slug", "locale"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlossaryTerm extends BaseEntity {

    @NotBlank
    @Column(name = "term", nullable = false, length = 160)
    private String term;

    @NotBlank
    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @NotBlank
    @Column(name = "category", nullable = false, length = 80)
    private String category;

    @NotBlank
    @Column(name = "definition", nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(name = "aliases", length = 1000)
    private String aliases;

    @Column(name = "locale", nullable = false, length = 8)
    @Builder.Default
    private String locale = "tr";

    @Column(name = "source_name", length = 160)
    private String sourceName;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 100;
}
