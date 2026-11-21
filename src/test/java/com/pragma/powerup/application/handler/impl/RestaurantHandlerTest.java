package com.pragma.powerup.application.handler.impl;

import com.pragma.powerup.application.dto.request.RestaurantRequestDto;
import com.pragma.powerup.application.dto.response.PageResponseDto;
import com.pragma.powerup.application.dto.response.RestaurantResponseDto;
import com.pragma.powerup.application.dto.response.RestaurantSummaryResponseDto;
import com.pragma.powerup.application.mapper.IRestaurantRequestMapper;
import com.pragma.powerup.application.mapper.IRestaurantResponseMapper;
import com.pragma.powerup.domain.api.IRestaurantServicePort;
import com.pragma.powerup.domain.exception.InvalidOwnerException;
import com.pragma.powerup.domain.exception.InvalidPaginationException;
import com.pragma.powerup.domain.model.PageModel;
import com.pragma.powerup.domain.model.RestaurantModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void listRestaurantsCallsServicePortAndCopiesThePageIntoTheResponse() {
        List<RestaurantModel> restaurants = List.of(new RestaurantModel(), new RestaurantModel());
        List<RestaurantSummaryResponseDto> summaries = List.of(
                new RestaurantSummaryResponseDto(), new RestaurantSummaryResponseDto());
        PageModel<RestaurantModel> page = new PageModel<>(restaurants, 1, 2, 5L, 3);
        when(restaurantServicePort.listRestaurants(1, 2)).thenReturn(page);
        when(restaurantResponseMapper.toSummaryList(restaurants)).thenReturn(summaries);

        PageResponseDto<RestaurantSummaryResponseDto> result = restaurantHandler.listRestaurants(1, 2);

        assertAll(
                () -> assertSame(summaries, result.getContent()),
                () -> assertEquals(1, result.getPage()),
                () -> assertEquals(2, result.getSize()),
                () -> assertEquals(5L, result.getTotalElements()),
                () -> assertEquals(3, result.getTotalPages())
        );
        verify(restaurantServicePort).listRestaurants(1, 2);
    }

    @Test
    void listRestaurantsWhenServicePortThrowsPropagatesExceptionWithoutMappingResponse() {
        when(restaurantServicePort.listRestaurants(-1, 10))
                .thenThrow(new InvalidPaginationException("The page number cannot be negative"));

        assertThrows(InvalidPaginationException.class, () -> restaurantHandler.listRestaurants(-1, 10));

        verifyNoInteractions(restaurantResponseMapper);
    }
}
