package com.pragma.powerup.application.handler;

import com.pragma.powerup.application.dto.request.DishRequestDto;
import com.pragma.powerup.application.dto.request.DishStatusRequestDto;
import com.pragma.powerup.application.dto.request.DishUpdateRequestDto;
import com.pragma.powerup.application.dto.response.DishResponseDto;
import com.pragma.powerup.application.dto.response.DishSummaryResponseDto;
import com.pragma.powerup.application.dto.response.PageResponseDto;

public interface IDishHandler {
    DishResponseDto saveDish(DishRequestDto dishRequestDto, Long ownerId);

    DishResponseDto updateDish(Long dishId, DishUpdateRequestDto dishUpdateRequestDto, Long ownerId);

    DishResponseDto updateDishStatus(Long dishId, DishStatusRequestDto dishStatusRequestDto, Long ownerId);

    PageResponseDto<DishSummaryResponseDto> listDishes(Long restaurantId, Long categoryId, int page, int size);
}
