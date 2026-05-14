package com.mintstack.finance.controller;

import com.mintstack.finance.dto.request.UpsertGlossaryTermRequest;
import com.mintstack.finance.dto.response.ApiResponse;
import com.mintstack.finance.dto.response.GlossaryTermResponse;
import com.mintstack.finance.dto.response.PaginationInfo;
import com.mintstack.finance.service.GlossaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Glossary", description = "Borsa ve finans kavram sozlugu")
public class GlossaryController {

    private final GlossaryService glossaryService;

    @GetMapping("/glossary")
    @Operation(summary = "Kavram sozlugunu ara")
    public ResponseEntity<ApiResponse<List<GlossaryTermResponse>>> search(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "tr") String locale,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        Page<GlossaryTermResponse> page = glossaryService.search(query, category, locale, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent(), PaginationInfo.from(page)));
    }

    @GetMapping("/glossary/{slug}")
    @Operation(summary = "Kavram sozlugu detayini getir")
    public ResponseEntity<ApiResponse<GlossaryTermResponse>> getBySlug(
        @PathVariable String slug,
        @RequestParam(defaultValue = "tr") String locale
    ) {
        return ResponseEntity.ok(ApiResponse.success(glossaryService.getBySlug(slug, locale)));
    }

    @PostMapping("/admin/glossary")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Kavram sozlugu terimi olustur/guncelle")
    public ResponseEntity<ApiResponse<GlossaryTermResponse>> upsert(
        @Valid @RequestBody UpsertGlossaryTermRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(glossaryService.upsert(request)));
    }

    @PutMapping("/admin/glossary/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Kavram sozlugu terimini guncelle")
    public ResponseEntity<ApiResponse<GlossaryTermResponse>> update(
        @PathVariable UUID id,
        @Valid @RequestBody UpsertGlossaryTermRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(glossaryService.update(id, request)));
    }

    @DeleteMapping("/admin/glossary/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Kavram sozlugu terimini pasife al")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        glossaryService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "Terim pasife alindi"));
    }
}
