package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWatchlistRequest {

    @NotBlank(message = "Watchlist adı boş olamaz")
    @Size(min = 1, max = 100, message = "Watchlist adı 1-100 karakter arasında olmalıdır")
    private String name;

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
    private String description;
}
