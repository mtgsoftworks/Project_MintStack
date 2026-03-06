package com.mintstack.finance.scheduler;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.UserDataPreference.DataType;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.service.MarketDataService;
import com.mintstack.finance.service.PriceUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;

import static com.mintstack.finance.entity.UserApiConfig.ApiProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataInstrumentUpdateService Unit Tests")
class MarketDataInstrumentUpdateServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;
    @Mock
    private MarketDataService marketDataService;
    @Mock
    private PriceUpdateService priceUpdateService;
    @Mock
    private MarketDataProviderResolver providerResolver;

    @InjectMocks
    private MarketDataInstrumentUpdateService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "configuredBatchSize", 2);
        when(providerResolver.resolveDataTypeForInstrument(any(), any())).thenReturn(DataType.BIST_STOCKS);
        when(providerResolver.fetchInstrumentPrice(any(), any(), any(), any(), any(), any()))
            .thenReturn(new BigDecimal("100.00"));
        when(providerResolver.resolveLatestVolume(any())).thenReturn(1_000L);
        when(instrumentRepository.save(any(Instrument.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Each instrument type should maintain independent round-robin offset")
    void updatePricesForType_ShouldTrackOffsetsPerTypeIndependently() {
        List<Instrument> stockList = List.of(
            instrument("S1", Instrument.InstrumentType.STOCK),
            instrument("S2", Instrument.InstrumentType.STOCK),
            instrument("S3", Instrument.InstrumentType.STOCK)
        );
        List<Instrument> bondList = List.of(
            instrument("B1", Instrument.InstrumentType.BOND),
            instrument("B2", Instrument.InstrumentType.BOND),
            instrument("B3", Instrument.InstrumentType.BOND)
        );

        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulatedOrderBySymbolAsc(
            Instrument.InstrumentType.STOCK, false)).thenReturn(stockList);
        when(instrumentRepository.findByTypeAndIsActiveTrueAndIsSimulatedOrderBySymbolAsc(
            Instrument.InstrumentType.BOND, false)).thenReturn(bondList);

        service.updatePricesForType(
            Instrument.InstrumentType.STOCK,
            null,
            null,
            null,
            new EnumMap<>(DataType.class)
        );
        service.updatePricesForType(
            Instrument.InstrumentType.BOND,
            null,
            null,
            null,
            new EnumMap<>(DataType.class)
        );

        ArgumentCaptor<Instrument> instrumentCaptor = ArgumentCaptor.forClass(Instrument.class);
        verify(providerResolver, atLeast(4)).fetchInstrumentPrice(
            instrumentCaptor.capture(),
            any(),
            any(),
            any(),
            any(),
            any()
        );

        List<String> symbols = instrumentCaptor.getAllValues().stream()
            .map(Instrument::getSymbol)
            .toList();

        assertThat(symbols).containsSequence("S1", "S2", "B1", "B2");
    }

    private Instrument instrument(String symbol, Instrument.InstrumentType type) {
        return Instrument.builder()
            .symbol(symbol)
            .name(symbol)
            .type(type)
            .isActive(true)
            .isSimulated(false)
            .build();
    }
}
