package com.mintstack.finance.mapper;

import com.mintstack.finance.dto.response.NewsResponse;
import com.mintstack.finance.entity.News;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for News entity/DTO conversions.
 */
@Mapper(componentModel = "spring")
public interface NewsMapper {

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categorySlug", source = "category.slug")
    NewsResponse toResponse(News news);

    List<NewsResponse> toResponseList(List<News> newsList);
}
