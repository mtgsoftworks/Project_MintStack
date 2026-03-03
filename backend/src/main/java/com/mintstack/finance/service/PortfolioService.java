package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.AddPortfolioItemRequest;
import com.mintstack.finance.dto.request.AdjustPortfolioCashRequest;
import com.mintstack.finance.dto.request.CreatePortfolioRequest;
import com.mintstack.finance.dto.request.ExecutePortfolioTradeRequest;
import com.mintstack.finance.dto.response.PortfolioItemResponse;
import com.mintstack.finance.dto.response.PortfolioResponse;
import com.mintstack.finance.dto.response.PortfolioSummaryResponse;
import com.mintstack.finance.dto.response.PortfolioTransactionResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Portfolio;
import com.mintstack.finance.entity.PortfolioItem;
import com.mintstack.finance.entity.PortfolioTransaction;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.exception.BadRequestException;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PortfolioItemRepository;
import com.mintstack.finance.repository.PortfolioRepository;
import com.mintstack.finance.repository.PortfolioTransactionRepository;
import com.mintstack.finance.service.portfolio.PortfolioFinancialRulesService;
import com.mintstack.finance.service.portfolio.PortfolioOrderExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@io.micrometer.observation.annotation.Observed(name = "portfolio.service", contextualName = "portfolio-operations")
public class PortfolioService {

    private static final BigDecimal DEFAULT_INITIAL_CASH = new BigDecimal("100000.000000");
    private static final BigDecimal MIN_ORDER_QUANTITY = new BigDecimal("0.000001");
    private static final ZoneId BIST_ZONE = ZoneId.of("Europe/Istanbul");

    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final InstrumentRepository instrumentRepository;
    private final PortfolioTransactionRepository portfolioTransactionRepository;
    private final UserService userService;
    private final PortfolioFinancialRulesService financialRulesService;
    private final PortfolioOrderExecutionService orderExecutionService;

    @Cacheable(value = "userPortfolios", key = "#keycloakId")
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getUserPortfolios(String keycloakId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        List<Portfolio> portfolios = portfolioRepository.findByUserIdWithItems(user.getId());
        
        return portfolios.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "portfolios", key = "#portfolioId")
    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(String keycloakId, UUID portfolioId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Portfolio portfolio = portfolioRepository.findByIdAndUserIdWithItems(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));
        
