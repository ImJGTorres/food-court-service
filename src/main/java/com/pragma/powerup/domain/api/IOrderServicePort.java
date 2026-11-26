package com.pragma.powerup.domain.api;

import com.pragma.powerup.domain.model.OrderModel;
import com.pragma.powerup.domain.model.OrderStatus;
import com.pragma.powerup.domain.model.PageModel;

public interface IOrderServicePort {
    OrderModel createOrder(OrderModel orderModel, Long clientId);

    PageModel<OrderModel> listOrdersByStatus(Long employeeId, OrderStatus status, int page, int size);

    OrderModel assignOrder(Long orderId, Long employeeId);

    OrderModel markOrderReady(Long orderId, Long employeeId);
}
