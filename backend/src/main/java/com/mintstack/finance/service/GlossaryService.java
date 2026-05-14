package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.UpsertGlossaryTermRequest;
import com.mintstack.finance.dto.response.GlossaryTermResponse;
import com.mintstack.finance.entity.GlossaryTerm;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.GlossaryTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GlossaryService {

    private final GlossaryTermRepository glossaryTermRepository;

    @Transactional(readOnly = true)
    public Page<GlossaryTermResponse> search(String query, String category, String locale, Pageable pageable) {
        String normalizedLocale = normalizeLocale(locale);
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : "";
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : "";
        boolean queryEnabled = StringUtils.hasText(normalizedQuery);
        boolean categoryEnabled = StringUtils.hasText(normalizedCategory);
        boolean localeEnabled = StringUtils.hasText(normalizedLocale);
        return glossaryTermRepository
            .searchActive(
                normalizedQuery,
                queryEnabled,
                normalizedCategory,
                categoryEnabled,
                normalizedLocale,
                localeEnabled,
                pageable
            )
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public GlossaryTermResponse getBySlug(String slug, String locale) {
        return glossaryTermRepository.findBySlugAndLocaleAndIsActiveTrue(slug, normalizeLocale(locale))
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Sozluk terimi", "slug", slug));
    }

    @Transactional
    public GlossaryTermResponse upsert(UpsertGlossaryTermRequest request) {
        String locale = normalizeLocale(request.getLocale());
        String slug = StringUtils.hasText(request.getSlug()) ? slugify(request.getSlug()) : slugify(request.getTerm());
        GlossaryTerm term = glossaryTermRepository.findBySlugAndLocale(slug, locale)
            .orElseGet(GlossaryTerm::new);

        term.setTerm(request.getTerm().trim());
        term.setSlug(slug);
        term.setCategory(request.getCategory().trim());
        term.setDefinition(request.getDefinition().trim());
        term.setAliases(joinAliases(request.getAliases()));
        term.setLocale(locale);
        term.setSourceName(request.getSourceName());
        term.setSourceUrl(request.getSourceUrl());
        term.setIsActive(request.getIsActive() == null || Boolean.TRUE.equals(request.getIsActive()));
        term.setSortOrder(request.getSortOrder() == null ? 100 : request.getSortOrder());
        return toResponse(glossaryTermRepository.save(term));
    }

    @Transactional
    public GlossaryTermResponse update(UUID id, UpsertGlossaryTermRequest request) {
        GlossaryTerm term = glossaryTermRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sozluk terimi", "id", id));
        String locale = normalizeLocale(request.getLocale());
        term.setTerm(request.getTerm().trim());
        term.setSlug(StringUtils.hasText(request.getSlug()) ? slugify(request.getSlug()) : slugify(request.getTerm()));
        term.setCategory(request.getCategory().trim());
        term.setDefinition(request.getDefinition().trim());
        term.setAliases(joinAliases(request.getAliases()));
        term.setLocale(locale);
        term.setSourceName(request.getSourceName());
        term.setSourceUrl(request.getSourceUrl());
        term.setIsActive(request.getIsActive() == null || Boolean.TRUE.equals(request.getIsActive()));
        term.setSortOrder(request.getSortOrder() == null ? 100 : request.getSortOrder());
        return toResponse(glossaryTermRepository.save(term));
    }

    @Transactional
    public void deactivate(UUID id) {
        GlossaryTerm term = glossaryTermRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sozluk terimi", "id", id));
        term.setIsActive(false);
        glossaryTermRepository.save(term);
    }

    private GlossaryTermResponse toResponse(GlossaryTerm term) {
        return GlossaryTermResponse.builder()
            .id(term.getId())
            .term(term.getTerm())
            .slug(term.getSlug())
            .category(term.getCategory())
            .definition(term.getDefinition())
            .aliases(splitAliases(term.getAliases()))
            .locale(term.getLocale())
            .sourceName(term.getSourceName())
            .sourceUrl(term.getSourceUrl())
            .isActive(term.getIsActive())
            .sortOrder(term.getSortOrder())
            .updatedAt(term.getUpdatedAt())
            .build();
    }

    private String normalizeLocale(String locale) {
        return StringUtils.hasText(locale) ? locale.trim().toLowerCase() : "tr";
    }

    private String joinAliases(List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return null;
        }
        return aliases.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .reduce((left, right) -> left + "," + right)
            .orElse(null);
    }

    private List<String> splitAliases(String aliases) {
        if (!StringUtils.hasText(aliases)) {
            return List.of();
        }
        return Arrays.stream(aliases.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase()
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "term" : normalized;
    }
}
