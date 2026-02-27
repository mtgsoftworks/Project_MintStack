package com.mintstack.finance.mapper;

import com.mintstack.finance.dto.request.CreatePortfolioRequest;
import com.mintstack.finance.dto.response.PortfolioResponse;
import com.mintstack.finance.entity.Portfolio;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for Portfolio entity/DTO conversions.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {PortfolioItemMapper.class})
public interface PortfolioMapper {

    @Mapping(target = "profitLoss", expression = "java(portfolio.getTotalProfitLoss())")
    @Mapping(target = "profitLossPercent", expression = "java(portfolio.getProfitLossPercent())")
    @Mapping(target = "totalValue", expression = "java(portfolio.getTotalValue())")
    @Mapping(target = "positionValue", expression = "java(portfolio.getPositionValue())")
    @Mapping(target = "cashBalance", expression = "java(portfolio.getCashBalance())")
    @Mapping(target = "initialCashBalance", expression = "java(portfolio.getInitialCashBalance())")
    @Mapping(target = "commissionRate", expression = "java(portfolio.getCommissionRate())")
    @Mapping(target = "minimumCommissionAmount", expression = "java(portfolio.getMinimumCommissionAmount())")
    @Mapping(target = "commissionTaxRate", expression = "java(portfolio.getCommissionTaxRate())")
    @Mapping(target = "netAssetValue", expression = "java(portfolio.getNetAssetValue())")
    @Mapping(target = "realizedProfitLoss", expression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "unrealizedProfitLoss", expression = "java(portfolio.getTotalProfitLoss())")
    @Mapping(target = "totalCost", expression = "java(portfolio.getTotalCost())")
    @Mapping(target = "itemCount", expression = "java(portfolio.getItems() != null ? portfolio.getItems().size() : 0)")
    PortfolioResponse toResponse(Portfolio portfolio);

    List<PortfolioResponse> toResponseList(List<Portfolio> portfolios);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "cashBalance", expression = "java(request.getInitialCashBalance())")
    @Mapping(target = "minimumCommissionAmount", expression = "java(request.getMinimumCommissionAmount())")
    @Mapping(target = "commissionTaxRate", expression = "java(request.getCommissionTaxRate())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Portfolio toEntity(CreatePortfolioRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "cashBalance", ignore = true)
    @Mapping(target = "initialCashBalance", ignore = true)
    @Mapping(target = "commissionRate", ignore = true)
    @Mapping(target = "minimumCommissionAmount", ignore = true)
    @Mapping(target = "commissionTaxRate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateFromRequest(CreatePortfolioRequest request, @MappingTarget Portfolio portfolio);
}
