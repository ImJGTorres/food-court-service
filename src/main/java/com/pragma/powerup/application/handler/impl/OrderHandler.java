package com.pragma.powerup.application.handler.impl;

import com.pragma.powerup.application.dto.request.OrderDeliveryRequestDto;
import com.pragma.powerup.application.dto.request.OrderRequestDto;
import com.pragma.powerup.application.dto.response.OrderResponseDto;
import com.pragma.powerup.application.dto.response.PageResponseDto;
import com.pragma.powerup.application.handler.IOrderHandler;
import com.pragma.powerup.application.mapper.IOrderRequestMapper;
import com.pragma.powerup.application.mapper.IOrderResponseMapper;
import com.pragma.powerup.domain.api.IOrderServicePort;
import com.pragma.powerup.domain.exception.InvalidSecurityPinException;
import com.pragma.powerup.domain.exception.OrderLockedException;
import com.pragma.powerup.domain.model.OrderModel;
import com.pragma.powerup.domain.model.OrderStatus;
import com.pragma.powerup.domain.model.PageModel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderHandler implements IOrderHandler {

    private final IOrderServicePort orderServicePort;
    private final IOrderRequestMapper orderRequestMapper;
    private final IOrderResponseMapper orderResponseMapper;

    @Override
    public OrderResponseDto createOrder(OrderRequestDto orderRequestDto, Long clientId) {
        OrderModel orderModel = orderRequestMapper.toOrder(orderRequestDto);
        OrderModel createdOrder = orderServicePort.createOrder(orderModel, clientId);
        return orderResponseMapper.toResponse(createdOrder);
    }

    @Override
    public PageResponseDto<OrderResponseDto> listOrdersByStatus(Long employeeId, OrderStatus status, int page,
                                                                int size) {
        PageModel<OrderModel> orderPage = orderServicePort.listOrdersByStatus(employeeId, status, page, size);
        return new PageResponseDto<>(orderResponseMapper.toResponseList(orderPage.getContent()),
                orderPage.getPage(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages());
    }

    @Override
    public OrderResponseDto assignOrder(Long orderId, Long employeeId) {
        return orderResponseMapper.toResponse(orderServicePort.assignOrder(orderId, employeeId));
    }

    @Override
    public OrderResponseDto markOrderReady(Long orderId, Long employeeId) {
        return orderResponseMapper.toResponse(orderServicePort.markOrderReady(orderId, employeeId));
    }

    @Override
    @Transactional(dontRollbackOn = {InvalidSecurityPinException.class, OrderLockedException.class})
    public OrderResponseDto deliverOrder(Long orderId, Long employeeId,
                                         OrderDeliveryRequestDto orderDeliveryRequestDto) {
        return orderResponseMapper.toResponse(
                orderServicePort.deliverOrder(orderId, employeeId, orderDeliveryRequestDto.getPin()));
    }
}
