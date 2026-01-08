package com.mintstack.finance.service;

import com.mintstack.finance.dto.request.AddPortfolioItemRequest;
import com.mintstack.finance.dto.request.CreatePortfolioRequest;
import com.mintstack.finance.dto.response.PortfolioItemResponse;
import com.mintstack.finance.dto.response.PortfolioResponse;
import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Portfolio;
import com.mintstack.finance.entity.PortfolioItem;
import com.mintstack.finance.entity.User;
import com.mintstack.finance.exception.BadRequestException;
import com.mintstack.finance.exception.ResourceNotFoundException;
import com.mintstack.finance.repository.InstrumentRepository;
import com.mintstack.finance.repository.PortfolioItemRepository;
import com.mintstack.finance.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final InstrumentRepository instrumentRepository;
    private final UserService userService;

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
        
        Portfolio portfolio = Portfolio.builder()
            .user(user)
            .name(request.getName())
            .description(request.getDescription())
            .isDefault(isDefault)
            .build();
        
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        log.info("Portfolio created: {} for user: {}", savedPortfolio.getId(), keycloakId);
        
        return mapToResponse(savedPortfolio);
    }

    @CacheEvict(value = "portfolios", key = "#portfolioId")
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
        
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        log.info("Portfolio updated: {}", portfolioId);
        
        return mapToResponse(savedPortfolio);
    }

    @CacheEvict(value = "portfolios", key = "#portfolioId")
    @Transactional
    public void deletePortfolio(String keycloakId, UUID portfolioId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));
        
        portfolioRepository.delete(portfolio);
        log.info("Portfolio deleted: {}", portfolioId);
    }

    @CacheEvict(value = "portfolios", key = "#portfolioId")
    @Transactional
    public PortfolioResponse addItem(String keycloakId, UUID portfolioId, AddPortfolioItemRequest request) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));
        
        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
            .orElseThrow(() -> new ResourceNotFoundException("Enstrüman", "id", request.getInstrumentId()));
        
        PortfolioItem item = PortfolioItem.builder()
            .portfolio(portfolio)
            .instrument(instrument)
            .quantity(request.getQuantity())
            .purchasePrice(request.getPurchasePrice())
            .purchaseDate(request.getPurchaseDate())
            .notes(request.getNotes())
            .build();
        
        portfolioItemRepository.save(item);
        log.info("Item added to portfolio {}: {}", portfolioId, instrument.getSymbol());
        
        return getPortfolio(keycloakId, portfolioId);
    }

    @CacheEvict(value = "portfolios", key = "#portfolioId")
    @Transactional
    public PortfolioResponse removeItem(String keycloakId, UUID portfolioId, UUID itemId) {
        User user = userService.getUserByKeycloakId(keycloakId);
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Portföy", "id", portfolioId));
        
        PortfolioItem item = portfolioItemRepository.findByIdAndPortfolioId(itemId, portfolioId)
            .orElseThrow(() -> new ResourceNotFoundException("Portföy kalemi", "id", itemId));
        
        portfolioItemRepository.delete(item);
        log.info("Item removed from portfolio {}: {}", portfolioId, itemId);
        
        return getPortfolio(keycloakId, portfolioId);
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolioSummary(String keycloakId, UUID portfolioId) {
        return getPortfolio(keycloakId, portfolioId);
    }

    private PortfolioResponse mapToResponse(Portfolio portfolio) {
        List<PortfolioItemResponse> items = portfolio.getItems().stream()
            .map(this::mapToItemResponse)
            .collect(Collectors.toList());
        
        return PortfolioResponse.builder()
            .id(portfolio.getId())
            .name(portfolio.getName())
            .description(portfolio.getDescription())
            .isDefault(portfolio.getIsDefault())
            .totalValue(portfolio.getTotalValue())
            .totalCost(portfolio.getTotalCost())
            .profitLoss(portfolio.getTotalProfitLoss())
            .profitLossPercent(portfolio.getProfitLossPercent())
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
}
