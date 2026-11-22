package com.pragma.powerup.application.mapper;

import com.pragma.powerup.application.dto.response.OrderDishResponseDto;
import com.pragma.powerup.application.dto.response.OrderResponseDto;
import com.pragma.powerup.domain.model.OrderDishModel;
import com.pragma.powerup.domain.model.OrderModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface IOrderResponseMapper {
    @Mapping(target = "restaurantId", source = "restaurant.id")
    OrderResponseDto toResponse(OrderModel orderModel);

    @Mapping(target = "dishId", source = "dish.id")
    @Mapping(target = "name", source = "dish.name")
    OrderDishResponseDto toDishResponse(OrderDishModel orderDishModel);
}
