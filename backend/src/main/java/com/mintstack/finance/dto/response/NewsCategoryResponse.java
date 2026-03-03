package com.mintstack.finance.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class NewsCategoryResponse {
    UUID id;
    String name;
    String slug;
    String description;
    Integer displayOrder;
    Boolean isActive;
}
