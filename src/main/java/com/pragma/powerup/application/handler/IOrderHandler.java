package com.pragma.powerup.application.handler;

import com.pragma.powerup.application.dto.request.OrderDeliveryRequestDto;
import com.pragma.powerup.application.dto.request.OrderRequestDto;
import com.pragma.powerup.application.dto.response.OrderResponseDto;
import com.pragma.powerup.application.dto.response.PageResponseDto;
import com.pragma.powerup.domain.model.OrderStatus;

public interface IOrderHandler {
    OrderResponseDto createOrder(OrderRequestDto orderRequestDto, Long clientId);

    PageResponseDto<OrderResponseDto> listOrdersByStatus(Long employeeId, OrderStatus status, int page, int size);

    OrderResponseDto assignOrder(Long orderId, Long employeeId);

    OrderResponseDto markOrderReady(Long orderId, Long employeeId);

    OrderResponseDto deliverOrder(Long orderId, Long employeeId, OrderDeliveryRequestDto orderDeliveryRequestDto);
}
