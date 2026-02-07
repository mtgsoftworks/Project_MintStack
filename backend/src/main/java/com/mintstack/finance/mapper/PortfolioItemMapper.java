package com.mintstack.finance.mapper;

import com.mintstack.finance.dto.response.PortfolioItemResponse;
import com.mintstack.finance.entity.PortfolioItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for PortfolioItem entity/DTO conversions.
 */
@Mapper(componentModel = "spring")
public interface PortfolioItemMapper {

    @Mapping(target = "instrumentId", source = "instrument.id")
    @Mapping(target = "instrumentSymbol", source = "instrument.symbol")
    @Mapping(target = "instrumentName", source = "instrument.name")
    @Mapping(target = "instrumentType", source = "instrument.type")
    @Mapping(target = "currentPrice", source = "instrument.currentPrice")
    @Mapping(target = "totalCost", expression = "java(item.getTotalCost())")
    @Mapping(target = "currentValue", expression = "java(item.getCurrentValue())")
    @Mapping(target = "profitLoss", expression = "java(item.getProfitLoss())")
    @Mapping(target = "profitLossPercent", expression = "java(item.getProfitLossPercent())")
    PortfolioItemResponse toResponse(PortfolioItem item);

    List<PortfolioItemResponse> toResponseList(List<PortfolioItem> items);
}
