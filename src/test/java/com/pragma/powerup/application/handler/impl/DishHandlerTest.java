package com.pragma.powerup.application.handler.impl;

import com.pragma.powerup.application.dto.request.DishRequestDto;
import com.pragma.powerup.application.dto.request.DishStatusRequestDto;
import com.pragma.powerup.application.dto.request.DishUpdateRequestDto;
import com.pragma.powerup.application.dto.response.DishResponseDto;
import com.pragma.powerup.application.mapper.IDishRequestMapper;
import com.pragma.powerup.application.mapper.IDishResponseMapper;
import com.pragma.powerup.domain.api.IDishServicePort;
import com.pragma.powerup.domain.exception.DishNotFoundException;
import com.pragma.powerup.domain.exception.NotRestaurantOwnerException;
import com.pragma.powerup.domain.model.DishModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishHandlerTest {
    private static final Long OWNER_ID = 1L;
    private static final Long DISH_ID = 7L;

    @Mock
    private IDishServicePort dishServicePort;

    @Mock
    private IDishRequestMapper dishRequestMapper;

    @Mock
    private IDishResponseMapper dishResponseMapper;

    @InjectMocks
    private DishHandler dishHandler;

    @Test
    void saveDishMapsRequestPassesTheOwnerIdToTheServicePortAndReturnsMappedResponse() {
        DishRequestDto request = new DishRequestDto();
        DishModel modelToSave = new DishModel();
        DishModel savedModel = new DishModel();
        DishResponseDto expectedResponse = new DishResponseDto();
        when(dishRequestMapper.toDish(request)).thenReturn(modelToSave);
        when(dishServicePort.saveDish(modelToSave, OWNER_ID)).thenReturn(savedModel);
        when(dishResponseMapper.toResponse(savedModel)).thenReturn(expectedResponse);

        DishResponseDto result = dishHandler.saveDish(request, OWNER_ID);

        assertSame(expectedResponse, result);
        verify(dishServicePort).saveDish(modelToSave, OWNER_ID);
    }

    @Test
    void saveDishWhenServicePortThrowsPropagatesExceptionWithoutMappingResponse() {
        DishRequestDto request = new DishRequestDto();
        DishModel modelToSave = new DishModel();
        when(dishRequestMapper.toDish(request)).thenReturn(modelToSave);
        when(dishServicePort.saveDish(modelToSave, OWNER_ID))
                .thenThrow(new NotRestaurantOwnerException("Only the owner of the restaurant can manage its dishes"));

        assertThrows(NotRestaurantOwnerException.class, () -> dishHandler.saveDish(request, OWNER_ID));

        verifyNoInteractions(dishResponseMapper);
    }

    @Test
    void updateDishMapsRequestPassesDishIdAndOwnerIdToTheServicePortAndReturnsMappedResponse() {
        DishUpdateRequestDto request = new DishUpdateRequestDto();
        DishModel changes = new DishModel();
        DishModel updatedModel = new DishModel();
        DishResponseDto expectedResponse = new DishResponseDto();
        when(dishRequestMapper.toDish(request)).thenReturn(changes);
        when(dishServicePort.updateDish(DISH_ID, changes, OWNER_ID)).thenReturn(updatedModel);
        when(dishResponseMapper.toResponse(updatedModel)).thenReturn(expectedResponse);

        DishResponseDto result = dishHandler.updateDish(DISH_ID, request, OWNER_ID);

        assertSame(expectedResponse, result);
        verify(dishServicePort).updateDish(DISH_ID, changes, OWNER_ID);
    }

    @Test
    void updateDishWhenServicePortThrowsPropagatesExceptionWithoutMappingResponse() {
        DishUpdateRequestDto request = new DishUpdateRequestDto();
        DishModel changes = new DishModel();
        when(dishRequestMapper.toDish(request)).thenReturn(changes);
        when(dishServicePort.updateDish(DISH_ID, changes, OWNER_ID))
                .thenThrow(new DishNotFoundException("The requested dish does not exist"));

        assertThrows(DishNotFoundException.class, () -> dishHandler.updateDish(DISH_ID, request, OWNER_ID));

        verifyNoInteractions(dishResponseMapper);
    }

    @Test
    void updateDishStatusPassesActiveDishIdAndOwnerIdToTheServicePortAndReturnsMappedResponse() {
        DishStatusRequestDto request = new DishStatusRequestDto();
        request.setActive(false);
        DishModel updatedDish = new DishModel();
        DishResponseDto expectedResponse = new DishResponseDto();
        when(dishServicePort.updateDishStatus(DISH_ID, false, OWNER_ID)).thenReturn(updatedDish);
        when(dishResponseMapper.toResponse(updatedDish)).thenReturn(expectedResponse);

        DishResponseDto result = dishHandler.updateDishStatus(DISH_ID, request, OWNER_ID);

        assertSame(expectedResponse, result);
        verify(dishServicePort).updateDishStatus(DISH_ID, false, OWNER_ID);
    }

    @Test
    void updateDishStatusWhenServicePortThrowsPropagatesExceptionWithoutMappingResponse() {
        DishStatusRequestDto request = new DishStatusRequestDto();
        request.setActive(true);
        when(dishServicePort.updateDishStatus(DISH_ID, true, OWNER_ID))
                .thenThrow(new NotRestaurantOwnerException("Only the owner of the restaurant can manage its dishes"));

        assertThrows(NotRestaurantOwnerException.class,
                () -> dishHandler.updateDishStatus(DISH_ID, request, OWNER_ID));

        verifyNoInteractions(dishResponseMapper);
    }
}
