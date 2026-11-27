package com.pragma.powerup.application.mapper;

import com.pragma.powerup.application.dto.response.OrderResponseDto;
import com.pragma.powerup.domain.model.OrderModel;
import com.pragma.powerup.domain.model.OrderStatus;
import com.pragma.powerup.domain.model.RestaurantModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guarda de seguridad (no prueba el copiado de campos): el PIN del pedido nunca debe llegar al JSON,
 * porque el empleado no puede conocerlo; solo le llega al cliente por SMS.
 */
class OrderResponseMapperTest {
    private static final String PIN = "482913";

    private final IOrderResponseMapper orderResponseMapper = Mappers.getMapper(IOrderResponseMapper.class);

    @Test
    void theSecurityPinOfAReadyOrderNeverAppearsInTheJsonOfTheResponse() throws Exception {
        RestaurantModel restaurant = new RestaurantModel();
        restaurant.setId(1L);
        OrderModel readyOrder = new OrderModel();
        readyOrder.setId(7L);
        readyOrder.setClientId(21L);
        readyOrder.setEmployeeId(24L);
        readyOrder.setRestaurant(restaurant);
        readyOrder.setStatus(OrderStatus.READY);
        readyOrder.setSecurityPin(PIN);
        readyOrder.setFailedPinAttempts(3);
        readyOrder.setDishes(List.of());

        OrderResponseDto response = orderResponseMapper.toResponse(readyOrder);
        String json = new ObjectMapper().writeValueAsString(response);

        assertFalse(json.contains(PIN));
        assertFalse(json.toLowerCase().contains("pin"));
        assertFalse(json.toLowerCase().contains("attempt"));
    }

    @Test
    void theSecurityPinOfEveryOrderOfAListNeverAppearsInTheJson() throws Exception {
        OrderModel readyOrder = new OrderModel();
        readyOrder.setRestaurant(new RestaurantModel());
        readyOrder.setSecurityPin(PIN);

        String json = new ObjectMapper().writeValueAsString(orderResponseMapper.toResponseList(List.of(readyOrder)));

        assertFalse(json.contains(PIN));
    }
}
