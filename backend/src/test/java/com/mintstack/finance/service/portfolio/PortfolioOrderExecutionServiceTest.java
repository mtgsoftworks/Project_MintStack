package com.mintstack.finance.service.portfolio;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Portfolio;
import com.mintstack.finance.entity.PortfolioItem;
import com.mintstack.finance.entity.PortfolioTransaction;
import com.mintstack.finance.entity.PriceHistory;
import com.mintstack.finance.repository.PortfolioItemRepository;
import com.mintstack.finance.repository.PortfolioRepository;
import com.mintstack.finance.repository.PortfolioTransactionRepository;
import com.mintstack.finance.repository.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioOrderExecutionServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioItemRepository portfolioItemRepository;

    @Mock
    private PortfolioTransactionRepository portfolioTransactionRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private PortfolioFinancialRulesService financialRulesService;

    private PortfolioOrderExecutionService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioOrderExecutionService(
            portfolioRepository,
            portfolioItemRepository,
            portfolioTransactionRepository,
            priceHistoryRepository,
            financialRulesService
        );

        lenient().when(financialRulesService.calculateCommission(any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);
        lenient().when(portfolioRepository.save(any(Portfolio.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(portfolioTransactionRepository.save(any(PortfolioTransaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(portfolioItemRepository.save(any(PortfolioItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void tryFillOrder_ShouldFillStopBuy_WhenPriceCrossesStop() {
        Portfolio portfolio = createPortfolio("100000.000000");
        Instrument instrument = createInstrument("BTCUSDT", Instrument.InstrumentType.CRYPTO, "105.000000");
        stubHighVolume(instrument, 1000L);

        PortfolioTransaction order = createStopOrder(
            portfolio,
            instrument,
            PortfolioTransaction.TransactionType.BUY,
            "2.000000",
            "100.000000"
        );

        service.tryFillOrder(portfolio, order, instrument);

        assertThat(order.getOrderStatus()).isEqualTo(PortfolioTransaction.OrderStatus.FILLED);
        assertThat(order.getFilledQuantity()).isEqualByComparingTo("2.000000");
        verify(portfolioItemRepository).save(any(PortfolioItem.class));
        verify(portfolioRepository).save(portfolio);
        verify(portfolioTransactionRepository).save(order);
    }

    @Test
    void tryFillOrder_ShouldFillStopSell_WhenPriceCrossesStop() {
        Portfolio portfolio = createPortfolio("100000.000000");
        Instrument instrument = createInstrument("BTCUSDT", Instrument.InstrumentType.CRYPTO, "95.000000");
        stubHighVolume(instrument, 1000L);

        PortfolioItem lot = PortfolioItem.builder()
            .portfolio(portfolio)
            .instrument(instrument)
            .quantity(new BigDecimal("3.000000"))
            .purchasePrice(new BigDecimal("90.000000"))
            .purchaseDate(LocalDate.now().minusDays(5))
            .build();

        when(portfolioItemRepository.findByPortfolioIdAndInstrumentIdOrderByPurchaseDateAsc(portfolio.getId(), instrument.getId()))
            .thenReturn(List.of(lot));

        PortfolioTransaction order = createStopOrder(
            portfolio,
            instrument,
            PortfolioTransaction.TransactionType.SELL,
            "2.000000",
            "100.000000"
        );

        service.tryFillOrder(portfolio, order, instrument);

        assertThat(order.getOrderStatus()).isEqualTo(PortfolioTransaction.OrderStatus.FILLED);
        assertThat(order.getFilledQuantity()).isEqualByComparingTo("2.000000");

        ArgumentCaptor<PortfolioItem> itemCaptor = ArgumentCaptor.forClass(PortfolioItem.class);
        verify(portfolioItemRepository, atLeastOnce()).save(itemCaptor.capture());
        PortfolioItem updatedLot = itemCaptor.getAllValues().get(itemCaptor.getAllValues().size() - 1);
        assertThat(updatedLot.getQuantity()).isEqualByComparingTo("1.000000");
    }

    @Test
    void tryFillOrder_ShouldKeepStopPending_WhenSessionClosedForNonCrypto() {
        Portfolio portfolio = createPortfolio("100000.000000");
        Instrument instrument = createInstrument("THYAO", Instrument.InstrumentType.STOCK, "105.000000");

        LocalDate saturday = LocalDate.of(2026, 1, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        ZonedDateTime weekendNoon = saturday.atTime(12, 0).atZone(ZoneId.of("Europe/Istanbul"));
        service.setClockForTesting(Clock.fixed(weekendNoon.toInstant(), ZoneId.of("Europe/Istanbul")));

        PortfolioTransaction order = createStopOrder(
            portfolio,
            instrument,
            PortfolioTransaction.TransactionType.BUY,
            "2.000000",
            "100.000000"
        );

        service.tryFillOrder(portfolio, order, instrument);

        assertThat(order.getOrderStatus()).isEqualTo(PortfolioTransaction.OrderStatus.PENDING);
        assertThat(order.getFilledQuantity()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(portfolioItemRepository, never()).save(any(PortfolioItem.class));
        verify(portfolioRepository, never()).save(any(Portfolio.class));
        verify(portfolioTransactionRepository, never()).save(any(PortfolioTransaction.class));
    }

    private Portfolio createPortfolio(String cashBalance) {
        Portfolio portfolio = Portfolio.builder()
            .name("Test Portfolio")
            .cashBalance(new BigDecimal(cashBalance))
            .commissionRate(new BigDecimal("0.001000"))
            .minimumCommissionAmount(new BigDecimal("1.000000"))
            .commissionTaxRate(new BigDecimal("0.050000"))
            .build();
        portfolio.setId(UUID.randomUUID());
        return portfolio;
    }

    private Instrument createInstrument(String symbol, Instrument.InstrumentType type, String currentPrice) {
        Instrument instrument = Instrument.builder()
            .symbol(symbol)
            .name(symbol)
            .type(type)
            .currentPrice(new BigDecimal(currentPrice))
            .currency("TRY")
            .build();
        instrument.setId(UUID.randomUUID());
        return instrument;
    }

    private PortfolioTransaction createStopOrder(
            Portfolio portfolio,
            Instrument instrument,
            PortfolioTransaction.TransactionType transactionType,
            String quantity,
            String stopPrice) {
        return PortfolioTransaction.builder()
            .portfolio(portfolio)
            .instrument(instrument)
            .transactionType(transactionType)
            .orderType(PortfolioTransaction.OrderType.STOP)
            .orderStatus(PortfolioTransaction.OrderStatus.PENDING)
            .quantity(new BigDecimal(quantity))
            .price(new BigDecimal(stopPrice))
            .stopPrice(new BigDecimal(stopPrice))
            .transactionDate(LocalDate.now())
            .build();
    }

    private void stubHighVolume(Instrument instrument, long volume) {
        PriceHistory latestPrice = PriceHistory.builder()
            .instrument(instrument)
            .closePrice(instrument.getCurrentPrice())
            .volume(volume)
            .priceDate(LocalDate.now())
            .build();

        when(priceHistoryRepository.findTopByInstrumentIdOrderByPriceDateDesc(instrument.getId()))
            .thenReturn(Optional.of(latestPrice));
    }
}
