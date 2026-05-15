package com.mintstack.finance.service.market;

import com.mintstack.finance.dto.request.HistoricalDataBackfillRequest;
import com.mintstack.finance.dto.response.HistoricalDataBackfillResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.external.TcmbApiClient;
import com.mintstack.finance.service.external.TefasFundClient;
import com.mintstack.finance.service.external.YahooFinanceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalDataBackfillServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private YahooFinanceClient yahooFinanceClient;

    @Mock
    private TefasFundClient tefasFundClient;

    @Mock
    private TcmbApiClient tcmbApiClient;

    @InjectMocks
    private HistoricalDataBackfillService service;

    @Test
    void backfill_ShouldRetryPriceHistorySave_WhenOptimisticLockConflictOccurs() {
        Instrument instrument = Instrument.builder()
            .symbol("THYAO")
            .name("Turk Hava Yollari")
            .type(Instrument.InstrumentType.STOCK)
            .currentPrice(new BigDecimal("300.00"))
            .isActive(true)
            .isSimulated(false)
            .build();
        instrument.setId(UUID.randomUUID());

        PriceHistory history = PriceHistory.builder()
            .instrument(instrument)
            .priceDate(LocalDate.of(2026, 5, 10))
            .closePrice(new BigDecimal("305.00"))
            .build();

        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulatedOrderBySymbolAsc(
            Instrument.InstrumentType.STOCK,
            false
        )).thenReturn(List.of(instrument));
        when(yahooFinanceClient.fetchHistoricalData(
            eq("THYAO"),
            eq(LocalDate.of(2026, 5, 10)),
            eq(LocalDate.of(2026, 5, 11)),
            any(),
            any()
        )).thenReturn(List.of(history));
        doThrow(new OptimisticLockingFailureException("concurrent update"))
            .doNothing()
            .when(marketDataService)
            .savePriceHistory(history);

        HistoricalDataBackfillResponse response = service.backfill(HistoricalDataBackfillRequest.builder()
            .startDate(LocalDate.of(2026, 5, 10))
            .endDate(LocalDate.of(2026, 5, 10))
            .instrumentTypes(List.of(Instrument.InstrumentType.STOCK))
            .maxInstruments(1)
            .includeSyntheticFallback(true)
            .build());

        assertThat(response.savedPriceRows()).isEqualTo(1);
        verify(marketDataService, times(2)).savePriceHistory(history);
    }
}
