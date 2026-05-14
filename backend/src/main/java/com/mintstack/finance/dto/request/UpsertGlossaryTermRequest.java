package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpsertGlossaryTermRequest {

    @NotBlank
    @Size(max = 160)
    private String term;

    @Size(max = 180)
    private String slug;

    @NotBlank
    @Size(max = 80)
    private String category;

    @NotBlank
    private String definition;

    private List<String> aliases;

    @Size(max = 8)
    private String locale = "tr";

    @Size(max = 160)
    private String sourceName;

    @Size(max = 500)
    private String sourceUrl;

    private Boolean isActive = true;

    private Integer sortOrder = 100;
}
