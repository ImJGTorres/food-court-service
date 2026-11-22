package com.pragma.powerup.application.handler.impl;

import com.pragma.powerup.application.dto.request.RestaurantEmployeeRequestDto;
import com.pragma.powerup.application.dto.response.RestaurantEmployeeResponseDto;
import com.pragma.powerup.application.mapper.IRestaurantEmployeeResponseMapper;
import com.pragma.powerup.domain.api.IRestaurantEmployeeServicePort;
import com.pragma.powerup.domain.exception.EmployeeAlreadyAssignedException;
import com.pragma.powerup.domain.model.RestaurantEmployeeModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantEmployeeHandlerTest {
    private static final Long RESTAURANT_ID = 1L;
    private static final Long EMPLOYEE_ID = 15L;
    private static final Long OWNER_ID = 10L;

    @Mock
    private IRestaurantEmployeeServicePort restaurantEmployeeServicePort;

    @Mock
    private IRestaurantEmployeeResponseMapper restaurantEmployeeResponseMapper;

    @InjectMocks
    private RestaurantEmployeeHandler restaurantEmployeeHandler;

    private RestaurantEmployeeRequestDto buildRequest() {
        RestaurantEmployeeRequestDto request = new RestaurantEmployeeRequestDto();
        request.setEmployeeId(EMPLOYEE_ID);
        return request;
    }

    @Test
    void assignEmployeePassesTheRestaurantTheEmployeeAndTheOwnerAndReturnsTheMappedLink() {
        RestaurantEmployeeModel savedLink = new RestaurantEmployeeModel(EMPLOYEE_ID, RESTAURANT_ID);
        RestaurantEmployeeResponseDto expectedResponse = new RestaurantEmployeeResponseDto();
        when(restaurantEmployeeServicePort.assignEmployee(RESTAURANT_ID, EMPLOYEE_ID, OWNER_ID)).thenReturn(savedLink);
        when(restaurantEmployeeResponseMapper.toResponse(savedLink)).thenReturn(expectedResponse);

        RestaurantEmployeeResponseDto result =
                restaurantEmployeeHandler.assignEmployee(RESTAURANT_ID, buildRequest(), OWNER_ID);

        assertSame(expectedResponse, result);
    }

    @Test
    void assignEmployeeWhenTheServicePortThrowsPropagatesTheExceptionWithoutMappingAResponse() {
        when(restaurantEmployeeServicePort.assignEmployee(RESTAURANT_ID, EMPLOYEE_ID, OWNER_ID))
                .thenThrow(new EmployeeAlreadyAssignedException("The employee is already assigned to a restaurant"));

        assertThrows(EmployeeAlreadyAssignedException.class,
                () -> restaurantEmployeeHandler.assignEmployee(RESTAURANT_ID, buildRequest(), OWNER_ID));

        verifyNoInteractions(restaurantEmployeeResponseMapper);
    }
}
