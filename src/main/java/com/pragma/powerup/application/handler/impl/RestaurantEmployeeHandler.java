package com.pragma.powerup.application.handler.impl;

import com.pragma.powerup.application.dto.request.RestaurantEmployeeRequestDto;
import com.pragma.powerup.application.dto.response.RestaurantEmployeeResponseDto;
import com.pragma.powerup.application.handler.IRestaurantEmployeeHandler;
import com.pragma.powerup.application.mapper.IRestaurantEmployeeResponseMapper;
import com.pragma.powerup.domain.api.IRestaurantEmployeeServicePort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantEmployeeHandler implements IRestaurantEmployeeHandler {

    private final IRestaurantEmployeeServicePort restaurantEmployeeServicePort;
    private final IRestaurantEmployeeResponseMapper restaurantEmployeeResponseMapper;

    @Override
    public RestaurantEmployeeResponseDto assignEmployee(Long restaurantId, RestaurantEmployeeRequestDto requestDto,
                                                        Long ownerId) {
        return restaurantEmployeeResponseMapper.toResponse(
                restaurantEmployeeServicePort.assignEmployee(restaurantId, requestDto.getEmployeeId(), ownerId));
    }
}
