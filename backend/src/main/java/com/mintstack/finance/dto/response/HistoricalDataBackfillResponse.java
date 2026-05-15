package com.mintstack.finance.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record HistoricalDataBackfillResponse(
    LocalDate startDate,
    LocalDate endDate,
    int requestedDays,
    int processedInstruments,
    int savedPriceRows,
    int savedCurrencyRows,
    int skippedInstruments,
    List<String> warnings,
    Map<String, Integer> rowsByType
) {
}
