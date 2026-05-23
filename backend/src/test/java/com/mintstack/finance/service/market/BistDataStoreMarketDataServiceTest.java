package com.mintstack.finance.service.market;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import com.mintstack.finance.service.PriceUpdateService;
import com.mintstack.finance.service.external.BistDataStoreClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BistDataStoreMarketDataServiceTest {

    @Mock
    private BistDataStoreClient bistDataStoreClient;

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private PriceUpdateService priceUpdateService;

    private BistDataStoreMarketDataService service;

    @BeforeEach
    void setUp() {
        service = new BistDataStoreMarketDataService(
            bistDataStoreClient,
            instrumentRepository,
            priceHistoryRepository,
            priceUpdateService
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "latestLookbackDays", 1);
        ReflectionTestUtils.setField(service, "maturityLookbackDays", 1);

        when(instrumentRepository.save(any(Instrument.class))).thenAnswer(invocation -> {
            Instrument instrument = invocation.getArgument(0);
            if (instrument.getId() == null) {
                instrument.setId(UUID.randomUUID());
            }
            return instrument;
        });
        when(priceHistoryRepository.findByInstrumentIdAndPriceDate(any(UUID.class), any(LocalDate.class)))
            .thenReturn(Optional.empty());
        when(priceHistoryRepository.save(any(PriceHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulatedOrderBySymbolAsc(
            eq(Instrument.InstrumentType.BOND),
            eq(false)
        )).thenReturn(List.of());
        lenient().when(bistDataStoreClient.fetchBondMaturityHints(any(LocalDate.class))).thenReturn(Map.of());
    }

    @Test
    void refreshBondPrices_ShouldPersistMaturityDateFromBistDataStore() {
        LocalDate tradeDate = LocalDate.of(2026, 5, 15);
        LocalDate maturityDate = LocalDate.of(2026, 7, 22);
        BistDataStoreClient.BistBondPrice bondPrice = new BistDataStoreClient.BistBondPrice(
            "TRDABVK72615",
            "TRDABVK72615",
            tradeDate,
            "TRY",
            new BigDecimal("99.62"),
            new BigDecimal("99.12"),
            maturityDate,
            new BigDecimal("99.70"),
            new BigDecimal("99.10"),
            new BigDecimal("100.00"),
            new BigDecimal("25800000"),
            new BigDecimal("28724736.19")
        );

        when(bistDataStoreClient.fetchBondPrices(any(LocalDate.class))).thenReturn(List.of(bondPrice));
        when(instrumentRepository.findBySymbol(eq("TRDABVK72615"))).thenReturn(Optional.empty());

        int processed = service.refreshBondPrices();

        assertThat(processed).isEqualTo(1);
        ArgumentCaptor<Instrument> instrumentCaptor = ArgumentCaptor.forClass(Instrument.class);
        verify(instrumentRepository).save(instrumentCaptor.capture());
        assertThat(instrumentCaptor.getValue().getMaturityDate()).isEqualTo(maturityDate);
        verify(priceUpdateService).broadcastMarketUpdate(
            eq("BOND"),
            eq("TRDABVK72615"),
            eq(new BigDecimal("99.62")),
            any()
        );
    }

    @Test
    void refreshViopPrices_ShouldDerivePreviousSettlementWhenMissing() {
        LocalDate tradeDate = LocalDate.of(2026, 5, 15);
        BistDataStoreClient.BistViopPrice viopPrice = new BistDataStoreClient.BistViopPrice(
            "F_TEST0526",
            "TEST_05/2026_VIS",
            tradeDate,
            "TRY",
            new BigDecimal("95.00"),
            null,
            new BigDecimal("96.00"),
            new BigDecimal("94.00"),
            new BigDecimal("97.00"),
            new BigDecimal("95.20"),
            new BigDecimal("-5.00"),
            LocalDate.of(2026, 5, 29),
            new BigDecimal("12345"),
            new BigDecimal("1172775")
        );

        when(bistDataStoreClient.fetchViopPrices(any(LocalDate.class))).thenReturn(List.of(viopPrice));
        when(instrumentRepository.findBySymbol(eq("F_TEST0526"))).thenReturn(Optional.empty());

        int processed = service.refreshViopPrices();

        assertThat(processed).isEqualTo(1);
        ArgumentCaptor<Instrument> instrumentCaptor = ArgumentCaptor.forClass(Instrument.class);
        verify(instrumentRepository).save(instrumentCaptor.capture());
        assertThat(instrumentCaptor.getValue().getPreviousClose()).isEqualByComparingTo("100.000000");
        assertThat(instrumentCaptor.getValue().getMaturityDate()).isEqualTo(LocalDate.of(2026, 5, 29));
        verify(priceUpdateService).broadcastMarketUpdate(
            eq("VIOP"),
            eq("F_TEST0526"),
            eq(new BigDecimal("95.00")),
            any()
        );
    }
}
