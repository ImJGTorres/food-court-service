package com.pragma.powerup.application.handler.impl;

import com.pragma.powerup.application.dto.request.DishRequestDto;
import com.pragma.powerup.application.dto.request.DishStatusRequestDto;
import com.pragma.powerup.application.dto.request.DishUpdateRequestDto;
import com.pragma.powerup.application.dto.response.DishResponseDto;
import com.pragma.powerup.application.handler.IDishHandler;
import com.pragma.powerup.application.mapper.IDishRequestMapper;
import com.pragma.powerup.application.mapper.IDishResponseMapper;
import com.pragma.powerup.domain.api.IDishServicePort;
import com.pragma.powerup.domain.model.DishModel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class DishHandler implements IDishHandler {
    private final IDishServicePort  dishServicePort;
    private final IDishResponseMapper dishResponseMapper;
    private final IDishRequestMapper dishRequestMapper;

    @Override
    public DishResponseDto saveDish(DishRequestDto dishRequestDto, Long ownerId) {
        DishModel dishModel = dishRequestMapper.toDish(dishRequestDto);
        DishModel savedDish = dishServicePort.saveDish(dishModel, ownerId);
        return dishResponseMapper.toResponse(savedDish);
    }

    @Override
    public DishResponseDto updateDish(Long dishId, DishUpdateRequestDto dishUpdateRequestDto, Long ownerId) {
        DishModel dishModel = dishRequestMapper.toDish(dishUpdateRequestDto);
        DishModel updatedDish = dishServicePort.updateDish(dishId, dishModel, ownerId);
        return dishResponseMapper.toResponse(updatedDish);
    }

    @Override
    public DishResponseDto updateDishStatus(Long dishId, DishStatusRequestDto dishStatusRequestDto, Long ownerId) {
        DishModel updatedDish = dishServicePort.updateDishStatus(dishId, dishStatusRequestDto.getActive(), ownerId);
        return dishResponseMapper.toResponse(updatedDish);
    }
}
