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
    @Mapping(target = "totalCost", expression = "java(portfolio.getTotalCost())")
    @Mapping(target = "itemCount", expression = "java(portfolio.getItems() != null ? portfolio.getItems().size() : 0)")
    PortfolioResponse toResponse(Portfolio portfolio);

    List<PortfolioResponse> toResponseList(List<Portfolio> portfolios);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Portfolio toEntity(CreatePortfolioRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateFromRequest(CreatePortfolioRequest request, @MappingTarget Portfolio portfolio);
}
