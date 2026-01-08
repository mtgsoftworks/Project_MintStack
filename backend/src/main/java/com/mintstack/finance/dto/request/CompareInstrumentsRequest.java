package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareInstrumentsRequest {

    @NotEmpty(message = "En az bir enstrüman seçmelisiniz")
    @Size(min = 1, max = 5, message = "En fazla 5 enstrüman karşılaştırabilirsiniz")
    private List<String> symbols;

    @NotNull(message = "Başlangıç tarihi zorunludur")
    private LocalDate startDate;

    @NotNull(message = "Bitiş tarihi zorunludur")
    private LocalDate endDate;
}