        return mapToResponse(portfolio);
    }

    @CacheEvict(value = "userPortfolios", key = "#keycloakId")
    @Transactional
    public PortfolioResponse createPortfolio(String keycloakId, CreatePortfolioRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);
        
        if (portfolioRepository.existsByUserIdAndName(user.getId(), request.getName())) {
            throw new BadRequestException("Bu isimde bir portföy zaten mevcut");
        }
        
        // If this is the first portfolio or marked as default, handle default flag
        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault()) || 
                           portfolioRepository.countByUserId(user.getId()) == 0;
        
        if (isDefault) {
            // Remove default flag from other portfolios
            portfolioRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .ifPresent(p -> {
                    p.setIsDefault(false);
                    portfolioRepository.save(p);
                });
        }
        
        BigDecimal initialCash = financialRulesService.normalizePositiveOrDefault(request.getInitialCashBalance(), DEFAULT_INITIAL_CASH);
        BigDecimal commissionRate = financialRulesService.normalizeCommissionRate(request.getCommissionRate());
        BigDecimal minimumCommission = financialRulesService.normalizePositiveOrDefault(
            request.getMinimumCommissionAmount(),
            financialRulesService.getDefaultMinimumCommission()
        );
        BigDecimal commissionTaxRate = financialRulesService.normalizeCommissionTaxRate(request.getCommissionTaxRate());

        Portfolio portfolio = Portfolio.builder()
            .user(user)
            .name(request.getName())
            .description(request.getDescription())
            .isDefault(isDefault)
            .initialCashBalance(initialCash)
            .cashBalance(initialCash)
            .commissionRate(commissionRate)
            .minimumCommissionAmount(minimumCommission)
            .commissionTaxRate(commissionTaxRate)
            .build();
        
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        log.info("Portfolio created: {} for user: {}", savedPortfolio.getId(), keycloakId);
        
        return mapToResponse(savedPortfolio);
    }

    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "portfolios", key = "#portfolioId"),
        @CacheEvict(value = "userPortfolios", key = "#keycloakId")
    })
    @Transactional
    public PortfolioResponse updatePortfolio(String keycloakId, UUID portfolioId, CreatePortfolioRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));
        
        if (request.getName() != null && !request.getName().equals(portfolio.getName())) {
            if (portfolioRepository.existsByUserIdAndName(user.getId(), request.getName())) {
                throw new BadRequestException("Bu isimde bir portföy zaten mevcut");
            }
            portfolio.setName(request.getName());
        }
        
        if (request.getDescription() != null) {
            portfolio.setDescription(request.getDescription());
        }

        if (request.getCommissionRate() != null) {
            portfolio.setCommissionRate(financialRulesService.normalizeCommissionRate(request.getCommissionRate()));
        }
        if (request.getMinimumCommissionAmount() != null) {
            portfolio.setMinimumCommissionAmount(
                financialRulesService.normalizePositiveOrDefault(
                    request.getMinimumCommissionAmount(),
                    financialRulesService.getDefaultMinimumCommission()
                )
            );
        }
        if (request.getCommissionTaxRate() != null) {
            portfolio.setCommissionTaxRate(financialRulesService.normalizeCommissionTaxRate(request.getCommissionTaxRate()));
        }
        
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        log.info("Portfolio updated: {}", portfolioId);
        
        return mapToResponse(savedPortfolio);
    }

    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "portfolios", key = "#portfolioId"),
        @CacheEvict(value = "userPortfolios", key = "#keycloakId")
    })
    @Transactional
    public void deletePortfolio(String keycloakId, UUID portfolioId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));
        
        portfolioRepository.delete(portfolio);
        log.info("Portfolio deleted: {}", portfolioId);
    }

    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "portfolios", key = "#portfolioId"),
        @CacheEvict(value = "userPortfolios", key = "#keycloakId")
    })
    @Transactional
    public PortfolioResponse addItem(String keycloakId, UUID portfolioId, AddPortfolioItemRequest request) {
        ExecutePortfolioTradeRequest tradeRequest = ExecutePortfolioTradeRequest.builder()
            .instrumentId(request.getInstrumentId())
            .instrumentSymbol(request.getInstrumentSymbol())
            .transactionType(PortfolioTransaction.TransactionType.BUY)
            .orderType(PortfolioTransaction.OrderType.MARKET)
            .quantity(request.getQuantity())
            .price(request.getPurchasePrice())
            .transactionDate(request.getPurchaseDate())
            .notes(request.getNotes())
            .build();
        return executeTrade(keycloakId, portfolioId, tradeRequest);
    }

    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "portfolios", key = "#portfolioId"),
        @CacheEvict(value = "userPortfolios", key = "#keycloakId")
    })
    @Transactional
    public PortfolioResponse removeItem(String keycloakId, UUID portfolioId, UUID itemId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));
        
        PortfolioItem item = portfolioItemRepository.findByIdAndPortfolioId(itemId, portfolioId)
            .orElseThrow(() -> new ResourceNotFoundException("Portföy kalemi", "id", itemId));

        ExecutePortfolioTradeRequest tradeRequest = ExecutePortfolioTradeRequest.builder()
            .instrumentId(item.getInstrument().getId())
            .transactionType(PortfolioTransaction.TransactionType.SELL)
            .orderType(PortfolioTransaction.OrderType.MARKET)
            .quantity(item.getQuantity())
            .price(resolveSellPrice(item))
            .transactionDate(LocalDate.now())
            .notes(item.getNotes())
            .build();
        return executeTrade(keycloakId, portfolioId, tradeRequest);
    }

    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "portfolios", key = "#portfolioId"),
        @CacheEvict(value = "userPortfolios", key = "#keycloakId")
    })
    @Transactional
    public PortfolioResponse executeTrade(String keycloakId, UUID portfolioId, ExecutePortfolioTradeRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));

        Instrument instrument = resolveTradeInstrument(request.getInstrumentId(), request.getInstrumentSymbol());
        LocalDate transactionDate = request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now();
        PortfolioTransaction.OrderType orderType = request.getOrderType() != null
            ? request.getOrderType()
            : PortfolioTransaction.OrderType.MARKET;

        validateOrderRequest(orderType, request);

        PortfolioTransaction order = PortfolioTransaction.builder()
            .portfolio(portfolio)
            .instrument(instrument)
            .transactionType(request.getTransactionType())
            .orderType(orderType)
            .orderStatus(PortfolioTransaction.OrderStatus.PENDING)
            .quantity(request.getQuantity())
            .filledQuantity(BigDecimal.ZERO)
            .price(resolveOrderReferencePrice(orderType, request, instrument))
            .limitPrice(request.getLimitPrice())
            .stopPrice(request.getStopPrice())
            .commissionAmount(BigDecimal.ZERO)
            .realizedProfitLoss(BigDecimal.ZERO)
            .transactionDate(transactionDate)
            .notes(request.getNotes())
            .build();

        PortfolioTransaction persistedOrder = portfolioTransactionRepository.save(order);
        if (persistedOrder != null) {
            order = persistedOrder;
        }
        orderExecutionService.tryFillOrder(portfolio, order, instrument);
        return reloadPortfolioResponse(user.getId(), portfolioId);
    }

    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "portfolios", key = "#portfolioId"),
        @CacheEvict(value = "userPortfolios", key = "#keycloakId")
    })
    @Transactional
    public PortfolioResponse processPendingOrders(String keycloakId, UUID portfolioId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));

        List<PortfolioTransaction> pendingOrders = portfolioTransactionRepository
            .findByPortfolioIdAndOrderStatusInOrderByCreatedAtAsc(
                portfolioId,
                List.of(PortfolioTransaction.OrderStatus.PENDING, PortfolioTransaction.OrderStatus.PARTIALLY_FILLED)
            );

        for (PortfolioTransaction order : pendingOrders) {
            orderExecutionService.tryFillOrder(portfolio, order, order.getInstrument());
        }

        return reloadPortfolioResponse(user.getId(), portfolioId);
    }

    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "portfolios", key = "#portfolioId"),
        @CacheEvict(value = "userPortfolios", key = "#keycloakId")
    })
    @Transactional
    public PortfolioResponse cancelOrder(String keycloakId, UUID portfolioId, UUID orderId, String reason) {
        User user = userService.getUserByKeycloakId(keycloakId);
        portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));

        PortfolioTransaction order = portfolioTransactionRepository.findByIdAndPortfolioId(orderId, portfolioId)
            .orElseThrow(() -> new ResourceNotFoundException("Emir", "id", orderId));

        if (order.getOrderStatus() == PortfolioTransaction.OrderStatus.FILLED
            || order.getOrderStatus() == PortfolioTransaction.OrderStatus.CANCELED
            || order.getOrderStatus() == PortfolioTransaction.OrderStatus.REJECTED) {
            throw new BadRequestException("Sadece bekleyen emirler iptal edilebilir");
        }

        order.setOrderStatus(PortfolioTransaction.OrderStatus.CANCELED);
        order.setCancelReason(reason != null && !reason.isBlank() ? reason : "Kullanıcı iptali");
        order.setFilledAt(LocalDateTime.now(BIST_ZONE));
        portfolioTransactionRepository.save(order);

        return reloadPortfolioResponse(user.getId(), portfolioId);
    }

    @org.springframework.cache.annotation.Caching(evict = {
        @CacheEvict(value = "portfolios", key = "#portfolioId"),
        @CacheEvict(value = "userPortfolios", key = "#keycloakId")
    })
    @Transactional
    public PortfolioResponse adjustCashBalance(String keycloakId, UUID portfolioId, AdjustPortfolioCashRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));

        if (request.getAction() == AdjustPortfolioCashRequest.CashAction.DEPOSIT) {
            portfolio.setCashBalance(safe(portfolio.getCashBalance()).add(request.getAmount()));
        } else {
            financialRulesService.ensureSufficientCash(portfolio, request.getAmount());
            portfolio.setCashBalance(safe(portfolio.getCashBalance()).subtract(request.getAmount()));
        }

        portfolioRepository.save(portfolio);
        return reloadPortfolioResponse(user.getId(), portfolioId);
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolioSummary(String keycloakId, UUID portfolioId) {
        return getPortfolio(keycloakId, portfolioId);
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getUserPortfolioSummary(String keycloakId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        List<Portfolio> portfolios = portfolioRepository.findByUserIdWithItems(user.getId());

        BigDecimal totalValue = portfolios.stream()
            .map(Portfolio::getTotalValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = portfolios.stream()
            .map(Portfolio::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfitLoss = totalValue.subtract(totalCost);

        BigDecimal totalProfitLossPercent = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossPercent = totalProfitLoss
                .divide(totalCost, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }

        BigDecimal totalCashBalance = portfolios.stream()
            .map(Portfolio::getCashBalance)
            .map(this::safe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNetAssetValue = portfolios.stream()
            .map(Portfolio::getNetAssetValue)
            .map(this::safe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRealizedProfitLoss = safe(portfolioTransactionRepository.sumRealizedProfitLossByUserId(user.getId()));
        BigDecimal totalUnrealizedProfitLoss = portfolios.stream()
            .map(Portfolio::getTotalProfitLoss)
            .map(this::safe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PortfolioSummaryResponse.builder()
            .totalValue(totalValue)
            .totalCost(totalCost)
            .totalProfitLoss(totalProfitLoss)
            .totalProfitLossPercent(totalProfitLossPercent)
            .totalCashBalance(totalCashBalance)
            .totalNetAssetValue(totalNetAssetValue)
            .totalRealizedProfitLoss(totalRealizedProfitLoss)
            .totalUnrealizedProfitLoss(totalUnrealizedProfitLoss)
            .portfolioCount(portfolios.size())
            .build();
    }

    @Transactional(readOnly = true)
    public Page<PortfolioTransactionResponse> getPortfolioTransactions(
            String keycloakId,
            UUID portfolioId,
            Pageable pageable) {
        return getPortfolioTransactions(keycloakId, portfolioId, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PortfolioTransactionResponse> getPortfolioTransactions(
            String keycloakId,
            UUID portfolioId,
            PortfolioTransaction.OrderStatus orderStatus,
            Pageable pageable) {
        User user = userService.getUserByKeycloakId(keycloakId);
        portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));

        Page<PortfolioTransaction> transactions;
        if (orderStatus == null) {
            transactions = portfolioTransactionRepository.findByPortfolioIdAndUserId(portfolioId, user.getId(), pageable);
        } else {
            transactions = portfolioTransactionRepository.findByPortfolioIdAndUserIdAndOrderStatus(
                portfolioId,
                user.getId(),
                orderStatus,
                pageable
            );
        }

        return transactions.map(this::mapToTransactionResponse);
    }

    private PortfolioResponse mapToResponse(Portfolio portfolio) {
        List<PortfolioItemResponse> items = portfolio.getItems().stream()
            .map(this::mapToItemResponse)
            .collect(Collectors.toList());

        BigDecimal positionValue = safe(portfolio.getPositionValue());
        BigDecimal cashBalance = safe(portfolio.getCashBalance());
        BigDecimal realizedProfitLoss = safe(portfolioTransactionRepository.sumRealizedProfitLossByPortfolioId(portfolio.getId()));
        BigDecimal unrealizedProfitLoss = safe(portfolio.getTotalProfitLoss());

        return PortfolioResponse.builder()
            .id(portfolio.getId())
            .name(portfolio.getName())
            .description(portfolio.getDescription())
            .isDefault(portfolio.getIsDefault())
            .totalValue(positionValue)
            .positionValue(positionValue)
            .totalCost(portfolio.getTotalCost())
            .profitLoss(unrealizedProfitLoss)
            .profitLossPercent(portfolio.getProfitLossPercent())
            .cashBalance(cashBalance)
            .initialCashBalance(safe(portfolio.getInitialCashBalance()))
            .commissionRate(safe(portfolio.getCommissionRate()))
            .minimumCommissionAmount(safe(portfolio.getMinimumCommissionAmount()))
            .commissionTaxRate(safe(portfolio.getCommissionTaxRate()))
            .netAssetValue(positionValue.add(cashBalance))
            .realizedProfitLoss(realizedProfitLoss)
            .unrealizedProfitLoss(unrealizedProfitLoss)
            .itemCount(portfolio.getItems().size())
            .items(items)
            .createdAt(portfolio.getCreatedAt())
            .updatedAt(portfolio.getUpdatedAt())
            .build();
    }

    private PortfolioItemResponse mapToItemResponse(PortfolioItem item) {
        Instrument instrument = item.getInstrument();
        
        return PortfolioItemResponse.builder()
            .id(item.getId())
            .instrumentId(instrument.getId())
            .instrumentSymbol(instrument.getSymbol())
            .instrumentName(instrument.getName())
            .instrumentType(instrument.getType())
            .quantity(item.getQuantity())
            .purchasePrice(item.getPurchasePrice())
            .currentPrice(instrument.getCurrentPrice())
            .totalCost(item.getTotalCost())
            .currentValue(item.getCurrentValue())
            .profitLoss(item.getProfitLoss())
            .profitLossPercent(item.getProfitLossPercent())
            .purchaseDate(item.getPurchaseDate())
            .notes(item.getNotes())
            .build();
    }

    private PortfolioResponse reloadPortfolioResponse(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserIdWithItems(portfolioId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));
        return mapToResponse(portfolio);
    }

    private void validateOrderRequest(PortfolioTransaction.OrderType orderType, ExecutePortfolioTradeRequest request) {
        if (request.getTransactionType() == null) {
            throw new BadRequestException("Islem tipi zorunludur");
        }
        if (request.getQuantity() == null || request.getQuantity().compareTo(MIN_ORDER_QUANTITY) < 0) {
            throw new BadRequestException("Miktar en az 0.000001 olmalidir");
        }
        if (orderType == PortfolioTransaction.OrderType.LIMIT && firstPositive(request.getLimitPrice(), request.getPrice()) == null) {
            throw new BadRequestException("Limit emir icin limit fiyat zorunludur");
        }
        if (orderType == PortfolioTransaction.OrderType.STOP && firstPositive(request.getStopPrice(), request.getPrice()) == null) {
            throw new BadRequestException("Stop emir icin stop fiyat zorunludur");
        }
    }

    private BigDecimal resolveOrderReferencePrice(
            PortfolioTransaction.OrderType orderType,
            ExecutePortfolioTradeRequest request,
            Instrument instrument) {
        BigDecimal marketPrice = instrument.getCurrentPrice();
        if (orderType == PortfolioTransaction.OrderType.MARKET) {
            return firstPositive(request.getPrice(), marketPrice);
        }
        if (orderType == PortfolioTransaction.OrderType.LIMIT) {
            return firstPositive(request.getLimitPrice(), request.getPrice(), marketPrice);
        }
        return firstPositive(request.getStopPrice(), request.getPrice(), marketPrice);
    }

    private Instrument resolveTradeInstrument(UUID instrumentId, String instrumentSymbol) {
        if (instrumentId != null) {
            return instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enstrüman", "id", instrumentId));
        }

        if (instrumentSymbol != null && !instrumentSymbol.isBlank()) {
            String symbol = instrumentSymbol.trim().toUpperCase();
            return instrumentRepository.findBySymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Enstrüman", "symbol", symbol));
        }
        throw new BadRequestException("Enstrüman ID veya sembolü zorunludur");
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

    private BigDecimal resolveSellPrice(PortfolioItem item) {
        Instrument instrument = item.getInstrument();
        if (instrument != null && instrument.getCurrentPrice() != null) {
            return instrument.getCurrentPrice();
        }
        return item.getPurchasePrice();
    }

    private PortfolioTransactionResponse mapToTransactionResponse(PortfolioTransaction transaction) {
        Instrument instrument = transaction.getInstrument();
        BigDecimal filledQuantity = safe(transaction.getFilledQuantity());
        BigDecimal grossQuantity = filledQuantity.compareTo(BigDecimal.ZERO) > 0 ? filledQuantity : safe(transaction.getQuantity());
        BigDecimal unitPrice = firstPositive(transaction.getAverageFillPrice(), transaction.getPrice());
        BigDecimal grossTotal = unitPrice != null
            ? unitPrice.multiply(grossQuantity).setScale(6, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        BigDecimal commissionAmount = safe(transaction.getCommissionAmount());
        BigDecimal netTotal = transaction.getTransactionType() == PortfolioTransaction.TransactionType.BUY
            ? grossTotal.add(commissionAmount)
            : grossTotal.subtract(commissionAmount);
        BigDecimal remainingQuantity = safe(transaction.getQuantity()).subtract(filledQuantity).max(BigDecimal.ZERO);

        return PortfolioTransactionResponse.builder()
            .id(transaction.getId())
            .portfolioId(transaction.getPortfolio().getId())
            .instrumentId(instrument.getId())
            .instrumentSymbol(instrument.getSymbol())
            .instrumentName(instrument.getName())
            .instrumentType(instrument.getType())
            .transactionType(transaction.getTransactionType())
            .orderType(transaction.getOrderType())
            .orderStatus(transaction.getOrderStatus())
            .quantity(transaction.getQuantity())
            .filledQuantity(filledQuantity)
            .remainingQuantity(remainingQuantity)
            .price(transaction.getPrice())
            .averageFillPrice(transaction.getAverageFillPrice())
            .total(netTotal)
            .grossTotal(grossTotal)
            .commissionAmount(commissionAmount)
            .netTotal(netTotal)
            .limitPrice(transaction.getLimitPrice())
            .stopPrice(transaction.getStopPrice())
            .realizedProfitLoss(safe(transaction.getRealizedProfitLoss()))
            .cancelReason(transaction.getCancelReason())
            .transactionDate(transaction.getTransactionDate())
            .notes(transaction.getNotes())
            .createdAt(transaction.getCreatedAt())
            .filledAt(transaction.getFilledAt())
            .build();
    }

}

