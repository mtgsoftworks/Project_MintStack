package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWatchlistRequest {

    @NotBlank(message = "Watchlist adi bos olamaz")
    @Size(min = 1, max = 100, message = "Watchlist adi 1-100 karakter arasinda olmalidir")
    private String name;

    @Size(max = 500, message = "Aciklama en fazla 500 karakter olabilir")
    private String description;

    @Size(max = 50, message = "Etiket en fazla 50 karakter olabilir")
    private String tag;

    @Size(max = 2000, message = "Not en fazla 2000 karakter olabilir")
    private String notes;

    private List<String> columnPreferences;
}
