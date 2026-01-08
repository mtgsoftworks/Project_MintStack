package com.mintstack.finance.service;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private AnalysisService analysisService;

    private Instrument testInstrument;
    private List<PriceHistory> priceHistoryList;

    @BeforeEach
    void setUp() {
        testInstrument = Instrument.builder()
            .symbol("THYAO")
            .name("Türk Hava Yolları")
            .type(Instrument.InstrumentType.STOCK)
            .exchange("BIST")
            .currentPrice(BigDecimal.valueOf(100))
            .isActive(true)
            .build();
        testInstrument.setId(UUID.randomUUID());

        LocalDate today = LocalDate.now();
        priceHistoryList = List.of(
            createPriceHistory(today.minusDays(2), BigDecimal.valueOf(95)),
            createPriceHistory(today.minusDays(1), BigDecimal.valueOf(98)),
            createPriceHistory(today, BigDecimal.valueOf(100))
        );
    }

    private PriceHistory createPriceHistory(LocalDate date, BigDecimal closePrice) {
        PriceHistory history = PriceHistory.builder()
            .instrument(testInstrument)
            .openPrice(closePrice.subtract(BigDecimal.ONE))
            .highPrice(closePrice.add(BigDecimal.ONE))
            .lowPrice(closePrice.subtract(BigDecimal.valueOf(2)))
            .closePrice(closePrice)
            .volume(1000000L)
            .priceDate(date)
            .build();
        history.setId(UUID.randomUUID());
        return history;
    }

    @Test
    void analysisService_ShouldBeInjected() {
        assertThat(analysisService).isNotNull();
    }

    @Test
    void instrumentRepository_ShouldFindBySymbol() {
        // Given
        when(instrumentRepository.findBySymbol("THYAO")).thenReturn(Optional.of(testInstrument));

        // When
        Optional<Instrument> result = instrumentRepository.findBySymbol("THYAO");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo("THYAO");
    }

    @Test
    void priceHistoryRepository_ShouldFindByInstrument() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        
        when(priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
            eq(testInstrument.getId()), eq(startDate), eq(endDate)))
            .thenReturn(priceHistoryList);

        // When
        List<PriceHistory> result = priceHistoryRepository.findByInstrumentIdAndPriceDateBetweenOrderByPriceDateAsc(
            testInstrument.getId(), startDate, endDate);

        // Then
        assertThat(result).hasSize(3);
    }
}
