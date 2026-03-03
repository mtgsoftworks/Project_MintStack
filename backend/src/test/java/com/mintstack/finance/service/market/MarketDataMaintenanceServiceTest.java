package com.mintstack.finance.service.market;

import com.mintstack.finance.entity.CurrencyRate;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.CurrencyRateRepository;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.NewsRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataMaintenanceServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private NewsRepository newsRepository;

    @InjectMocks
    private MarketDataMaintenanceService marketDataMaintenanceService;

    @Test
    void saveCurrencyRates_ShouldPersistAllRates() {
        List<CurrencyRate> rates = List.of(CurrencyRate.builder().currencyCode("USD").build());

        marketDataMaintenanceService.saveCurrencyRates(rates);

        verify(currencyRateRepository).saveAll(rates);
    }

    @Test
    void updateInstrumentPrice_ShouldUpdateCurrentAndPreviousClose() {
        Instrument instrument = Instrument.builder()
            .symbol("THYAO")
            .currentPrice(new BigDecimal("100"))
            .previousClose(new BigDecimal("95"))
            .build();
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(instrument));

        marketDataMaintenanceService.updateInstrumentPrice("THYAO", new BigDecimal("110"));

        assertThat(instrument.getPreviousClose()).isEqualByComparingTo("100");
        assertThat(instrument.getCurrentPrice()).isEqualByComparingTo("110");
        verify(instrumentRepository).save(instrument);
    }

    @Test
    void updateInstrumentPrice_ShouldThrow_WhenInstrumentNotFound() {
        when(instrumentRepository.findBySymbol("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketDataMaintenanceService.updateInstrumentPrice("MISSING", BigDecimal.TEN))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void savePriceHistory_ShouldMergeExistingEntry() {
        UUID instrumentId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 3, 3);

        Instrument instrument = Instrument.builder().symbol("THYAO").build();
        instrument.setId(instrumentId);
        PriceHistory existing = PriceHistory.builder()
            .instrument(instrument)
            .priceDate(date)
            .openPrice(new BigDecimal("90"))
            .highPrice(new BigDecimal("95"))
            .lowPrice(new BigDecimal("85"))
            .closePrice(new BigDecimal("92"))
            .adjustedClose(new BigDecimal("92"))
            .volume(10L)
            .build();

        PriceHistory incoming = PriceHistory.builder()
            .instrument(instrument)
            .priceDate(date)
            .openPrice(new BigDecimal("100"))
            .highPrice(new BigDecimal("110"))
            .lowPrice(new BigDecimal("99"))
            .closePrice(new BigDecimal("108"))
            .adjustedClose(new BigDecimal("108"))
            .volume(500L)
            .build();

        when(priceHistoryRepository.findByInstrumentIdAndPriceDate(instrumentId, date))
            .thenReturn(Optional.of(existing));

        marketDataMaintenanceService.savePriceHistory(incoming);

        assertThat(existing.getOpenPrice()).isEqualByComparingTo("100");
        assertThat(existing.getHighPrice()).isEqualByComparingTo("110");
        assertThat(existing.getLowPrice()).isEqualByComparingTo("99");
        assertThat(existing.getClosePrice()).isEqualByComparingTo("108");
        assertThat(existing.getAdjustedClose()).isEqualByComparingTo("108");
        assertThat(existing.getVolume()).isEqualTo(500L);
        verify(priceHistoryRepository).save(existing);
    }

    @Test
    void deleteAllMarketData_ShouldDeleteDataAndDeactivateRealInstruments() {
        Instrument realStock = Instrument.builder()
            .symbol("THYAO")
            .type(Instrument.InstrumentType.STOCK)
            .isSimulated(false)
            .isActive(true)
            .build();
        Instrument realIndex = Instrument.builder()
            .symbol("XU100")
            .type(Instrument.InstrumentType.INDEX)
            .isSimulated(false)
            .isActive(true)
            .build();
        Instrument simulatedStock = Instrument.builder()
            .symbol("SIM")
            .type(Instrument.InstrumentType.STOCK)
            .isSimulated(true)
            .isActive(true)
            .build();

        when(currencyRateRepository.count()).thenReturn(12L);
        when(priceHistoryRepository.count()).thenReturn(34L);
        when(newsRepository.count()).thenReturn(7L);
        when(instrumentRepository.findAll()).thenReturn(List.of(realStock, realIndex, simulatedStock));

        Map<String, Object> result = marketDataMaintenanceService.deleteAllMarketData();

        assertThat(result.get("deletedCurrencyRates")).isEqualTo(12L);
        assertThat(result.get("deletedPriceHistory")).isEqualTo(34L);
        assertThat(result.get("deletedNews")).isEqualTo(7L);
        assertThat(result.get("deactivatedRealInstruments")).isEqualTo(2L);
        assertThat(result.get("deactivatedIndices")).isEqualTo(1L);

        ArgumentCaptor<List<Instrument>> captor = ArgumentCaptor.forClass(List.class);
        verify(instrumentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(realStock.getIsActive()).isFalse();
        assertThat(realIndex.getIsActive()).isFalse();

        verify(currencyRateRepository).deleteAll();
        verify(priceHistoryRepository).deleteAll();
        verify(newsRepository).deleteAllInBatch();
    }
}
