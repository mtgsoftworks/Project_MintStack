package com.mintstack.finance.dto.request;

import com.mintstack.finance.entity.Instrument.InstrumentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class HistoricalDataBackfillRequest {

    private List<InstrumentType> instrumentTypes;

    private List<String> symbols;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(value = 1, message = "Gun sayisi en az 1 olmalidir")
    @Max(value = 365, message = "Gun sayisi en fazla 365 olmalidir")
    private Integer days;

    @Min(value = 1, message = "Enstruman limiti en az 1 olmalidir")
    @Max(value = 500, message = "Enstruman limiti en fazla 500 olmalidir")
    private Integer maxInstruments;

    private Boolean includeSyntheticFallback;
}
