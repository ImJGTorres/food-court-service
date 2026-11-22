package com.pragma.powerup.application.handler.impl;

import com.pragma.powerup.application.dto.request.OrderRequestDto;
import com.pragma.powerup.application.dto.response.OrderResponseDto;
import com.pragma.powerup.application.mapper.IOrderRequestMapper;
import com.pragma.powerup.application.mapper.IOrderResponseMapper;
import com.pragma.powerup.domain.api.IOrderServicePort;
import com.pragma.powerup.domain.exception.OrderInProgressException;
import com.pragma.powerup.domain.model.OrderModel;
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
class OrderHandlerTest {
    private static final Long CLIENT_ID = 20L;

    @Mock
    private IOrderServicePort orderServicePort;

    @Mock
    private IOrderRequestMapper orderRequestMapper;

    @Mock
    private IOrderResponseMapper orderResponseMapper;

    @InjectMocks
    private OrderHandler orderHandler;

    @Test
    void createOrderMapsTheRequestPassesTheClientIdAndReturnsTheMappedResponse() {
        OrderRequestDto request = new OrderRequestDto();
        OrderModel orderToCreate = new OrderModel();
        OrderModel createdOrder = new OrderModel();
        OrderResponseDto expectedResponse = new OrderResponseDto();
        when(orderRequestMapper.toOrder(request)).thenReturn(orderToCreate);
        when(orderServicePort.createOrder(orderToCreate, CLIENT_ID)).thenReturn(createdOrder);
        when(orderResponseMapper.toResponse(createdOrder)).thenReturn(expectedResponse);

        OrderResponseDto result = orderHandler.createOrder(request, CLIENT_ID);

        assertSame(expectedResponse, result);
        verify(orderServicePort).createOrder(orderToCreate, CLIENT_ID);
    }

    @Test
    void createOrderWhenTheServicePortThrowsPropagatesTheExceptionWithoutMappingAResponse() {
        OrderRequestDto request = new OrderRequestDto();
        OrderModel orderToCreate = new OrderModel();
        when(orderRequestMapper.toOrder(request)).thenReturn(orderToCreate);
        when(orderServicePort.createOrder(orderToCreate, CLIENT_ID))
                .thenThrow(new OrderInProgressException("You already have an order in progress"));

        assertThrows(OrderInProgressException.class, () -> orderHandler.createOrder(request, CLIENT_ID));

        verifyNoInteractions(orderResponseMapper);
    }
}
