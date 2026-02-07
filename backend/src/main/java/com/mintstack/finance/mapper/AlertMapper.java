package com.mintstack.finance.mapper;

import com.mintstack.finance.dto.response.AlertResponse;
import com.mintstack.finance.entity.PriceAlert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for PriceAlert entity/DTO conversions.
 */
@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(target = "symbol", source = "instrument.symbol")
    @Mapping(target = "instrumentName", source = "instrument.name")
    @Mapping(target = "alertType", expression = "java(alert.getAlertType().name())")
    AlertResponse toResponse(PriceAlert alert);

    List<AlertResponse> toResponseList(List<PriceAlert> alerts);
}
