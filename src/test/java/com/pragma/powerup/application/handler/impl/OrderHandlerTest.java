package com.pragma.powerup.application.handler.impl;

import com.pragma.powerup.application.dto.request.OrderRequestDto;
import com.pragma.powerup.application.dto.response.OrderResponseDto;
import com.pragma.powerup.application.dto.response.PageResponseDto;
import com.pragma.powerup.application.mapper.IOrderRequestMapper;
import com.pragma.powerup.application.mapper.IOrderResponseMapper;
import com.pragma.powerup.domain.api.IOrderServicePort;
import com.pragma.powerup.domain.exception.EmployeeNotAssignedException;
import com.pragma.powerup.domain.exception.OrderInProgressException;
import com.pragma.powerup.domain.exception.OrderNotPendingException;
import com.pragma.powerup.domain.model.OrderModel;
import com.pragma.powerup.domain.model.OrderStatus;
import com.pragma.powerup.domain.model.PageModel;
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
class OrderHandlerTest {
    private static final Long CLIENT_ID = 20L;
    private static final Long EMPLOYEE_ID = 24L;
    private static final Long ORDER_ID = 1L;

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

    @Test
    void listOrdersByStatusCallsTheServicePortAndCopiesThePageIntoTheResponse() {
        List<OrderModel> orders = List.of(new OrderModel());
        List<OrderResponseDto> responses = List.of(new OrderResponseDto());
        PageModel<OrderModel> page = new PageModel<>(orders, 1, 2, 3L, 2);
        when(orderServicePort.listOrdersByStatus(EMPLOYEE_ID, OrderStatus.PENDING, 1, 2)).thenReturn(page);
        when(orderResponseMapper.toResponseList(orders)).thenReturn(responses);

        PageResponseDto<OrderResponseDto> result = orderHandler.listOrdersByStatus(EMPLOYEE_ID, OrderStatus.PENDING, 1, 2);

        assertAll(
                () -> assertSame(responses, result.getContent()),
                () -> assertEquals(1, result.getPage()),
                () -> assertEquals(2, result.getSize()),
                () -> assertEquals(3L, result.getTotalElements()),
                () -> assertEquals(2, result.getTotalPages())
        );
    }

    @Test
    void listOrdersByStatusWhenTheServicePortThrowsPropagatesTheExceptionWithoutMapping() {
        when(orderServicePort.listOrdersByStatus(EMPLOYEE_ID, OrderStatus.PENDING, 0, 10))
                .thenThrow(new EmployeeNotAssignedException("The employee is not assigned to any restaurant"));

        assertThrows(EmployeeNotAssignedException.class,
                () -> orderHandler.listOrdersByStatus(EMPLOYEE_ID, OrderStatus.PENDING, 0, 10));

        verifyNoInteractions(orderResponseMapper);
    }

    @Test
    void assignOrderPassesTheOrderAndTheEmployeeAndReturnsTheMappedOrder() {
        OrderModel assignedOrder = new OrderModel();
        OrderResponseDto expectedResponse = new OrderResponseDto();
        when(orderServicePort.assignOrder(ORDER_ID, EMPLOYEE_ID)).thenReturn(assignedOrder);
        when(orderResponseMapper.toResponse(assignedOrder)).thenReturn(expectedResponse);

        OrderResponseDto result = orderHandler.assignOrder(ORDER_ID, EMPLOYEE_ID);

        assertSame(expectedResponse, result);
        verify(orderServicePort).assignOrder(ORDER_ID, EMPLOYEE_ID);
    }

    @Test
    void assignOrderWhenTheServicePortThrowsPropagatesTheExceptionWithoutMapping() {
        when(orderServicePort.assignOrder(ORDER_ID, EMPLOYEE_ID))
                .thenThrow(new OrderNotPendingException("Only pending orders can be assigned"));

        assertThrows(OrderNotPendingException.class, () -> orderHandler.assignOrder(ORDER_ID, EMPLOYEE_ID));

        verifyNoInteractions(orderResponseMapper);
    }
}
