package com.mintstack.finance.mapper;

import com.mintstack.finance.dto.request.CreateWatchlistRequest;
import com.mintstack.finance.dto.response.WatchlistResponse;
import com.mintstack.finance.entity.Watchlist;
import com.mintstack.finance.entity.WatchlistItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for Watchlist entity/DTO conversions.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WatchlistMapper {

    @Mapping(target = "itemCount", expression = "java(watchlist.getItems() != null ? watchlist.getItems().size() : 0)")
    @Mapping(target = "items", source = "items")
    WatchlistResponse toResponse(Watchlist watchlist);

    @Mapping(target = "symbol", source = "instrument.symbol")
    @Mapping(target = "name", source = "instrument.name")
    @Mapping(target = "type", expression = "java(item.getInstrument().getType().name())")
    @Mapping(target = "currentPrice", source = "instrument.currentPrice")
    @Mapping(target = "previousClose", source = "instrument.previousClose")
    @Mapping(target = "addedAt", source = "addedAt")
    WatchlistResponse.WatchlistItemResponse toItemResponse(WatchlistItem item);

    List<WatchlistResponse> toResponseList(List<Watchlist> watchlists);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Watchlist toEntity(CreateWatchlistRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(CreateWatchlistRequest request, @MappingTarget Watchlist watchlist);
}
