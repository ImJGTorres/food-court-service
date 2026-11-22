package com.pragma.powerup.application.mapper;

import com.pragma.powerup.application.dto.response.DishResponseDto;
import com.pragma.powerup.application.dto.response.DishSummaryResponseDto;
import com.pragma.powerup.domain.model.DishModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IDishResponseMapper {
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "restaurantId", source = "restaurant.id")
    DishResponseDto toResponse(DishModel dishModel);

    @Mapping(target = "categoryName", source = "category.name")
    DishSummaryResponseDto toSummary(DishModel dishModel);

    List<DishSummaryResponseDto> toSummaryList(List<DishModel> dishModels);
}
