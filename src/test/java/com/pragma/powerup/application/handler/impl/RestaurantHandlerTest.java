package com.pragma.powerup.application.handler.impl;

import com.pragma.powerup.application.dto.request.RestaurantRequestDto;
import com.pragma.powerup.application.dto.response.RestaurantResponseDto;
import com.pragma.powerup.application.mapper.IRestaurantRequestMapper;
import com.pragma.powerup.application.mapper.IRestaurantResponseMapper;
import com.pragma.powerup.domain.api.IRestaurantServicePort;
import com.pragma.powerup.domain.exception.InvalidOwnerException;
import com.pragma.powerup.domain.model.RestaurantModel;
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
class RestaurantHandlerTest {

    @Mock
    private IRestaurantServicePort restaurantServicePort;

    @Mock
    private IRestaurantRequestMapper restaurantRequestMapper;

    @Mock
    private IRestaurantResponseMapper restaurantResponseMapper;

    @InjectMocks
    private RestaurantHandler restaurantHandler;

    @Test
    void saveRestaurantMapsRequestCallsServicePortAndReturnsMappedResponse() {
        RestaurantRequestDto request = new RestaurantRequestDto();
        RestaurantModel modelToSave = new RestaurantModel();
        RestaurantModel savedModel = new RestaurantModel();
        RestaurantResponseDto expectedResponse = new RestaurantResponseDto();
        when(restaurantRequestMapper.toRestaurant(request)).thenReturn(modelToSave);
        when(restaurantServicePort.saveRestaurant(modelToSave)).thenReturn(savedModel);
        when(restaurantResponseMapper.toResponse(savedModel)).thenReturn(expectedResponse);

        RestaurantResponseDto result = restaurantHandler.saveRestaurant(request);

        assertSame(expectedResponse, result);
        verify(restaurantServicePort).saveRestaurant(modelToSave);
    }

    @Test
    void saveRestaurantWhenServicePortThrowsPropagatesExceptionWithoutMappingResponse() {
        RestaurantRequestDto request = new RestaurantRequestDto();
        RestaurantModel modelToSave = new RestaurantModel();
        when(restaurantRequestMapper.toRestaurant(request)).thenReturn(modelToSave);
        when(restaurantServicePort.saveRestaurant(modelToSave))
                .thenThrow(new InvalidOwnerException("The user is not an owner"));

        assertThrows(InvalidOwnerException.class, () -> restaurantHandler.saveRestaurant(request));

        verifyNoInteractions(restaurantResponseMapper);
    }
}
