package com.pragma.powerup.application.handler;

import com.pragma.powerup.application.dto.request.RestaurantEmployeeRequestDto;
import com.pragma.powerup.application.dto.response.RestaurantEmployeeResponseDto;

public interface IRestaurantEmployeeHandler {
    RestaurantEmployeeResponseDto assignEmployee(Long restaurantId, RestaurantEmployeeRequestDto requestDto, Long ownerId);
}
