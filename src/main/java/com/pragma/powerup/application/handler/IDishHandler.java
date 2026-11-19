package com.pragma.powerup.application.handler;

import com.pragma.powerup.application.dto.request.DishRequestDto;
import com.pragma.powerup.application.dto.request.DishStatusRequestDto;
import com.pragma.powerup.application.dto.request.DishUpdateRequestDto;
import com.pragma.powerup.application.dto.response.DishResponseDto;

public interface IDishHandler {
    DishResponseDto saveDish(DishRequestDto dishRequestDto, Long ownerId);

    DishResponseDto updateDish(Long dishId, DishUpdateRequestDto dishUpdateRequestDto, Long ownerId);

    DishResponseDto updateDishStatus(Long dishId, DishStatusRequestDto dishStatusRequestDto, Long ownerId);
}
