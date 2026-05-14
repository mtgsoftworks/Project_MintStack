package com.mintstack.finance.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record GlossaryTermResponse(
    UUID id,
    String term,
    String slug,
    String category,
    String definition,
    List<String> aliases,
    String locale,
    String sourceName,
    String sourceUrl,
    Boolean isActive,
    Integer sortOrder,
    LocalDateTime updatedAt
) {
}
