package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWatchlistItemRequest {

    @Size(max = 1000, message = "Item notu en fazla 1000 karakter olabilir")
    private String notes;
}
