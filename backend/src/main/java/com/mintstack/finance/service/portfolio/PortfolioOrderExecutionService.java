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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioOrderExecutionService {

    private static final BigDecimal MIN_ORDER_QUANTITY = new BigDecimal("0.000001");
    private static final ZoneId BIST_ZONE = ZoneId.of("Europe/Istanbul");
    private static final LocalTime BIST_SESSION_START = LocalTime.of(10, 0);
    private static final LocalTime BIST_SESSION_END = LocalTime.of(18, 0);

    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final PortfolioTransactionRepository portfolioTransactionRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PortfolioFinancialRulesService financialRulesService;
    private Clock clock = Clock.system(BIST_ZONE);

    void setClockForTesting(Clock clock) {
        this.clock = clock != null ? clock.withZone(BIST_ZONE) : Clock.system(BIST_ZONE);
    }

    public void tryFillOrder(Portfolio portfolio, PortfolioTransaction order, Instrument instrument) {
        if (order == null || instrument == null) {
            return;
        }
        if (order.getOrderStatus() == PortfolioTransaction.OrderStatus.FILLED
            || order.getOrderStatus() == PortfolioTransaction.OrderStatus.CANCELED
            || order.getOrderStatus() == PortfolioTransaction.OrderStatus.REJECTED) {
            return;
        }

        BigDecimal marketPrice = order.getOrderType() == PortfolioTransaction.OrderType.MARKET
            ? firstPositive(order.getPrice(), instrument.getCurrentPrice(), order.getAverageFillPrice())
            : firstPositive(instrument.getCurrentPrice(), order.getPrice(), order.getAverageFillPrice());
        if (marketPrice == null) {
            return;
        }
        if (!isOrderTriggered(order, marketPrice)) {
            return;
        }
        if (order.getOrderType() != PortfolioTransaction.OrderType.MARKET && !isTradingSessionOpen(instrument)) {
            return;
        }

        BigDecimal filledQuantity = safe(order.getFilledQuantity());
        BigDecimal remainingQuantity = order.getQuantity().subtract(filledQuantity);
        if (remainingQuantity.compareTo(MIN_ORDER_QUANTITY) < 0) {
            markOrderFilledIfNeeded(order);
            return;
        }

        Long volume = resolveLatestVolume(instrument);
        BigDecimal desiredFill = determineFillQuantity(remainingQuantity, volume, order.getOrderType());
        if (desiredFill.compareTo(MIN_ORDER_QUANTITY) < 0) {
            return;
        }

        if (order.getTransactionType() == PortfolioTransaction.TransactionType.BUY) {
            fillBuyOrder(portfolio, order, instrument, marketPrice, desiredFill, volume);
        } else {
            fillSellOrder(portfolio, order, instrument, marketPrice, desiredFill, volume);
        }

        portfolioRepository.save(portfolio);
        portfolioTransactionRepository.save(order);
    }

    private void fillBuyOrder(
            Portfolio portfolio,
            PortfolioTransaction order,
            Instrument instrument,
            BigDecimal marketPrice,
            BigDecimal desiredFill,
            Long volume) {
        BigDecimal executionPrice = applySlippage(marketPrice, order.getTransactionType(), order.getOrderType(), desiredFill, volume);

        BigDecimal fillQuantity = desiredFill;
        BigDecimal grossTotal = executionPrice.multiply(fillQuantity);
        BigDecimal commissionAmount = financialRulesService.calculateCommission(portfolio, instrument, grossTotal);
        BigDecimal requiredCash = grossTotal.add(commissionAmount);

        if (safe(portfolio.getCashBalance()).compareTo(requiredCash) < 0) {
            BigDecimal affordableQuantity = calculateAffordableQuantity(portfolio, instrument, executionPrice);
            if (affordableQuantity.compareTo(MIN_ORDER_QUANTITY) < 0) {
                markOrderRejected(order, "Yetersiz nakit bakiye");
                return;
            }
            fillQuantity = fillQuantity.min(affordableQuantity);
            grossTotal = executionPrice.multiply(fillQuantity);
            commissionAmount = financialRulesService.calculateCommission(portfolio, instrument, grossTotal);
            requiredCash = grossTotal.add(commissionAmount);
        }

        portfolio.setCashBalance(safe(portfolio.getCashBalance()).subtract(requiredCash));
        PortfolioItem item = PortfolioItem.builder()
            .portfolio(portfolio)
            .instrument(instrument)
            .quantity(fillQuantity)
            .purchasePrice(executionPrice)
            .purchaseDate(order.getTransactionDate())
            .notes(order.getNotes())
            .build();
        portfolioItemRepository.save(item);

        updateOrderAfterFill(order, fillQuantity, executionPrice, commissionAmount, BigDecimal.ZERO);
    }

    private void fillSellOrder(
            Portfolio portfolio,
            PortfolioTransaction order,
            Instrument instrument,
            BigDecimal marketPrice,
            BigDecimal desiredFill,
            Long volume) {
        BigDecimal availableQuantity = getAvailableQuantity(portfolio.getId(), instrument.getId());
        if (availableQuantity.compareTo(MIN_ORDER_QUANTITY) < 0) {
            markOrderRejected(order, "Satis icin enstruman bakiyesi bulunamadi");
            return;
        }

        BigDecimal fillQuantity = desiredFill.min(availableQuantity);
        if (fillQuantity.compareTo(MIN_ORDER_QUANTITY) < 0) {
            return;
        }

        BigDecimal executionPrice = applySlippage(marketPrice, order.getTransactionType(), order.getOrderType(), fillQuantity, volume);
        SellConsumption consumption = consumeSellLots(portfolio.getId(), instrument.getId(), fillQuantity);
        if (consumption.filledQuantity().compareTo(MIN_ORDER_QUANTITY) < 0) {
            markOrderRejected(order, "Satis icin uygun lot bulunamadi");
            return;
        }

        fillQuantity = consumption.filledQuantity();
        BigDecimal costBasis = consumption.costBasis();
        BigDecimal grossTotal = executionPrice.multiply(fillQuantity);
        BigDecimal commissionAmount = financialRulesService.calculateCommission(portfolio, instrument, grossTotal);
        BigDecimal netTotal = grossTotal.subtract(commissionAmount);
        BigDecimal realizedProfitLoss = netTotal.subtract(costBasis);

        portfolio.setCashBalance(safe(portfolio.getCashBalance()).add(netTotal));
        updateOrderAfterFill(order, fillQuantity, executionPrice, commissionAmount, realizedProfitLoss);
    }

    private SellConsumption consumeSellLots(UUID portfolioId, UUID instrumentId, BigDecimal quantityToConsume) {
        List<PortfolioItem> lots = portfolioItemRepository.findByPortfolioIdAndInstrumentIdOrderByPurchaseDateAsc(portfolioId, instrumentId);

        BigDecimal remaining = quantityToConsume;
        BigDecimal costBasis = BigDecimal.ZERO;
        BigDecimal consumed = BigDecimal.ZERO;

        for (PortfolioItem lot : lots) {
            if (remaining.compareTo(MIN_ORDER_QUANTITY) < 0) {
                break;
            }
            BigDecimal lotQuantity = safe(lot.getQuantity());
            if (lotQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal usedQuantity = lotQuantity.min(remaining);
            costBasis = costBasis.add(usedQuantity.multiply(safe(lot.getPurchasePrice())));
            consumed = consumed.add(usedQuantity);

            if (lotQuantity.compareTo(usedQuantity) <= 0) {
                portfolioItemRepository.delete(lot);
            } else {
                lot.setQuantity(lotQuantity.subtract(usedQuantity));
                portfolioItemRepository.save(lot);
            }
            remaining = remaining.subtract(usedQuantity);
        }

        return new SellConsumption(consumed, costBasis);
    }

    private BigDecimal getAvailableQuantity(UUID portfolioId, UUID instrumentId) {
        return portfolioItemRepository.findByPortfolioIdAndInstrumentIdOrderByPurchaseDateAsc(portfolioId, instrumentId)
            .stream()
            .map(PortfolioItem::getQuantity)
            .map(this::safe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updateOrderAfterFill(
            PortfolioTransaction order,
            BigDecimal fillQuantity,
            BigDecimal executionPrice,
            BigDecimal commissionAmount,
            BigDecimal realizedProfitLoss) {
        BigDecimal previousFilled = safe(order.getFilledQuantity());
        BigDecimal newFilled = previousFilled.add(fillQuantity).setScale(6, RoundingMode.HALF_UP);

        BigDecimal weightedPrevious = firstPositive(order.getAverageFillPrice(), order.getPrice(), executionPrice).multiply(previousFilled);
        BigDecimal weightedCurrent = executionPrice.multiply(fillQuantity);
        BigDecimal averageFill = weightedPrevious.add(weightedCurrent).divide(newFilled, 6, RoundingMode.HALF_UP);

        order.setFilledQuantity(newFilled);
        order.setAverageFillPrice(averageFill);
        order.setPrice(averageFill);
        order.setCommissionAmount(safe(order.getCommissionAmount()).add(commissionAmount).setScale(6, RoundingMode.HALF_UP));
        order.setRealizedProfitLoss(safe(order.getRealizedProfitLoss()).add(realizedProfitLoss).setScale(6, RoundingMode.HALF_UP));

        BigDecimal remaining = order.getQuantity().subtract(newFilled);
        if (remaining.compareTo(MIN_ORDER_QUANTITY) < 0) {
            order.setOrderStatus(PortfolioTransaction.OrderStatus.FILLED);
            order.setFilledAt(LocalDateTime.now(clock));
        } else {
            order.setOrderStatus(PortfolioTransaction.OrderStatus.PARTIALLY_FILLED);
        }
    }

    private void markOrderRejected(PortfolioTransaction order, String reason) {
        order.setOrderStatus(PortfolioTransaction.OrderStatus.REJECTED);
        order.setCancelReason(reason);
        order.setFilledAt(LocalDateTime.now(clock));
    }

    private void markOrderFilledIfNeeded(PortfolioTransaction order) {
        if (order.getOrderStatus() != PortfolioTransaction.OrderStatus.FILLED) {
            order.setOrderStatus(PortfolioTransaction.OrderStatus.FILLED);
            order.setFilledAt(LocalDateTime.now(clock));
        }
    }

    private boolean isOrderTriggered(PortfolioTransaction order, BigDecimal marketPrice) {
        if (order.getOrderType() == PortfolioTransaction.OrderType.MARKET) {
            return true;
        }
        if (order.getOrderType() == PortfolioTransaction.OrderType.LIMIT) {
            BigDecimal limitPrice = firstPositive(order.getLimitPrice(), order.getPrice());
            if (limitPrice == null) {
                return false;
            }
            if (order.getTransactionType() == PortfolioTransaction.TransactionType.BUY) {
                return marketPrice.compareTo(limitPrice) <= 0;
            }
            return marketPrice.compareTo(limitPrice) >= 0;
        }

        BigDecimal stopPrice = firstPositive(order.getStopPrice(), order.getPrice());
        if (stopPrice == null) {
            return false;
        }
        if (order.getTransactionType() == PortfolioTransaction.TransactionType.BUY) {
            return marketPrice.compareTo(stopPrice) >= 0;
        }
        return marketPrice.compareTo(stopPrice) <= 0;
    }

    private boolean isTradingSessionOpen(Instrument instrument) {
        // Keep market orders immediately executable; session simulation is applied for queued orders.
        // Market orders are created and filled in the same request path.
        if (instrument == null) {
            return true;
        }
        if (instrument.getType() == Instrument.InstrumentType.CRYPTO) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime time = now.toLocalTime();
        return !time.isBefore(BIST_SESSION_START) && !time.isAfter(BIST_SESSION_END);
    }

    private Long resolveLatestVolume(Instrument instrument) {
        if (instrument == null || instrument.getId() == null || priceHistoryRepository == null) {
            return null;
        }
        var latestHistory = priceHistoryRepository.findTopByInstrumentIdOrderByPriceDateDesc(instrument.getId());
        if (latestHistory == null) {
            return null;
        }
        return latestHistory.map(PriceHistory::getVolume).orElse(null);
    }

    private BigDecimal determineFillQuantity(BigDecimal remainingQuantity, Long volume, PortfolioTransaction.OrderType orderType) {
        if (remainingQuantity.compareTo(MIN_ORDER_QUANTITY) < 0) {
            return BigDecimal.ZERO;
        }
        if (orderType == PortfolioTransaction.OrderType.MARKET) {
            return remainingQuantity;
        }
        if (volume == null || volume <= 0) {
            return remainingQuantity.multiply(new BigDecimal("0.50"))
                .max(MIN_ORDER_QUANTITY)
                .min(remainingQuantity)
                .setScale(6, RoundingMode.DOWN);
        }

        BigDecimal participationRate = orderType == PortfolioTransaction.OrderType.MARKET
            ? new BigDecimal("0.25")
            : orderType == PortfolioTransaction.OrderType.LIMIT
                ? new BigDecimal("0.15")
                : new BigDecimal("0.10");

        BigDecimal maxFillByVolume = new BigDecimal(volume).multiply(participationRate).setScale(6, RoundingMode.DOWN);
        BigDecimal fill = remainingQuantity.min(maxFillByVolume);
        return fill.compareTo(MIN_ORDER_QUANTITY) < 0 ? BigDecimal.ZERO : fill;
    }

    private BigDecimal applySlippage(
            BigDecimal marketPrice,
            PortfolioTransaction.TransactionType transactionType,
            PortfolioTransaction.OrderType orderType,
            BigDecimal quantity,
            Long volume) {
        if (orderType == PortfolioTransaction.OrderType.MARKET) {
            return marketPrice.setScale(6, RoundingMode.HALF_UP);
        }

        BigDecimal baseBps = orderType == PortfolioTransaction.OrderType.MARKET
            ? new BigDecimal("12")
            : orderType == PortfolioTransaction.OrderType.LIMIT
                ? new BigDecimal("4")
                : new BigDecimal("18");

        BigDecimal liquidityBps;
        if (volume == null || volume <= 0) {
            liquidityBps = new BigDecimal("8");
        } else {
            BigDecimal ratio = quantity.divide(new BigDecimal(volume), 10, RoundingMode.HALF_UP);
            liquidityBps = ratio.multiply(new BigDecimal("5000"));
        }

        BigDecimal totalBps = baseBps.add(liquidityBps).min(new BigDecimal("150"));
        BigDecimal slippageRatio = totalBps.divide(new BigDecimal("10000"), 10, RoundingMode.HALF_UP);

        BigDecimal multiplier = transactionType == PortfolioTransaction.TransactionType.BUY
            ? BigDecimal.ONE.add(slippageRatio)
            : BigDecimal.ONE.subtract(slippageRatio).max(new BigDecimal("0.0001"));
        return marketPrice.multiply(multiplier).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAffordableQuantity(Portfolio portfolio, Instrument instrument, BigDecimal executionPrice) {
        BigDecimal cash = safe(portfolio.getCashBalance());
        BigDecimal maxByPrice = cash.divide(executionPrice, 6, RoundingMode.DOWN);
        if (maxByPrice.compareTo(MIN_ORDER_QUANTITY) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal low = BigDecimal.ZERO;
        BigDecimal high = maxByPrice;
        for (int i = 0; i < 16; i++) {
            BigDecimal mid = low.add(high).divide(new BigDecimal("2"), 6, RoundingMode.DOWN);
            BigDecimal gross = executionPrice.multiply(mid);
            BigDecimal commission = financialRulesService.calculateCommission(portfolio, instrument, gross);
            BigDecimal total = gross.add(commission);
            if (total.compareTo(cash) <= 0) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return low.compareTo(MIN_ORDER_QUANTITY) >= 0 ? low : BigDecimal.ZERO;
    }

    private BigDecimal firstPositive(BigDecimal... values) {
        if (values == null) {
            return null;
        }
        for (BigDecimal value : values) {
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record SellConsumption(BigDecimal filledQuantity, BigDecimal costBasis) {
    }
}
